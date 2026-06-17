package com.ganesh.ev.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.BuildConfig
import com.ganesh.ev.data.model.Station
import com.ganesh.ev.data.network.DirectionsClient
import com.ganesh.ev.data.network.RetrofitClient
import com.ganesh.ev.util.LocationHelper
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Route / trip planning (E2).
 *
 * Calls the Directions API for the driving route to a typed destination, decodes
 * the polyline, and surfaces the user's stations that fall within
 * [CORRIDOR_RADIUS_KM] of the route — answering "where can I charge on the way."
 * No SoC/range optimization in v1.
 */
class RoutePlanningViewModel : ViewModel() {

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Loaded(
                val routePoints: List<LatLng>,
                val stations: List<Station>,
                val distance: String?,
                val duration: String?
        ) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun planRoute(originLat: Double, originLng: Double, destination: String) {
        if (destination.isBlank()) return
        viewModelScope.launch {
            _state.value = State.Loading
            try {
                val response = DirectionsClient.api.getDirections(
                        origin = "$originLat,$originLng",
                        destination = destination,
                        key = BuildConfig.MAPS_API_KEY
                )
                val body = response.body()
                val route = body?.routes?.firstOrNull()
                val encoded = route?.overviewPolyline?.points
                if (!response.isSuccessful || body?.status != "OK" || encoded.isNullOrEmpty()) {
                    _state.value = State.Error(
                            when (body?.status) {
                                "ZERO_RESULTS" -> "No route found to that destination."
                                "NOT_FOUND" -> "Couldn't find that destination."
                                else -> "Could not plan a route. Please try again."
                            }
                    )
                    return@launch
                }

                val points = PolyUtil.decode(encoded)
                val leg = route.legs.firstOrNull()

                // Find stations within the corridor (heavy filtering off the main thread).
                val stations = loadStations()
                val along = withContext(Dispatchers.Default) {
                    stationsAlongRoute(stations, points)
                }

                _state.value = State.Loaded(
                        routePoints = points,
                        stations = along,
                        distance = leg?.distance?.text,
                        duration = leg?.duration?.text
                )
            } catch (e: Exception) {
                _state.value = State.Error("Network error: ${e.message}")
            }
        }
    }

    fun reset() {
        _state.value = State.Idle
    }

    private suspend fun loadStations(): List<Station> = try {
        val response = RetrofitClient.apiService.getAllStations()
        if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    /** Stations whose distance to the (sampled) route polyline is within the corridor. */
    private fun stationsAlongRoute(stations: List<Station>, route: List<LatLng>): List<Station> {
        if (route.isEmpty()) return emptyList()
        // Sample the polyline to cap the work at ~MAX_SAMPLES points.
        val step = (route.size / MAX_SAMPLES).coerceAtLeast(1)
        val sampled = route.filterIndexed { i, _ -> i % step == 0 }
        return stations.filter { station ->
            sampled.any { p ->
                LocationHelper.haversineDistance(
                        station.latitude, station.longitude, p.latitude, p.longitude
                ) <= CORRIDOR_RADIUS_KM
            }
        }
    }

    companion object {
        private const val CORRIDOR_RADIUS_KM = 5.0
        private const val MAX_SAMPLES = 150
    }
}
