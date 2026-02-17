package com.ganesh.ev.data.model

data class LivePowerData(
        val stationId: Long,
        val voltage: Double,
        val current: Double,
        val power: Double,
        val timestamp: String,
        val forecastedLoad: Double? = null
)
