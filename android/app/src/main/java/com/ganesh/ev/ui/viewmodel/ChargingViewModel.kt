package com.ganesh.ev.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.model.ChargingSession
import com.ganesh.ev.data.model.StartChargingRequest
import com.ganesh.ev.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ChargingUiState {
    object Initial : ChargingUiState()
    object Loading : ChargingUiState()
    data class SessionStarted(val session: ChargingSession) : ChargingUiState()
    data class SessionStopped(val session: ChargingSession) : ChargingUiState()
    data class SessionLoaded(val session: ChargingSession) : ChargingUiState()
    data class SessionsLoaded(val sessions: List<ChargingSession>) : ChargingUiState()
    data class Error(val message: String) : ChargingUiState()
}

class ChargingViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow<ChargingUiState>(ChargingUiState.Initial)
    val uiState: StateFlow<ChargingUiState> = _uiState.asStateFlow()
    
    fun startCharging(bookingId: Long) {
        viewModelScope.launch {
            _uiState.value = ChargingUiState.Loading
            try {
                val request = StartChargingRequest(bookingId = bookingId)
                val response = RetrofitClient.apiService.startCharging(request)
                if (response.isSuccessful) {
                    val session = response.body()?.data
                    if (session != null) {
                        _uiState.value = ChargingUiState.SessionStarted(session)
                    } else {
                        _uiState.value = ChargingUiState.Error("Failed to start charging session")
                    }
                } else {
                    _uiState.value = ChargingUiState.Error("Failed to start charging: ${response.message()}")
                }
            } catch (e: Exception) {
                _uiState.value = ChargingUiState.Error("Network error: ${e.message}")
            }
        }
    }
    
    fun stopCharging(sessionId: Long) {
        viewModelScope.launch {
            _uiState.value = ChargingUiState.Loading
            try {
                val response = RetrofitClient.apiService.stopCharging(sessionId)
                if (response.isSuccessful) {
                    val session = response.body()?.data
                    if (session != null) {
                        _uiState.value = ChargingUiState.SessionStopped(session)
                    } else {
                        _uiState.value = ChargingUiState.Error("Failed to stop charging session")
                    }
                } else {
                    _uiState.value = ChargingUiState.Error("Failed to stop charging: ${response.message()}")
                }
            } catch (e: Exception) {
                _uiState.value = ChargingUiState.Error("Network error: ${e.message}")
            }
        }
    }
    
    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            _uiState.value = ChargingUiState.Loading
            try {
                val response = RetrofitClient.apiService.getSession(sessionId)
                if (response.isSuccessful) {
                    val session = response.body()?.data
                    if (session != null) {
                        _uiState.value = ChargingUiState.SessionLoaded(session)
                    } else {
                        _uiState.value = ChargingUiState.Error("Session not found")
                    }
                } else {
                    _uiState.value = ChargingUiState.Error("Failed to load session")
                }
            } catch (e: Exception) {
                _uiState.value = ChargingUiState.Error("Network error: ${e.message}")
            }
        }
    }
    
    fun loadUserHistory(userId: Long) {
        viewModelScope.launch {
            _uiState.value = ChargingUiState.Loading
            try {
                val response = RetrofitClient.apiService.getUserChargingHistory(userId)
                if (response.isSuccessful) {
                    val sessions = response.body()?.data ?: emptyList()
                    _uiState.value = ChargingUiState.SessionsLoaded(sessions)
                } else {
                    _uiState.value = ChargingUiState.Error("Failed to load history")
                }
            } catch (e: Exception) {
                _uiState.value = ChargingUiState.Error("Network error: ${e.message}")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = ChargingUiState.Initial
    }
}
