package com.ganesh.ev.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.model.Booking
import com.ganesh.ev.data.model.BookingRequest
import com.ganesh.ev.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BookingUiState {
    object Initial : BookingUiState()
    object Loading : BookingUiState()
    data class BookingCreated(val booking: Booking) : BookingUiState()
    data class BookingsLoaded(val bookings: List<Booking>) : BookingUiState()
    data class BookingCancelled(val message: String) : BookingUiState()
    data class Error(val message: String) : BookingUiState()
    object PromptTruckFallback : BookingUiState()
}

class BookingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<BookingUiState>(BookingUiState.Initial)
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    private val _truckPrice = mutableStateOf<Double?>(null)
    val truckPrice: State<Double?> = _truckPrice

    fun setTruckPrice(price: Double?) {
        _truckPrice.value = price
    }

    /**
     * "Book Now" — sends stationId + connectorType + vehicleType. Backend handles: timestamp, slot
     * assignment, grace period.
     */
    fun createBooking(
            userId: Long,
            stationId: Long,
            connectorType: String,
            vehicleType: String,
            allowTruckSlotFallback: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.value = BookingUiState.Loading
            try {
                val request =
                        BookingRequest(
                                userId = userId,
                                stationId = stationId,
                                connectorType = connectorType,
                                vehicleType = vehicleType,
                                allowTruckSlotFallback = allowTruckSlotFallback
                        )
                android.util.Log.d("BookingVM", "Sending booking request: $request")
                val response = RetrofitClient.apiService.createBooking(request)
                android.util.Log.d("BookingVM", "Response code: ${response.code()}")
                android.util.Log.d("BookingVM", "Response successful: ${response.isSuccessful}")
                if (response.isSuccessful) {
                    val booking = response.body()
                    android.util.Log.d("BookingVM", "Response body: $booking")
                    if (booking != null) {
                        _uiState.value = BookingUiState.BookingCreated(booking)
                    } else {
                        _uiState.value = BookingUiState.Error("Server returned empty response")
                    }
                } else {
                    val rawErrorBody = response.errorBody()?.string() ?: response.message()
                    val cleanMessage =
                            try {
                                val json = org.json.JSONObject(rawErrorBody)
                                json.optString("message", rawErrorBody)
                            } catch (e: Exception) {
                                rawErrorBody
                            }
                    android.util.Log.e(
                            "BookingVM",
                            "Error response: code=${response.code()}, cleanMessage=$cleanMessage"
                    )

                    if (cleanMessage.contains("PROMPT_TRUCK_FALLBACK")) {
                        _uiState.value = BookingUiState.PromptTruckFallback
                    } else {
                        _uiState.value = BookingUiState.Error(cleanMessage)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BookingVM", "Exception during booking", e)
                _uiState.value = BookingUiState.Error("Network error: ${e.message}")
            }
        }
    }

    fun loadUserBookings(userId: Long) {
        viewModelScope.launch {
            _uiState.value = BookingUiState.Loading
            try {
                val response = RetrofitClient.apiService.getUserBookings(userId)
                if (response.isSuccessful) {
                    // API returns direct list, not wrapped in ApiResponse
                    val bookings = response.body() ?: emptyList()
                    // Sort by startTime descending (newest first)
                    val sortedBookings = bookings.sortedByDescending { it.startTime }
                    _uiState.value = BookingUiState.BookingsLoaded(sortedBookings)
                } else {
                    _uiState.value = BookingUiState.Error("Failed to load bookings")
                }
            } catch (e: Exception) {
                _uiState.value = BookingUiState.Error("Network error: ${e.message}")
            }
        }
    }

    fun cancelBooking(bookingId: Long) {
        viewModelScope.launch {
            _uiState.value = BookingUiState.Loading
            try {
                val response = RetrofitClient.apiService.cancelBooking(bookingId)
                if (response.isSuccessful) {
                    _uiState.value =
                            BookingUiState.BookingCancelled("Booking cancelled successfully")
                } else {
                    _uiState.value = BookingUiState.Error("Failed to cancel booking")
                }
            } catch (e: Exception) {
                _uiState.value = BookingUiState.Error("Network error: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = BookingUiState.Initial
    }
}
