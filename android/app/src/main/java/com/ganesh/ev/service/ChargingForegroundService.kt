package com.ganesh.ev.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ganesh.ev.data.model.SimulatedSession
import com.ganesh.ev.data.notifications.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Keeps the charging session alive while the app is backgrounded / the screen
 * is off (CV-5). The socket itself lives in [ChargingManager]; this service
 * promotes the process to the foreground with an ongoing notification that
 * mirrors live SoC and offers a Stop action. START_REDELIVER_INTENT lets it
 * resume the right session after process death.
 */
class ChargingForegroundService : Service() {

    companion object {
        const val EXTRA_BOOKING_ID = "bookingId"
        const val ACTION_STOP = "com.ganesh.ev.action.STOP_CHARGING"
        private const val NOTIF_ID = 4242
        // Separate id so stopping the service (which cancels NOTIF_ID) doesn't
        // also remove the "tap to pay" completion card.
        private const val COMPLETE_NOTIF_ID = 4243
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            ChargingManager.requestStop()
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }

        val bookingId = intent?.getLongExtra(EXTRA_BOOKING_ID, -1L) ?: -1L
        // Resume the socket if the process was restarted with a redelivered intent.
        if (bookingId > 0) ChargingManager.resume(bookingId)

        startInForeground(buildNotification(ChargingManager.telemetry.value))

        if (!observing) {
            observing = true
            scope.launch {
                ChargingManager.telemetry.collect { telemetry ->
                    when {
                        // Null is emitted during teardown — ignore (onDestroy removes
                        // the card), don't downgrade to the "in progress" fallback.
                        telemetry == null -> {}
                        // Session finished (battery full / overtime): swap the ongoing
                        // card for a dismissable "tap to pay" card and stop the service.
                        telemetry.completed -> {
                            showCompletionNotification()
                            stopForegroundCompat()
                            stopSelf()
                        }
                        else ->
                                NotificationManagerCompat.from(this@ChargingForegroundService)
                                        .notify(NOTIF_ID, buildNotification(telemetry))
                    }
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(t: SimulatedSession?): Notification {
        val contentText =
                if (t != null) {
                    val soc = "${t.socPercentage.roundToInt()}%"
                    val power = "%.1f".format(t.powerKw)
                    "SoC $soc · $power kW"
                } else {
                    "Charging in progress"
                }

        val contentIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        Intent(
                                        Intent.ACTION_VIEW,
                                        // isNewSession=false → resume/show the live session instead
                                        // of trying to start a new one (which would already exist).
                                        Uri.parse("plugsy://charging/${ChargingManager.activeBookingId}?isNewSession=false")
                                )
                                .setPackage(packageName),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        val stopIntent =
                PendingIntent.getService(
                        this,
                        1,
                        Intent(this, ChargingForegroundService::class.java).setAction(ACTION_STOP),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        return NotificationCompat.Builder(this, Notifications.CHANNEL_CHARGING)
                .setSmallIcon(applicationInfo.icon)
                .setContentTitle("Charging your vehicle")
                .setContentText(contentText)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent)
                .addAction(0, "Stop", stopIntent)
                .build()
    }

    /** Terminal "Charging complete — tap to pay" card shown when the battery is full. */
    private fun showCompletionNotification() {
        val sessionId = ChargingManager.activeSessionId
        val tapIntent = if (sessionId > 0) {
            Intent(Intent.ACTION_VIEW, Uri.parse("plugsy://payment/$sessionId"))
                    .setPackage(packageName)
        } else {
            // Lost the session id (process death mid-session) — open the app; the
            // Home banner / history "Pay now" still recovers the payment.
            packageManager.getLaunchIntentForPackage(packageName)
        }
        val contentIntent = tapIntent?.let {
            PendingIntent.getActivity(
                    this,
                    2,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(this, Notifications.CHANNEL_CHARGING)
                .setSmallIcon(applicationInfo.icon)
                .setContentTitle("Charging complete")
                .setContentText("Your vehicle is charged. Tap to pay.")
                .setAutoCancel(true)
                .apply { contentIntent?.let { setContentIntent(it) } }
                .build()

        NotificationManagerCompat.from(this).notify(COMPLETE_NOTIF_ID, notification)
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        observing = false
        // Ensure the ongoing notification is gone when the service stops via
        // stopService() (the disconnect path), not just the in-notification Stop action.
        stopForegroundCompat()
        NotificationManagerCompat.from(this).cancel(NOTIF_ID)
    }
}
