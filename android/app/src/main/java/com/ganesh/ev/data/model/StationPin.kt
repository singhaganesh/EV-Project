package com.ganesh.ev.data.model

import java.io.Serializable

/**
 * Ultra-lightweight model for map pins — only coordinates. Matches backend StationPinDTO. Used for
 * non-nearby stations.
 */
data class StationPin(val id: Long, val latitude: Double, val longitude: Double) : Serializable
