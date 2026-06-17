package com.ganesh.ev.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.model.ReviewRequest
import com.ganesh.ev.data.model.ReviewSummary
import com.ganesh.ev.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Station reviews (F2): load summary/list and post the caller's review. */
@HiltViewModel
class ReviewsViewModel @Inject constructor(
        private val apiService: ApiService
) : ViewModel() {

    private val _summary = MutableStateFlow(ReviewSummary())
    val summary: StateFlow<ReviewSummary> = _summary.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun load(stationId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.getReviews(stationId)
                if (response.isSuccessful) {
                    _summary.value = response.body()?.data ?: ReviewSummary()
                }
            } catch (_: Exception) {
                // Non-fatal; tab just shows empty.
            }
        }
    }

    fun submit(stationId: Long, rating: Int, comment: String?) {
        viewModelScope.launch {
            _submitting.value = true
            try {
                val response = apiService.postReview(stationId, ReviewRequest(rating, comment))
                if (response.isSuccessful && response.body()?.success == true) {
                    _message.value = "Thanks for your review!"
                    load(stationId) // refresh list + summary + flags
                } else {
                    _message.value = response.body()?.message ?: "Could not submit review"
                }
            } catch (e: Exception) {
                _message.value = "Network error: ${e.message}"
            } finally {
                _submitting.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
