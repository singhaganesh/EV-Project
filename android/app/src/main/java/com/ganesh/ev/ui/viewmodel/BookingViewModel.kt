package com.ganesh.ev.ui.viewmodel

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
}

class BookingViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow<BookingUiState>(BookingUiState.Initial)
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()
    
    fun createBooking(userId: Long, slotId: Long, startTime: String, endTime: String) {
        viewModelScope.launch {
            _uiState.value = BookingUiState.Loading
            try {
                val request = BookingRequest(
                    userId = userId,
                    slotId = slotId,
                    startTime = startTime,
                    endTime = endTime
                )
                val response = RetrofitClient.apiService.createBooking(request)
                if (response.isSuccessful) {
                    val booking = response.body()?.data
                    if (booking != null) {
                        _uiState.value = BookingUiState.BookingCreated(booking)
                    } else {
                        _uiState.value = BookingUiState.Error("Failed to create booking")
                    }
                } else {
                    _uiState.value = BookingUiState.Error("Failed to create booking: ${response.message()}")
                }
            } catch (e: Exception) {
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
                    val bookings = response.body()?.data ?: emptyList()
                    _uiState.value = BookingUiState.BookingsLoaded(bookings)
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
                    _uiState.value = BookingUiState.BookingCancelled("Booking cancelled successfully")
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
