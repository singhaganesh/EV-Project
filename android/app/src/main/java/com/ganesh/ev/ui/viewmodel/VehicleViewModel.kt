package com.ganesh.ev.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.model.Vehicle
import com.ganesh.ev.data.model.VehicleRequest
import com.ganesh.ev.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Vehicle garage CRUD (C1). */
@HiltViewModel
class VehicleViewModel @Inject constructor(
        private val apiService: ApiService
) : ViewModel() {

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadVehicles() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = apiService.getVehicles()
                if (response.isSuccessful) {
                    _vehicles.value = response.body()?.data ?: emptyList()
                } else {
                    _error.value = "Failed to load vehicles"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun addVehicle(make: String, model: String, batteryKwh: Double?, connectorType: String?) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = apiService.addVehicle(
                        VehicleRequest(make.trim(), model.trim(), batteryKwh, connectorType)
                )
                if (response.isSuccessful) {
                    loadVehicles()
                } else {
                    _error.value = response.body()?.message ?: "Failed to add vehicle"
                    _loading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                _loading.value = false
            }
        }
    }

    fun deleteVehicle(id: Long) {
        viewModelScope.launch {
            // Optimistic removal.
            val previous = _vehicles.value
            _vehicles.value = previous.filterNot { it.id == id }
            try {
                val response = apiService.deleteVehicle(id)
                if (!response.isSuccessful) {
                    _vehicles.value = previous // roll back
                    _error.value = "Failed to remove vehicle"
                }
            } catch (e: Exception) {
                _vehicles.value = previous
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
