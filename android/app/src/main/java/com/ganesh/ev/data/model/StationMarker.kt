package com.ganesh.ev.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Lightweight model for map markers — matches backend StationMarkerDTO. Only contains data needed
 * to render pins on the map.
 */
data class StationMarker(
        val id: Long,
        val name: String,
        val latitude: Double,
        val longitude: Double,
        @SerializedName("available") val available: Boolean
) : Serializable
