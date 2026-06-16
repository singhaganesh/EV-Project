package com.ganesh.ev.data.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives FCM messages (CV-11). Expected data payload keys:
 *   type     — CHARGING_COMPLETE | FORCE_STOPPED | BOOKING_EXPIRING | PAYMENT_STATUS
 *   title    — notification title (falls back to the notification block)
 *   body     — notification body
 *   deepLink — e.g. "plugsy://payment/123" so the tap opens the right screen
 */
class EvMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        DeviceTokenRegistrar.register(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "Plugsy"
        val body = message.notification?.body ?: data["body"] ?: ""
        Notifications.show(
                context = this,
                type = data["type"],
                title = title,
                body = body,
                deepLink = data["deepLink"]
        )
    }
}
