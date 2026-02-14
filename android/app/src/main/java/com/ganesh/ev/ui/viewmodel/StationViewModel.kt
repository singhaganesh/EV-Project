package com.ganesh.ev.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.model.ChargerSlot
import com.ganesh.ev.data.model.Station
import com.ganesh.ev.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StationUiState {
    object Initial : StationUiState()
    object Loading : StationUiState()
    data class StationsLoaded(val stations: List<Station>) : StationUiState()
    data class StationDetailLoaded(val station: Station, val slots: List<ChargerSlot>) : StationUiState()
    data class Error(val message: String) : StationUiState()
}

class StationViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow<StationUiState>(StationUiState.Initial)
    val uiState: StateFlow<StationUiState> = _uiState.asStateFlow()
    
    fun loadStations() {
        viewModelScope.launch {
            _uiState.value = StationUiState.Loading
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
    
    fun resetState() {
        _uiState.value = StationUiState.Initial
    }
}
