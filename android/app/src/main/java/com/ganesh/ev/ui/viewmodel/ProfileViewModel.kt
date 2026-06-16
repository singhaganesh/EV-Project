package com.ganesh.ev.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.model.UpdateProfileRequest
import com.ganesh.ev.data.model.User
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

    sealed class UpdateState {
        object Idle : UpdateState()
        object Saving : UpdateState()
        data class Saved(val user: User) : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun updateProfile(userId: Long, name: String, email: String?) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Saving
            try {
                val response =
                        RetrofitClient.apiService.updateProfile(
                                userId,
                                UpdateProfileRequest(name = name, email = email)
                        )
                val updated = response.body()?.data
                if (response.isSuccessful && updated != null) {
                    _updateState.value = UpdateState.Saved(updated)
                } else {
                    _updateState.value =
                            UpdateState.Error(response.body()?.message ?: "Failed to update profile")
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "Network error")
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

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
