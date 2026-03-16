package com.ganesh.ev.util

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Formats ISO date string to "16 Mar 2026, 3:40 PM"
 */
fun formatBookingDateTime(dateString: String?): String {
    if (dateString == null) return "-"
    return try {
        // Handle ISO-8601 like 2026-03-16T15:40:00 or 2026-03-16T15:40
        val inputFormat = if (dateString.length > 16) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        } else {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        }
        val date = inputFormat.parse(dateString)
        val outputFormat = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
        if (date != null) outputFormat.format(date) else "-"
    } catch (e: Exception) {
        dateString.replace("T", " ") // Fallback
    }
}
