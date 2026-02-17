package com.ganesh.ev.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.model.ChargerSlot
import com.ganesh.ev.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SlotBookingUiState {
    object Initial : SlotBookingUiState()
    object Loading : SlotBookingUiState()
    data class Success(val slot: ChargerSlot) : SlotBookingUiState()
    data class Error(val message: String) : SlotBookingUiState()
}

class SlotBookingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<SlotBookingUiState>(SlotBookingUiState.Initial)
    val uiState: StateFlow<SlotBookingUiState> = _uiState.asStateFlow()

    fun loadSlot(slotId: Long) {
        viewModelScope.launch {
            _uiState.value = SlotBookingUiState.Loading
            try {
                // Assuming getSlotById returns Response<ChargerSlot> directly based on my
                // ApiService update
                // Wait, ApiService update: Response<ChargerSlot>
                // Backend controller: ResponseEntity<ChargerSlot>
                // So untyped Response body is ChargerSlot.
                // Usually ApiResponse wrapper is used.
                // Let's check ApiService definition again.
                // suspend fun getSlotById(@Path("id") id: Long): Response<ChargerSlot>
                // Backend: return new ResponseEntity<>(slot, HttpStatus.OK);
                // This returns the object directly, not wrapped in ApiResponse if I didn't wrap it.
                // Controller: public ResponseEntity<ChargerSlot> getSlotById
                // So yes, it returns ChargerSlot JSON.

                val response = RetrofitClient.apiService.getSlotById(slotId)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = SlotBookingUiState.Success(response.body()!!)
                } else {
                    _uiState.value = SlotBookingUiState.Error("Failed to load slot details")
                }
            } catch (e: Exception) {
                _uiState.value = SlotBookingUiState.Error("Network error: ${e.message}")
            }
        }
    }
}
