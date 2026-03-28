package com.ganesh.ev.util

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Formats ISO date string to "16 Mar 2026, 3:40 PM"
 */
fun formatBookingDateTime(dateString: String?): String {
    if (dateString == null) return "-"
    return try {
        val clean = dateString.replace(Regex("\\.\\d+$"), "")
        val inputFormat = if (clean.contains("T")) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        } else {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        }
        val date = inputFormat.parse(clean)
        val outputFormat = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
        if (date != null) outputFormat.format(date) else "-"
    } catch (e: Exception) {
        dateString.replace("T", " ") // Fallback
    }
}

/**
 * Formats ISO date string to relative time (e.g., "2 hr ago", "Just now")
 */
fun formatRelativeTime(dateString: String?): String {
    if (dateString == null) return "Never"
    return try {
        val clean = dateString.replace(Regex("\\.\\d+$"), "")
        val formatter = if (clean.contains("T")) {
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        } else {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        }
        
        val past = LocalDateTime.parse(clean, formatter)
        val now = LocalDateTime.now()
        val duration = java.time.Duration.between(past, now)
        
        val seconds = duration.seconds
        if (seconds < 60) return "Just now"
        
        val minutes = duration.toMinutes()
        if (minutes < 60) return "$minutes min ago"
        
        val hours = duration.toHours()
        if (hours < 24) return "$hours hr ago"
        
        val days = duration.toDays()
        if (days < 30) return "$days days ago"
        
        val months = days / 30
        if (months < 12) return "$months months ago"
        
        return "Long ago"
    } catch (e: Exception) {
        "Recently"
    }
}
