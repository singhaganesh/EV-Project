package com.ganesh.ev.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.ev.data.model.FirebaseLoginRequest
import com.ganesh.ev.data.model.UpdateProfileRequest
import com.ganesh.ev.data.model.User
import com.ganesh.ev.data.network.RetrofitClient
import com.ganesh.ev.data.repository.UserPreferencesRepository
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    // SMS code dispatched; the UI shows the segmented OTP input + resend countdown.
    object CodeSent : AuthUiState()
    // Verified, but a brand-new account — collect name/email before continuing.
    data class NewUserProfile(val user: User) : AuthUiState()
    data class Success(val user: User, val token: String, val refreshToken: String?) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

/**
 * Customer phone login via Firebase Phone Auth.
 *
 * Firebase sends/verifies the SMS OTP (no DLT needed); on success we exchange the
 * Firebase ID token for our own JWT at POST /api/auth/firebase-login. Brand-new
 * accounts then collect name/email via the authenticated profile-update endpoint.
 */
class AuthViewModel(
    private val userPreferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var phoneE164: String = ""
    // True once an SMS has been dispatched (onCodeSent). Used to distinguish
    // SMS-based auto-retrieval (which we ignore, so the "Allow" consent prompt
    // drives verification) from instant verification (no SMS — must be honored,
    // else the user is stuck with no code to enter).
    private var smsDispatched = false

    // Held between firebase-login and profile completion for new users.
    private var pendingToken: String? = null
    private var pendingRefresh: String? = null
    private var pendingUser: User? = null

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // If an SMS was already sent, this is SMS-based auto-retrieval: ignore it
            // so the visible "Allow" consent prompt (or manual entry) completes the
            // login consistently. If no SMS was sent, this is instant verification —
            // honor it, otherwise the user has nothing to enter and would be stuck.
            if (smsDispatched) return
            signInWithCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            _uiState.value = AuthUiState.Error(
                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Invalid phone number."
                    is FirebaseTooManyRequestsException ->
                        "Too many attempts. Please try again later."
                    else -> e.message ?: "Verification failed. Please try again."
                }
            )
        }

        override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
            verificationId = id
            resendToken = token
            smsDispatched = true
            _uiState.value = AuthUiState.CodeSent
        }
    }

    /** Starts SMS verification for a full E.164 number (e.g. +919876543210). */
    fun sendCode(activity: Activity, phoneNumberE164: String) {
        phoneE164 = phoneNumberE164
        smsDispatched = false
        _uiState.value = AuthUiState.Loading
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumberE164)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
        )
    }

    /** Re-sends the SMS using the force-resending token from the first send. */
    fun resendCode(activity: Activity) {
        _uiState.value = AuthUiState.Loading
        val builder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneE164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        resendToken?.let { builder.setForceResendingToken(it) }
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    /** Verifies the manually-entered 6-digit code. */
    fun verifyCode(code: String) {
        val id = verificationId
        if (id == null) {
            _uiState.value = AuthUiState.Error("Please request a code first.")
            return
        }
        signInWithCredential(PhoneAuthProvider.getCredential(id, code))
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        _uiState.value = AuthUiState.Loading
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.user?.getIdToken(false)
                        ?.addOnCompleteListener { tokenTask ->
                            val idToken = tokenTask.result?.token
                            if (tokenTask.isSuccessful && idToken != null) {
                                exchangeWithBackend(idToken)
                            } else {
                                _uiState.value =
                                    AuthUiState.Error("Could not retrieve login token. Please try again.")
                            }
                        }
                } else {
                    val msg = (task.exception as? FirebaseAuthInvalidCredentialsException)
                        ?.let { "Incorrect code. Please check and try again." }
                        ?: task.exception?.message ?: "Verification failed."
                    _uiState.value = AuthUiState.Error(msg)
                }
            }
    }

    private fun exchangeWithBackend(idToken: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.firebaseLogin(FirebaseLoginRequest(idToken))
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    val token = data?.token
                    val user = data?.user
                    if (token == null || user == null) {
                        _uiState.value = AuthUiState.Error("Login failed. Please try again.")
                        return@launch
                    }
                    // Persist tokens so the next authenticated call (profile update) works.
                    RetrofitClient.setAuthToken(token)
                    userPreferencesRepository?.saveAuthToken(token)
                    data.refreshToken?.let {
                        RetrofitClient.setRefreshToken(it)
                        userPreferencesRepository?.saveRefreshToken(it)
                    }
                    userPreferencesRepository?.saveUser(user)

                    pendingToken = token
                    pendingRefresh = data.refreshToken
                    pendingUser = user

                    _uiState.value = if (data.isNewUser == true) {
                        AuthUiState.NewUserProfile(user)
                    } else {
                        AuthUiState.Success(user, token, data.refreshToken)
                    }
                } else {
                    _uiState.value = AuthUiState.Error(response.body()?.message ?: "Login failed.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Network error: ${e.message}")
            }
        }
    }

    /** Sets name/email on a brand-new account, then completes login. */
    fun submitProfile(name: String, email: String?) {
        val user = pendingUser
        val token = pendingToken
        if (user == null || token == null) {
            _uiState.value = AuthUiState.Error("Session expired. Please sign in again.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val finalEmail = if (email.isNullOrBlank()) null else email.trim()
                val response = RetrofitClient.apiService.updateProfile(
                    user.id,
                    UpdateProfileRequest(name = name, email = finalEmail)
                )
                val updated = response.body()?.data
                if (response.isSuccessful && response.body()?.success == true && updated != null) {
                    userPreferencesRepository?.saveUser(updated)
                    _uiState.value = AuthUiState.Success(updated, token, pendingRefresh)
                } else {
                    _uiState.value =
                        AuthUiState.Error(response.body()?.message ?: "Failed to save profile.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Network error: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.logout()
            } catch (e: Exception) {
                // Ignore logout network error
            }
            firebaseAuth.signOut()
            RetrofitClient.clearAuthTokens()
            userPreferencesRepository?.clearUserData()
            _uiState.value = AuthUiState.Initial
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Initial
    }
}
