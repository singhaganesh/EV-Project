package com.ganesh.ev.data.model

import java.io.Serializable

data class StationWithScore(
        val station: Station,
        val distance: Double,
        val score: Double,
        val trafficScore: Double,
        val gridScore: Double,
        val parkingScore: Double,
        val accessScore: Double,
        val availableSlots: Int,
        val totalSlots: Int,
        val connectorTypes: List<String> = emptyList(),
        val lastActive: String = "Unknown",
        val rating: Double = 0.0
) : Serializable
