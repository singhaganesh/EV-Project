package com.ganesh.ev.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.model.ChargerSlot
import com.ganesh.ev.data.model.LivePowerData
import com.ganesh.ev.data.model.Station
import com.ganesh.ev.data.model.StationPin
import com.ganesh.ev.data.model.StationWithScore
import com.ganesh.ev.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StationUiState {
    object Initial : StationUiState()
    object Loading : StationUiState()
    data class StationsLoaded(val stations: List<Station>) : StationUiState()
    data class NearbyStationsLoaded(val stations: List<StationWithScore>) : StationUiState()
    data class StationDetailLoaded(
            val station: Station,
            val slots: List<ChargerSlot>,
            val powerData: LivePowerData? = null
    ) : StationUiState()
    data class Error(val message: String) : StationUiState()
}

class StationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<StationUiState>(StationUiState.Initial)
    val uiState: StateFlow<StationUiState> = _uiState.asStateFlow()

    // Separate loading flag — does NOT block the map
    private val _isLoadingStations = MutableStateFlow(false)
    val isLoadingStations: StateFlow<Boolean> = _isLoadingStations.asStateFlow()

    // Other pins (lightweight lat/lng only — non-nearby stations)
    private val _otherPins = MutableStateFlow<List<StationPin>>(emptyList())
    val otherPins: StateFlow<List<StationPin>> = _otherPins.asStateFlow()

    // In-Memory Pin Cache: Prevents pins from flickering/disappearing during panning
    private val pinCache = mutableMapOf<Long, StationPin>()

    // Store user location for detail queries
    private var userLat: Double = 0.0
    private var userLng: Double = 0.0

    // Store the last known viewport bounds so we can re-use them on marker click
    private var lastNeLat: Double = 0.0
    private var lastNeLng: Double = 0.0
    private var lastSwLat: Double = 0.0
    private var lastSwLng: Double = 0.0

    fun loadStations() {
        viewModelScope.launch {
            _isLoadingStations.value = true
            fetchAllStations()
            _isLoadingStations.value = false
        }
    }

    private suspend fun fetchAllStations() {
        try {
            val response = RetrofitClient.apiService.getAllStations()
            if (response.isSuccessful) {
                val stations = response.body() ?: emptyList<Station>()
                _uiState.value = StationUiState.StationsLoaded(stations)
            } else {
                _uiState.value = StationUiState.Error("Failed to load stations")
            }
        } catch (e: Exception) {
            _uiState.value = StationUiState.Error("Network error: ${e.message}")
        }
    }

    fun loadStationDetail(stationId: Long) {
        viewModelScope.launch {
            _isLoadingStations.value = true
            _uiState.value = StationUiState.Loading
            try {
                val stationResponse = RetrofitClient.apiService.getStationById(stationId)
                val slotsResponse = RetrofitClient.apiService.getSlotsByStation(stationId)

                if (stationResponse.isSuccessful && slotsResponse.isSuccessful) {
                    val station = stationResponse.body()?.data
                    val slots = slotsResponse.body()?.data ?: emptyList()
                    if (station != null) {
                        _uiState.value = StationUiState.StationDetailLoaded(station, slots)
                        loadStationPower(stationId)
                    } else {
                        _uiState.value = StationUiState.Error("Station not found")
                    }
                } else {
                    _uiState.value = StationUiState.Error("Failed to load station details")
                }
            } catch (e: Exception) {
                _uiState.value = StationUiState.Error("Network error: ${e.message}")
            }
            _isLoadingStations.value = false
        }
    }

    // ===== UNIFIED: Viewport + Nearby in one call =====
    // Full data for top 5 nearby, lat/lng pins for the rest
    fun loadViewportWithNearby(
            neLat: Double,
            neLng: Double,
            swLat: Double,
            swLng: Double,
            lat: Double,
            lng: Double
    ) {
        // Smart Delta Check: Skip fetch if viewport hasn't moved significantly
        if (!shouldRefetchViewport(neLat, neLng, swLat, swLng) &&
                        _uiState.value !is StationUiState.Initial
        ) {
            return
        }

        userLat = lat
        userLng = lng
        lastNeLat = neLat
        lastNeLng = neLng
        lastSwLat = swLat
        lastSwLng = swLng

        viewModelScope.launch {
            _isLoadingStations.value = true
            try {
                val response =
                        RetrofitClient.apiService.getViewportWithNearby(
                                neLat,
                                neLng,
                                swLat,
                                swLng,
                                lat,
                                lng,
                                5 // limit
                        )
                if (response.isSuccessful) {
                    val data = response.body()?.data
                    if (data != null) {
                        val nearby = data.nearbyStations

                        // Only update nearby stations on FIRST load.
                        // After that, nearby only changes when user clicks a pin
                        // (handled by onMarkerClicked).
                        val currentState = _uiState.value
                        if (nearby.isNotEmpty() &&
                                        currentState !is StationUiState.NearbyStationsLoaded
                        ) {
                            _uiState.value = StationUiState.NearbyStationsLoaded(nearby)
                        }

                        // Merge new pins into the in-memory cache
                        data.otherPins.forEach { pin -> pinCache[pin.id] = pin }
                        // Also cache nearby pins so they show as green dots when user pans away
                        nearby.forEach { s ->
                            pinCache[s.station.id] =
                                    StationPin(
                                            s.station.id,
                                            s.station.latitude,
                                            s.station.longitude
                                    )
                        }

                        // Emit ALL cached pins, excluding the current nearby stations in the pager
                        val currentNearbyIds =
                                when (val state = _uiState.value) {
                                    is StationUiState.NearbyStationsLoaded ->
                                            state.stations.map { it.station.id }.toSet()
                                    else -> emptySet()
                                }
                        _otherPins.value =
                                pinCache.values.filter { !currentNearbyIds.contains(it.id) }
                    }
                } else {
                    // Fallback to old endpoint
                    fetchAllStations()
                }
            } catch (e: Exception) {
                fetchAllStations()
            }
            _isLoadingStations.value = false
        }
    }

    private fun shouldRefetchViewport(
            newNeLat: Double,
            newNeLng: Double,
            newSwLat: Double,
            newSwLng: Double
    ): Boolean {
        if (lastNeLat == 0.0 && lastNeLng == 0.0) return true // First load

        // Calculate viewport width/height
        val latSpan = Math.abs(lastNeLat - lastSwLat)
        val lngSpan = Math.abs(lastNeLng - lastSwLng)

        // Calculate how much the center has moved
        val latMove = Math.abs(newNeLat - lastNeLat)
        val lngMove = Math.abs(newNeLng - lastNeLng)

        // Only refetch if the map panned by more than 15% of the visible area
        return (latMove > latSpan * 0.15) || (lngMove > lngSpan * 0.15)
    }

    // When a marker is clicked: call the unified endpoint centered on clicked marker
    // to get 5 nearest from that point (full data) + rest as pins
    fun onMarkerClicked(stationId: Long, markerLat: Double, markerLng: Double) {
        viewModelScope.launch {
            _isLoadingStations.value = true
            try {
                val response =
                        RetrofitClient.apiService.getViewportWithNearby(
                                lastNeLat,
                                lastNeLng,
                                lastSwLat,
                                lastSwLng,
                                markerLat,
                                markerLng,
                                5
                        )
                if (response.isSuccessful) {
                    val data = response.body()?.data
                    if (data != null) {
                        val nearby = data.nearbyStations
                        if (nearby.isNotEmpty()) {
                            // Recalculate distance from user's actual location
                            val withUserDistance =
                                    nearby.map { s ->
                                        s.copy(
                                                distance =
                                                        haversineKm(
                                                                userLat,
                                                                userLng,
                                                                s.station.latitude,
                                                                s.station.longitude
                                                        )
                                        )
                                    }
                            // Ensure clicked station is first in the list
                            val clicked = withUserDistance.find { it.station.id == stationId }
                            val others =
                                    withUserDistance.filter { it.station.id != stationId }.take(4)
                            val reordered =
                                    if (clicked != null) {
                                        listOf(clicked) + others
                                    } else {
                                        // Clicked station not in nearby — fetch its detail
                                        // individually
                                        val detailResponse =
                                                RetrofitClient.apiService.getStationDetail(
                                                        stationId,
                                                        userLat,
                                                        userLng
                                                )
                                        if (detailResponse.isSuccessful) {
                                            val detail = detailResponse.body()?.data
                                            if (detail != null) {
                                                listOf(detail) + withUserDistance.take(4)
                                            } else withUserDistance
                                        } else withUserDistance
                                    }
                            _uiState.value = StationUiState.NearbyStationsLoaded(reordered)
                        }
                        _otherPins.value =
                                pinCache.values.filter { pin ->
                                    !nearby.map { it.station.id }.contains(pin.id)
                                }
                    }
                }
            } catch (e: Exception) {
                // Silent fail — keep existing pager data
            }
            _isLoadingStations.value = false
        }
    }

    fun clearSelectedDetail() {
        pinCache.clear()
        _otherPins.value = emptyList()
    }

    fun loadStationPower(stationId: Long) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getStationLivePower(stationId)
                if (response.isSuccessful) {
                    val powerData = response.body()?.data
                    val currentState = _uiState.value
                    if (currentState is StationUiState.StationDetailLoaded &&
                                    currentState.station.id == stationId
                    ) {
                        _uiState.value = currentState.copy(powerData = powerData)
                    }
                }
            } catch (e: Exception) {
                // Silent fail for power data updates
            }
        }
    }

    fun resetState() {
        _uiState.value = StationUiState.Initial
    }

    /** Haversine distance in km between two lat/lng points */
    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(Math.toRadians(lat1)) *
                                Math.cos(Math.toRadians(lat2)) *
                                Math.sin(dLng / 2) *
                                Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
