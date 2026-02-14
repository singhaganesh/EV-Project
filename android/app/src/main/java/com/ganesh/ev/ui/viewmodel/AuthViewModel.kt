package com.ganesh.ev.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.model.AuthResponse
import com.ganesh.ev.data.model.CompleteProfileRequest
import com.ganesh.ev.data.model.User
import com.ganesh.ev.data.network.RetrofitClient
import com.ganesh.ev.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class OtpSent(val otp: String, val message: String) : AuthUiState()
    data class OtpValidated(val isNewUser: Boolean, val token: String?, val user: User?) : AuthUiState()
    data class ProfileCompleted(val user: User, val token: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val userPreferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private var currentMobileNumber: String = ""
    
    fun sendOtp(mobileNumber: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            currentMobileNumber = mobileNumber
            
            try {
                val response = RetrofitClient.apiService.sendOtp(mobileNumber)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        val otp = apiResponse.data?.get("otp") ?: ""
                        _uiState.value = AuthUiState.OtpSent(otp, apiResponse.message ?: "OTP sent successfully")
                    } else {
                        _uiState.value = AuthUiState.Error(apiResponse?.message ?: "Failed to send OTP")
                    }
                } else {
                    _uiState.value = AuthUiState.Error("Failed to send OTP: ${response.message()}")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Network error: ${e.message}")
            }
        }
    }
    
    fun validateOtp(mobileNumber: String, otp: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            try {
                val response = RetrofitClient.apiService.validateOtp(mobileNumber, otp)
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse?.success == true) {
                        val authData = authResponse.data
                        val isNewUser = authData?.isNewUser ?: true
                        val token = authData?.token
                        val user = authData?.user
                        
                        if (token != null && token.isNotEmpty()) {
                            // Existing user - save token and user
                            RetrofitClient.setAuthToken(token)
                            userPreferencesRepository?.saveAuthToken(token)
                            user?.let { userPreferencesRepository?.saveUser(it) }
                        }
                        
                        _uiState.value = AuthUiState.OtpValidated(
                            isNewUser = isNewUser,
                            token = token,
                            user = user
                        )
                    } else {
                        _uiState.value = AuthUiState.Error(authResponse?.message ?: "Invalid OTP")
                    }
                } else {
                    _uiState.value = AuthUiState.Error("Failed to validate OTP: ${response.message()}")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Network error: ${e.message}")
            }
        }
    }
    
    fun completeProfile(name: String, email: String?) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            try {
                val request = CompleteProfileRequest(
                    mobileNumber = currentMobileNumber,
                    name = name,
                    email = email
                )
                
                val response = RetrofitClient.apiService.completeProfile(request)
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse?.success == true) {
                        val authData = authResponse.data
                        val token = authData?.token
                        val user = authData?.user
                        
                        if (token != null && user != null) {
                            // Save token and user
                            RetrofitClient.setAuthToken(token)
                            userPreferencesRepository?.saveAuthToken(token)
                            userPreferencesRepository?.saveUser(user)
                            _uiState.value = AuthUiState.ProfileCompleted(user, token)
                        } else {
                            // Even if user is null, try to create one
                            val newUser = User(
                                id = 0,
                                mobileNumber = currentMobileNumber,
                                name = name,
                                email = email,
                                isFirstTimeUser = false,
                                role = "CUSTOMER",
                                createdAt = null,
                                updatedAt = null
                            )
                            if (token != null) {
                                RetrofitClient.setAuthToken(token)
                                userPreferencesRepository?.saveAuthToken(token)
                                userPreferencesRepository?.saveUser(newUser)
                                _uiState.value = AuthUiState.ProfileCompleted(newUser, token)
                            } else {
                                _uiState.value = AuthUiState.Error("Failed to get user data")
                            }
                        }
                    } else {
                        _uiState.value = AuthUiState.Error(authResponse?.message ?: "Failed to complete profile")
                    }
                } else {
                    _uiState.value = AuthUiState.Error("Failed to complete profile: ${response.message()}")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Network error: ${e.message}")
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            RetrofitClient.clearAuthToken()
            userPreferencesRepository?.clearUserData()
            _uiState.value = AuthUiState.Initial
        }
    }
    
    fun resetState() {
        _uiState.value = AuthUiState.Initial
    }
}
