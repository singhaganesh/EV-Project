package com.ganesh.ev.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object LocationHelper {

    // Haversine formula to calculate distance between two points in km
    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a =
                sin(dLat / 2).pow(2) +
                        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)

        val c = 2 * asin(sqrt(a))
        return R * c
    }

    // Launch Google Maps navigation
    fun navigateToStation(context: Context, lat: Double, lng: Double) {
        val uri = "google.navigation:q=$lat,$lng"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback: Open in browser or show toast
            val webIntent =
                    Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                    "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"
                            )
                    )
            context.startActivity(webIntent)
        }
    }
}
