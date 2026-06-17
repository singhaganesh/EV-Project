package com.ganesh.ev.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.model.BookingTemplate
import com.ganesh.ev.data.model.BookingTemplateRequest
import com.ganesh.ev.data.model.Station
import com.ganesh.ev.data.model.Vehicle
import com.ganesh.ev.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Recurring bookings (G2): manage templates + supply the pickers (stations,
 * vehicles) the create/registration form needs.
 */
@HiltViewModel
class RecurringBookingsViewModel @Inject constructor(
        private val apiService: ApiService
) : ViewModel() {

    private val _templates = MutableStateFlow<List<BookingTemplate>>(emptyList())
    val templates: StateFlow<List<BookingTemplate>> = _templates.asStateFlow()

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations.asStateFlow()

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Resolves a station id to its name for display (falls back to "#id"). */
    fun stationName(id: Long): String =
            _stations.value.firstOrNull { it.id == id }?.name ?: "Station #$id"

    fun loadAll() {
        viewModelScope.launch {
            _loading.value = true
            loadTemplates()
            try {
                val s = apiService.getAllStations()
                if (s.isSuccessful) _stations.value = s.body() ?: emptyList()
            } catch (_: Exception) {}
            try {
                val v = apiService.getVehicles()
                if (v.isSuccessful) _vehicles.value = v.body()?.data ?: emptyList()
            } catch (_: Exception) {}
            _loading.value = false
        }
    }

    private suspend fun loadTemplates() {
        try {
            val response = apiService.getBookingTemplates()
            if (response.isSuccessful) _templates.value = response.body()?.data ?: emptyList()
        } catch (_: Exception) {}
    }

    fun create(request: BookingTemplateRequest) {
        viewModelScope.launch {
            try {
                val response = apiService.createBookingTemplate(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    _message.value = "Recurring booking saved"
                    loadTemplates()
                } else {
                    _message.value = response.body()?.message ?: "Could not save"
                }
            } catch (e: Exception) {
                _message.value = "Network error: ${e.message}"
            }
        }
    }

    fun setActive(template: BookingTemplate, active: Boolean) {
        viewModelScope.launch {
            // Optimistic toggle.
            _templates.value = _templates.value.map {
                if (it.id == template.id) it.copy(active = active) else it
            }
            try {
                apiService.updateBookingTemplate(template.id, template.toRequest(active))
            } catch (_: Exception) {
                loadTemplates() // resync on failure
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            val previous = _templates.value
            _templates.value = previous.filterNot { it.id == id }
            try {
                val response = apiService.deleteBookingTemplate(id)
                if (!response.isSuccessful) _templates.value = previous
            } catch (_: Exception) {
                _templates.value = previous
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun BookingTemplate.toRequest(active: Boolean) = BookingTemplateRequest(
            stationId = stationId,
            vehicleId = vehicleId,
            connectorType = connectorType,
            vehicleType = vehicleType,
            timeOfDay = timeOfDay.take(5), // "HH:mm"
            daysOfWeek = daysOfWeek,
            active = active
    )
}
