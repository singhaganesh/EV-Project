package com.ganesh.ev.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Notification channels + display helper for FCM pushes (CV-11).
 *
 * Channels are created once from [com.ganesh.ev.EvApplication]. Each push routes
 * to a channel by its `type` and carries an optional `deepLink` so a tap lands
 * on the right screen (relies on the deep links wired in CV-12).
 */
object Notifications {

    const val CHANNEL_CHARGING = "charging"
    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_TRANSACTIONAL = "transactional"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        nm.createNotificationChannel(
                NotificationChannel(
                                CHANNEL_CHARGING,
                                "Charging",
                                NotificationManager.IMPORTANCE_HIGH
                        )
                        .apply { description = "Live charging status and completion alerts" }
        )
        nm.createNotificationChannel(
                NotificationChannel(
                                CHANNEL_REMINDERS,
                                "Reminders",
                                NotificationManager.IMPORTANCE_DEFAULT
                        )
                        .apply { description = "Reservation expiry reminders" }
        )
        nm.createNotificationChannel(
                NotificationChannel(
                                CHANNEL_TRANSACTIONAL,
                                "Payments",
                                NotificationManager.IMPORTANCE_DEFAULT
                        )
                        .apply { description = "Payment confirmations" }
        )
    }

    fun show(context: Context, type: String?, title: String, body: String, deepLink: String?) {
        // Respect the user's notification preferences (B2).
        if (!NotificationPrefs.isEnabledFor(type)) return

        val channel =
                when (type) {
                    "CHARGING_COMPLETE", "FORCE_STOPPED" -> CHANNEL_CHARGING
                    "BOOKING_EXPIRING" -> CHANNEL_REMINDERS
                    "PAYMENT_STATUS" -> CHANNEL_TRANSACTIONAL
                    else -> CHANNEL_TRANSACTIONAL
                }

        val intent =
                if (!deepLink.isNullOrEmpty()) {
                    Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).setPackage(context.packageName)
                } else {
                    context.packageManager.getLaunchIntentForPackage(context.packageName)
                }

        val pending =
                PendingIntent.getActivity(
                        context,
                        (deepLink ?: title).hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        val notification =
                NotificationCompat.Builder(context, channel)
                        .setSmallIcon(context.applicationInfo.icon)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setContentIntent(pending)
                        .build()

        // notify() is a no-op without POST_NOTIFICATIONS on Android 13+, so this
        // is safe even before the runtime permission is granted.
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
}
