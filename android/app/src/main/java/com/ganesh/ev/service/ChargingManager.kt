package com.ganesh.ev.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.ganesh.ev.BuildConfig
import com.ganesh.ev.data.model.SimulatedSession
import com.ganesh.ev.data.network.RetrofitClient
import com.ganesh.ev.data.network.StompClient
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Process-singleton owner of the live charging WebSocket + telemetry (CV-5).
 *
 * Holding the socket here (rather than in [com.ganesh.ev.ui.viewmodel.ChargingViewModel])
 * lets charging survive the user leaving the screen: the ViewModel merely
 * observes [telemetry], while [ChargingForegroundService] keeps the process
 * alive with an ongoing notification. The session only tears down on an explicit
 * stop/complete, not when the UI is destroyed.
 */
object ChargingManager {

    private var appContext: Context? = null
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var stompClient: StompClient? = null

    private val _telemetry = MutableStateFlow<SimulatedSession?>(null)
    val telemetry: StateFlow<SimulatedSession?> = _telemetry.asStateFlow()

    @Volatile
    var activeBookingId: Long = -1L
        private set

    @Volatile
    var activeSessionId: Long = -1L
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun setActiveSessionId(id: Long) {
        if (id > 0) activeSessionId = id
    }

    /** Connects telemetry for [bookingId] and starts the foreground service. */
    fun connect(bookingId: Long) {
        val ctx = appContext ?: return
        if (activeBookingId == bookingId && stompClient != null) {
            startService(ctx, bookingId) // already connected; ensure service is up
            return
        }
        disconnectSocketOnly()
        activeBookingId = bookingId
        _telemetry.value = null
        openSocket(bookingId)
        startService(ctx, bookingId)
    }

    /** Re-opens the socket after a process-death service restart (no service start). */
    fun resume(bookingId: Long) {
        if (bookingId <= 0) return
        if (activeBookingId == bookingId && stompClient != null) return
        activeBookingId = bookingId
        openSocket(bookingId)
    }

    private fun openSocket(bookingId: Long) {
        val baseUrl = BuildConfig.BASE_URL
        val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://") + "ws/websocket"
        val client = StompClient(wsUrl)
        stompClient = client
        client.connect()
        client.subscribe("/topic/session/$bookingId") { json ->
            try {
                val t = gson.fromJson(json, SimulatedSession::class.java)
                _telemetry.value = t
                if (t?.completed == true) {
                    // Backend finished the session — stop receiving (off the socket
                    // callback thread). The completed frame stays in _telemetry so the
                    // foreground service swaps to the "tap to pay" card and an open
                    // charging screen advances to payment.
                    scope.launch { disconnectSocketOnly() }
                }
            } catch (e: Exception) {
                Log.e("ChargingManager", "Error parsing telemetry: ${e.message}")
            }
        }
    }

    /** Stops the session's charging on the backend (from the notification Stop action). */
    fun requestStop() {
        val sid = activeSessionId
        scope.launch {
            try {
                if (sid > 0) RetrofitClient.apiService.stopCharging(sid)
            } catch (_: Exception) {
                // Non-fatal; disconnect regardless.
            }
            disconnect()
        }
    }

    /** Fully tears down telemetry and stops the foreground service. */
    fun disconnect() {
        disconnectSocketOnly()
        activeBookingId = -1L
        activeSessionId = -1L
        _telemetry.value = null
        appContext?.let { stopService(it) }
    }

    private fun disconnectSocketOnly() {
        stompClient?.disconnect()
        stompClient = null
    }

    private fun startService(ctx: Context, bookingId: Long) {
        val intent = Intent(ctx, ChargingForegroundService::class.java)
                .putExtra(ChargingForegroundService.EXTRA_BOOKING_ID, bookingId)
        ContextCompat.startForegroundService(ctx, intent)
    }

    private fun stopService(ctx: Context) {
        ctx.stopService(Intent(ctx, ChargingForegroundService::class.java))
    }
}
