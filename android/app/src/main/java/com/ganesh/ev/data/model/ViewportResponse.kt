package com.ganesh.ev.data.model

import java.io.Serializable

/**
 * Combined viewport response: full data for nearby stations + minimal pins for the rest. Matches
 * backend ViewportResponseDTO.
 */
data class ViewportResponse(
        val nearbyStations: List<StationWithScore>,
        val otherPins: List<StationPin>
) : Serializable
