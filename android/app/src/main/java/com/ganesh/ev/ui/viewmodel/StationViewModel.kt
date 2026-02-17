package com.ganesh.ev.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.model.ChargerSlot
import com.ganesh.ev.data.model.LivePowerData
import com.ganesh.ev.data.model.Station
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

    fun loadStations() {
        viewModelScope.launch {
            _uiState.value = StationUiState.Loading
            fetchAllStations()
        }
    }

    private suspend fun fetchAllStations() {
        try {
            val response = RetrofitClient.apiService.getAllStations()
            if (response.isSuccessful) {
                // Backend returns raw array, so body is List<Station>
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
            _uiState.value = StationUiState.Loading
            try {
                val stationResponse = RetrofitClient.apiService.getStationById(stationId)
                val slotsResponse = RetrofitClient.apiService.getSlotsByStation(stationId)

                if (stationResponse.isSuccessful && slotsResponse.isSuccessful) {
                    val station = stationResponse.body()?.data
                    val slots = slotsResponse.body()?.data ?: emptyList()
                    if (station != null) {
                        _uiState.value = StationUiState.StationDetailLoaded(station, slots)
                        // Trigger power data load
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
        }
    }

    fun loadNearbyStations(lat: Double, lng: Double) {
        viewModelScope.launch {
            _uiState.value = StationUiState.Loading
            try {
                // Radius default 10.0
                val response =
                        RetrofitClient.apiService.getNearbyStations(
                                lat,
                                lng,
                                50.0
                        ) // Increased radius
                if (response.isSuccessful) {
                    val stations = response.body()?.data ?: emptyList()
                    if (stations.isNotEmpty()) {
                        _uiState.value = StationUiState.NearbyStationsLoaded(stations)
                    } else {
                        // Fallback to all stations if no nearby found
                        fetchAllStations()
                    }
                } else {
                    // Fallback to all stations on API error
                    fetchAllStations()
                }
            } catch (e: Exception) {
                // Fallback to all stations on network error
                fetchAllStations()
            }
        }
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
                // Silent fail for power data updates to not disrupt UI
            }
        }
    }

    fun resetState() {
        _uiState.value = StationUiState.Initial
    }
}
