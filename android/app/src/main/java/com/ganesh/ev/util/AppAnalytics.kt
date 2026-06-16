package com.ganesh.ev.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Thin wrapper over Firebase Analytics so the rest of the app logs funnel
 * events without depending on the SDK directly (CV-13). Initialized once from
 * [com.ganesh.ev.EvApplication]; calls are no-ops until then.
 */
object AppAnalytics {

    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context.applicationContext)
    }

    fun log(event: String, params: Map<String, String> = emptyMap()) {
        val instance = analytics ?: return
        val bundle = Bundle().apply { params.forEach { (k, v) -> putString(k, v) } }
        instance.logEvent(event, bundle)
    }

    // ── Funnel: discover → book → charge → pay ──
    fun appOpen() = log("app_open")
    fun stationViewed(stationId: Long) = log("station_view", mapOf("station_id" to stationId.toString()))
    fun bookingCreated(stationId: Long) = log("booking_created", mapOf("station_id" to stationId.toString()))
    fun chargingStarted(bookingId: Long) = log("charging_started", mapOf("booking_id" to bookingId.toString()))
    fun chargingCompleted(sessionId: Long) = log("charging_completed", mapOf("session_id" to sessionId.toString()))
    fun paymentSuccess(sessionId: Long) = log("payment_success", mapOf("session_id" to sessionId.toString()))
}
