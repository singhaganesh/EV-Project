package com.ganesh.ev.data.notifications

import com.ganesh.ev.data.model.DeviceTokenRequest
import com.ganesh.ev.data.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Registers this device's FCM token with the backend so the user can receive
 * push notifications (CV-11). Registration is skipped until the user is logged
 * in; [com.ganesh.ev.data.notifications.EvMessagingService] re-registers when the
 * token rotates.
 */
object DeviceTokenRegistrar {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fetches the current FCM token and registers it if the user is logged in. */
    fun fetchAndRegister() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            register(token)
        }
    }

    fun register(token: String) {
        if (RetrofitClient.getAuthToken().isEmpty()) return
        scope.launch {
            try {
                RetrofitClient.apiService.registerDeviceToken(DeviceTokenRequest(token))
            } catch (_: Exception) {
                // Non-fatal: retried on next login or token rotation.
            }
        }
    }
}
