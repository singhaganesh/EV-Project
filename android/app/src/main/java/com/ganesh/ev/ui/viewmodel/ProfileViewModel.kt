package com.ganesh.ev.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    sealed class DeleteState {
        object Idle : DeleteState()
        object Deleting : DeleteState()
        object Deleted : DeleteState()
        data class Error(val message: String) : DeleteState()
    }

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    fun deleteAccount() {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting
            try {
                val response = RetrofitClient.apiService.deleteAccount()
                if (response.isSuccessful && response.body()?.success != false) {
                    _deleteState.value = DeleteState.Deleted
                } else {
                    _deleteState.value =
                            DeleteState.Error(response.body()?.message ?: "Failed to delete account")
                }
            } catch (e: Exception) {
                _deleteState.value = DeleteState.Error(e.message ?: "Network error")
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }
}
