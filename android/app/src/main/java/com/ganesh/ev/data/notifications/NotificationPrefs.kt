package com.ganesh.ev.data.notifications

/**
 * In-memory snapshot of the user's notification preferences (B2).
 *
 * [Notifications.show] runs on an FCM background thread and can't read DataStore
 * synchronously, so [com.ganesh.ev.EvApplication] keeps these volatile fields in
 * sync with the persisted prefs and the display path consults them. All default
 * on, so notifications still work before the snapshot is first populated.
 *
 * Only FCM push display is gated here — the mandatory foreground-service charging
 * notification is unaffected.
 */
object NotificationPrefs {

    @Volatile var masterEnabled = true
    @Volatile var charging = true
    @Volatile var reminders = true
    @Volatile var payments = true

    /** Whether a push of the given FCM `type` should be shown. */
    fun isEnabledFor(type: String?): Boolean {
        if (!masterEnabled) return false
        return when (type) {
            "CHARGING_COMPLETE", "FORCE_STOPPED" -> charging
            "BOOKING_EXPIRING" -> reminders
            "PAYMENT_STATUS" -> payments
            else -> payments
        }
    }
}
