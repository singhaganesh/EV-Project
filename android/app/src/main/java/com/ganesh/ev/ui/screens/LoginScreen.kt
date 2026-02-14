package com.ganesh.ev.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.data.model.User
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.AuthUiState
import com.ganesh.ev.ui.viewmodel.AuthViewModel
import com.ganesh.ev.ui.viewmodel.AuthViewModelFactory

@Composable
fun LoginScreen(
        onLoginSuccess: (User, String?) -> Unit,
        viewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(LocalContext.current))
) {
    var mobileNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var showOtpField by remember { mutableStateOf(false) }
    var showProfileForm by remember { mutableStateOf(false) }
    var sentOtp by remember { mutableStateOf("") }
    var currentUser by remember { mutableStateOf<User?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.OtpSent -> {
                showOtpField = true
                sentOtp = state.otp
            }
            is AuthUiState.OtpValidated -> {
                if (state.isNewUser) {
                    showOtpField = false
                    showProfileForm = true
                } else {
                    showOtpField = false
                    showProfileForm = false
                    if (state.token != null && state.user != null) {
                        currentUser = state.user
                        onLoginSuccess(state.user, state.token)
                    } else if (state.token != null) {
                        val user =
                                User(
                                        id = 0,
                                        mobileNumber = mobileNumber,
                                        name = "User",
                                        email = null,
                                        isFirstTimeUser = false,
                                        role = "CUSTOMER",
                                        createdAt = null,
                                        updatedAt = null
                                )
                        currentUser = user
                        onLoginSuccess(user, state.token)
                    }
                }
            }
            is AuthUiState.ProfileCompleted -> {
                showOtpField = false
                showProfileForm = false
                state.user?.let { currentUser = it }
                currentUser?.let { onLoginSuccess(it, state.token) }
            }
            is AuthUiState.Error -> {
                showOtpField = false
                showProfileForm = false
            }
            else -> {}
        }
    }

    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    MaterialTheme.colorScheme
                                                                            .background,
                                                                    MaterialTheme.colorScheme
                                                                            .surfaceVariant.copy(
                                                                            alpha = 0.5f
                                                                    )
                                                            )
                                            )
                            )
    ) {
        Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Text(text = "⚡", style = MaterialTheme.typography.displayLarge)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text = "EV Charging",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
            )

            Text(
                    text = "Power up your journey",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            when {
                showProfileForm -> {
                    ClayProfileForm(
                            name = name,
                            email = email,
                            onNameChange = { name = it },
                            onEmailChange = { email = it },
                            onSubmit = { viewModel.completeProfile(name, email) },
                            isLoading = uiState is AuthUiState.Loading
                    )
                }
                showOtpField -> {
                    ClayOtpForm(
                            otp = otp,
                            sentOtp = sentOtp,
                            onOtpChange = { otp = it },
                            onSubmit = { viewModel.validateOtp(mobileNumber, otp) },
                            isLoading = uiState is AuthUiState.Loading,
                            onResendOtp = { viewModel.sendOtp(mobileNumber) }
                    )
                }
                else -> {
                    ClayMobileForm(
                            mobileNumber = mobileNumber,
                            onMobileChange = { mobileNumber = it },
                            onSubmit = { viewModel.sendOtp(mobileNumber) },
                            isLoading = uiState is AuthUiState.Loading
                    )
                }
            }

            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                ClayCard(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        cornerRadius = 16.dp
                ) {
                    Text(
                            text = (uiState as AuthUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ClayMobileForm(
        mobileNumber: String,
        onMobileChange: (String) -> Unit,
        onSubmit: () -> Unit,
        isLoading: Boolean
) {
    ClayCard {
        Text(text = "Enter your mobile number", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(16.dp))

        ClayTextField(
                value = mobileNumber,
                onValueChange = onMobileChange,
                label = { Text("Mobile Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        ClayButton(
                onClick = onSubmit,
                enabled = mobileNumber.length >= 10 && !isLoading,
                modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                )
            } else {
                Text("Send OTP")
            }
        }
    }
}

@Composable
fun ClayOtpForm(
        otp: String,
        sentOtp: String,
        onOtpChange: (String) -> Unit,
        onSubmit: () -> Unit,
        isLoading: Boolean,
        onResendOtp: () -> Unit
) {
    ClayCard {
        Text("Enter OTP", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(12.dp))

        if (sentOtp.isNotEmpty()) {
            ClayCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    cornerRadius = 16.dp
            ) {
                Text(
                        text = "Your OTP: $sentOtp",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onResendOtp) { Text("Resend OTP") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ClayTextField(
                value = otp,
                onValueChange = onOtpChange,
                label = { Text("OTP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        ClayButton(
                onClick = onSubmit,
                enabled = otp.length == 6 && !isLoading,
                modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                )
            } else {
                Text("Verify OTP")
            }
        }
    }
}

@Composable
fun ClayProfileForm(
        name: String,
        email: String,
        onNameChange: (String) -> Unit,
        onEmailChange: (String) -> Unit,
        onSubmit: () -> Unit,
        isLoading: Boolean
) {
    ClayCard {
        Text("Complete Your Profile", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(16.dp))

        ClayTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        ClayTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email (Optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        ClayButton(
                onClick = onSubmit,
                enabled = name.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                )
            } else {
                Text("Complete Profile")
            }
        }
    }
}
