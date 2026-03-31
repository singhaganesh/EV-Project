package com.ganesh.ev.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.model.ChargingSession
import com.ganesh.ev.data.model.SimpleChargingSession
import com.ganesh.ev.data.model.SimulatedSession
import com.ganesh.ev.data.model.StartChargingRequest
import com.ganesh.ev.data.network.RetrofitClient
import com.ganesh.ev.data.network.StompClient
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ChargingUiState {
    object Initial : ChargingUiState()
    object Loading : ChargingUiState()
    data class SessionStarted(val session: SimpleChargingSession) : ChargingUiState()
    data class SessionStopped(val simpleSession: SimpleChargingSession) : ChargingUiState()
    data class SessionLoaded(val session: ChargingSession) : ChargingUiState()
    data class SessionsLoaded(val sessions: List<ChargingSession>) : ChargingUiState()
    data class PowerDataLoaded(val powerData: com.ganesh.ev.data.model.LivePowerData) :
            ChargingUiState()
    data class Error(val message: String) : ChargingUiState()
}

class ChargingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ChargingUiState>(ChargingUiState.Initial)
    val uiState: StateFlow<ChargingUiState> = _uiState.asStateFlow()

    private val _telemetryData = MutableStateFlow<SimulatedSession?>(null)
    val telemetryData: StateFlow<SimulatedSession?> = _telemetryData.asStateFlow()

    private var stompClient: StompClient? = null
    private val gson = Gson()

    fun startCharging(bookingId: Long) {
        viewModelScope.launch {
            _uiState.value = ChargingUiState.Loading
            try {
                val request = StartChargingRequest(bookingId = bookingId)
                val response = RetrofitClient.apiService.startCharging(request)
                android.util.Log.d("ChargingVM", "Start charging response: ${response.code()}")
                android.util.Log.d("ChargingVM", "Response body: ${response.body()}")
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    android.util.Log.d("ChargingVM", "API Response success: ${apiResponse?.success}")
                    android.util.Log.d("ChargingVM", "API Response data: ${apiResponse?.data}")
                    
                    // Check if backend returned success=false
                    if (apiResponse?.success == false) {
                        _uiState.value = ChargingUiState.Error(apiResponse.message ?: "Failed to start charging")
                        return@launch
                    }
                    
                    // Handle both SimpleChargingSession and Map responses
                    val simpleSession = when (val data = apiResponse?.data) {
                        is SimpleChargingSession -> data
                        is Map<*, *> -> {
                            val id = (data["id"] as? Number)?.toLong() ?: -1L
                            SimpleChargingSession(id, null, null, null, null)
                        }
                        else -> null
                    }
                    
                    if (simpleSession != null && simpleSession.id > 0) {
                        _uiState.value = ChargingUiState.SessionStarted(simpleSession)
                        // Immediately load the full session details
                        loadSession(simpleSession.id)
                        startWebSocketTelemetry(bookingId)
                    } else {
                        _uiState.value = ChargingUiState.Error("Failed to start charging session: ${apiResponse?.message}")
                    }
                } else {
                    _uiState.value =
                            ChargingUiState.Error("Failed to start charging: ${response.message()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("ChargingVM", "Exception during start charging", e)
                _uiState.value = ChargingUiState.Error("Network error: ${e.message}")
            }
        }
    }

    private fun startWebSocketTelemetry(bookingId: Long) {
        val baseUrl = com.ganesh.ev.BuildConfig.BASE_URL
        val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://") + "ws/websocket"
        
        stompClient = StompClient(wsUrl)
        stompClient?.connect()
        stompClient?.subscribe("/topic/session/$bookingId") { json ->
            try {
                val data = gson.fromJson(json, SimulatedSession::class.java)
                _telemetryData.value = data
            } catch (e: Exception) {
                android.util.Log.e("STOMP", "Error parsing telemetry: ${e.message}")
            }
        }
    }

    fun stopCharging(sessionId: Long) {
        viewModelScope.launch {
            _uiState.value = ChargingUiState.Loading
            try {
                val response = RetrofitClient.apiService.stopCharging(sessionId)
                if (response.isSuccessful) {
                    val simpleSession = response.body()?.data
                    if (simpleSession != null) {
                        _uiState.value = ChargingUiState.SessionStopped(simpleSession)
                        stopWebSocketTelemetry()
                    } else {
                        _uiState.value = ChargingUiState.Error("Failed to stop charging session")
                    }
                } else {
                    _uiState.value =
                            ChargingUiState.Error("Failed to stop charging: ${response.message()}")
                }
            } catch (e: Exception) {
                _uiState.value = ChargingUiState.Error("Network error: ${e.message}")
            }
        }
    }

    private fun stopWebSocketTelemetry() {
        stompClient?.disconnect()
        stompClient = null
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
                        if (session.endTime == null) {
                            session.booking?.id?.let { startWebSocketTelemetry(it) }
                        }
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

    fun loadSessionByBooking(bookingId: Long) {
        viewModelScope.launch {
            _uiState.value = ChargingUiState.Loading
            try {
                val response = RetrofitClient.apiService.getSessionByBooking(bookingId)
                if (response.isSuccessful) {
                    val session = response.body()?.data
                    if (session != null) {
                        _uiState.value = ChargingUiState.SessionLoaded(session)
                        if (session.endTime == null) {
                            startWebSocketTelemetry(bookingId)
                        }
                    } else {
                        _uiState.value = ChargingUiState.Error("Session not found for this booking")
                    }
                } else {
                    _uiState.value = ChargingUiState.Error("Failed to fetch active session")
                }
            } catch (e: Exception) {
                _uiState.value = ChargingUiState.Error("Network error: ${e.message}")
            }
        }
    }

    fun verifyPayment(orderId: String, paymentId: String, signature: String) {
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "razorpay_order_id" to orderId,
                    "razorpay_payment_id" to paymentId,
                    "razorpay_signature" to signature
                )
                val response = RetrofitClient.apiService.verifyPayment(data)
                if (response.isSuccessful) {
                    // Success! No need to reload here as we are likely finishing the flow
                }
            } catch (e: Exception) {
                android.util.Log.e("ChargingVM", "Payment verification failed", e)
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

    private var isPolling = false

    fun startPollingPower(stationId: Long) {
        if (isPolling) return
        isPolling = true
        viewModelScope.launch {
            while (isPolling) {
                try {
                    val response = RetrofitClient.apiService.getStationLivePower(stationId)
                    if (response.isSuccessful && response.body()?.data != null) {
                        // Note: We don't overwrite the main state if it's SessionStarted/Loaded,
                        // so we might need a separate StateFlow for power data or a composite
                        // state.
                        // For simplicity, we emit a side-effect or update a separate flow.
                        // However, since UIState is a sealed class, switching to PowerDataLoaded
                        // might hide the Session info.
                        // Ideally, we should add powerData to SessionStarted/Loaded or use a
                        // separate flow.
                        // I'll update the approach to use a separate flow for power data.
                        _powerDataState.value = response.body()?.data
                    }
                } catch (e: Exception) {
                    // Ignore polling errors
                }
                kotlinx.coroutines.delay(5000) // Poll every 5 seconds
            }
        }
    }

    fun stopPolling() {
        isPolling = false
    }

    private val _powerDataState = MutableStateFlow<com.ganesh.ev.data.model.LivePowerData?>(null)
    val powerDataState: StateFlow<com.ganesh.ev.data.model.LivePowerData?> =
            _powerDataState.asStateFlow()

    fun resetState() {
        _uiState.value = ChargingUiState.Initial
        _powerDataState.value = null
        stopPolling()
        isPolling = false
    }
}
