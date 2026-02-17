package com.ganesh.ev.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.model.ChargerSlot
import com.ganesh.ev.data.model.LivePowerData
import com.ganesh.ev.data.model.Station
import com.ganesh.ev.data.model.StationMarker
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

    // Viewport markers (lightweight, for map pins)
    private val _viewportMarkers = MutableStateFlow<List<StationMarker>>(emptyList())
    val viewportMarkers: StateFlow<List<StationMarker>> = _viewportMarkers.asStateFlow()

    // Selected station detail (on marker click)
    private val _selectedStationDetail = MutableStateFlow<StationWithScore?>(null)
    val selectedStationDetail: StateFlow<StationWithScore?> = _selectedStationDetail.asStateFlow()

    // Store user location for detail queries
    private var userLat: Double = 0.0
    private var userLng: Double = 0.0

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

    // Load nearest 5 stations — map stays visible, only a thin loading bar shows
    fun loadNearbyStations(lat: Double, lng: Double) {
        userLat = lat
        userLng = lng
        viewModelScope.launch {
            _isLoadingStations.value = true
            try {
                val response = RetrofitClient.apiService.getNearbyStations(lat, lng, 50.0, 5)
                if (response.isSuccessful) {
                    val stations = response.body()?.data ?: emptyList()
                    if (stations.isNotEmpty()) {
                        _uiState.value = StationUiState.NearbyStationsLoaded(stations)
                    } else {
                        fetchAllStations()
                    }
                } else {
                    fetchAllStations()
                }
            } catch (e: Exception) {
                fetchAllStations()
            }
            _isLoadingStations.value = false
        }
    }

    // Load lightweight markers for current map viewport
    fun loadViewportMarkers(neLat: Double, neLng: Double, swLat: Double, swLng: Double) {
        viewModelScope.launch {
            _isLoadingStations.value = true
            try {
                val response =
                        RetrofitClient.apiService.getStationsInViewport(neLat, neLng, swLat, swLng)
                if (response.isSuccessful) {
                    _viewportMarkers.value = response.body()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                // Silent fail — keep existing markers
            }
            _isLoadingStations.value = false
        }
    }

    // When a marker is clicked: fetch 5 nearest stations from the marker's lat/lng
    // and update the bottom pager (clicked station first, then 4 nearest)
    // Distances are always recalculated from the user's actual location.
    fun onMarkerClicked(stationId: Long, markerLat: Double, markerLng: Double) {
        viewModelScope.launch {
            _isLoadingStations.value = true
            try {
                val response =
                        RetrofitClient.apiService.getNearbyStations(markerLat, markerLng, 50.0, 5)
                if (response.isSuccessful) {
                    val stations = response.body()?.data ?: emptyList()
                    if (stations.isNotEmpty()) {
                        // Recalculate distance from user's actual location
                        val withUserDistance =
                                stations.map { s ->
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
                        val others = withUserDistance.filter { it.station.id != stationId }.take(4)
                        val reordered =
                                if (clicked != null) {
                                    listOf(clicked) + others
                                } else {
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
                }
            } catch (e: Exception) {
                // Silent fail — keep existing pager data
            }
            _isLoadingStations.value = false
        }
    }

    fun clearSelectedDetail() {
        _selectedStationDetail.value = null
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
