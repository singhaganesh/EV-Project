package com.ganesh.ev.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.model.Station
import com.ganesh.ev.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Favorites + recents (F3).
 *
 * Favorites come from the backend; "recents" are the distinct stations from the
 * user's recent bookings (no extra storage). [favoriteIds] backs the heart
 * toggle on the station detail screen.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
        private val apiService: ApiService
) : ViewModel() {

    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds.asStateFlow()

    private val _favorites = MutableStateFlow<List<Station>>(emptyList())
    val favorites: StateFlow<List<Station>> = _favorites.asStateFlow()

    private val _recents = MutableStateFlow<List<Station>>(emptyList())
    val recents: StateFlow<List<Station>> = _recents.asStateFlow()

    fun loadFavorites() {
        viewModelScope.launch {
            try {
                val response = apiService.getFavorites()
                if (response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    _favorites.value = list
                    _favoriteIds.value = list.map { it.id }.toSet()
                }
            } catch (_: Exception) {
                // Non-fatal; UI just shows nothing.
            }
        }
    }

    fun loadRecents(userId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.getUserBookings(userId, 0, 20)
                if (response.isSuccessful) {
                    val bookings = response.body()?.data?.content ?: emptyList()
                    _recents.value = bookings.mapNotNull { it.slot?.station }.distinctBy { it.id }
                }
            } catch (_: Exception) {
                // Non-fatal.
            }
        }
    }

    fun toggle(stationId: Long) {
        val isFav = _favoriteIds.value.contains(stationId)
        // Optimistic update so the heart responds instantly.
        _favoriteIds.value =
                if (isFav) _favoriteIds.value - stationId else _favoriteIds.value + stationId
        if (isFav) _favorites.value = _favorites.value.filterNot { it.id == stationId }

        viewModelScope.launch {
            try {
                if (isFav) apiService.removeFavorite(stationId) else apiService.addFavorite(stationId)
            } catch (_: Exception) {
                // Roll back on failure.
                _favoriteIds.value =
                        if (isFav) _favoriteIds.value + stationId
                        else _favoriteIds.value - stationId
            }
        }
    }
}
