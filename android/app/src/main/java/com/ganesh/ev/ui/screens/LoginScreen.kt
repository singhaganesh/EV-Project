package com.ganesh.ev.ui.screens

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.data.model.User
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.AuthUiState
import com.ganesh.ev.ui.viewmodel.AuthViewModel
import com.ganesh.ev.ui.viewmodel.AuthViewModelFactory
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import kotlinx.coroutines.delay

private enum class AuthStep { MOBILE, OTP, PROFILE }

// Indian mobile numbers: 10 digits starting with 6–9.
private val INDIAN_MOBILE_REGEX = Regex("^[6-9]\\d{9}$")

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun LoginScreen(
        onLoginSuccess: (User, String?, String?) -> Unit,
        viewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(LocalContext.current))
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var mobileDigits by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var step by remember { mutableStateOf(AuthStep.MOBILE) }
    // WhatsApp-style "is this number correct?" confirmation before sending the SMS.
    var showConfirmDialog by remember { mutableStateOf(false) }
    // Bumped each time a code is (re)sent, to (re)start the resend countdown.
    var resendNonce by remember { mutableStateOf(0) }
    var resendSeconds by remember { mutableStateOf(0) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.CodeSent -> {
                step = AuthStep.OTP
                resendNonce++
            }
            is AuthUiState.NewUserProfile -> step = AuthStep.PROFILE
            is AuthUiState.Success -> onLoginSuccess(state.user, state.token, state.refreshToken)
            else -> {}
        }
    }

    // Resend countdown — resets to 60s whenever a new code is sent.
    LaunchedEffect(resendNonce) {
        if (resendNonce > 0) {
            resendSeconds = 60
            while (resendSeconds > 0) {
                delay(1000)
                resendSeconds--
            }
        }
    }

    // Auto-submit once all 6 digits are entered (manual path; instant verification
    // is handled inside the ViewModel callback).
    LaunchedEffect(otp) {
        if (otp.length == 6 && uiState is AuthUiState.CodeSent) {
            viewModel.verifyCode(otp)
        }
    }

    // ── SMS User Consent: the "Allow" prompt that auto-reads the incoming OTP ──
    // This coexists with Firebase's own silent auto-retrieval; whichever reads the
    // SMS first wins (both end in a successful login). EVERY step is wrapped in
    // try/catch and the receiver is lifecycle-bound, so a failure can never crash
    // the OTP screen the way the earlier, unguarded version did.
    val smsConsentLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                val code = message?.let { Regex("\\d{6}").find(it)?.value }
                if (!code.isNullOrEmpty()) {
                    otp = code // LaunchedEffect(otp) auto-submits once 6 digits are set
                }
            }
        } catch (_: Exception) { /* never crash the OTP screen */ }
    }

    // (Re)start the consent listener each time a code is sent (resendNonce bumps on
    // every send, including the first) and tear the receiver down when we leave.
    DisposableEffect(resendNonce) {
        if (resendNonce <= 0) {
            onDispose { }
        } else {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    try {
                        if (intent?.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
                        val extras = intent.extras ?: return
                        val status = BundleCompat.getParcelable(
                                extras, SmsRetriever.EXTRA_STATUS, Status::class.java
                        ) ?: return
                        if (status.statusCode == CommonStatusCodes.SUCCESS) {
                            val consentIntent = BundleCompat.getParcelable(
                                    extras, SmsRetriever.EXTRA_CONSENT_INTENT, Intent::class.java
                            )
                            if (consentIntent != null) smsConsentLauncher.launch(consentIntent)
                        }
                    } catch (_: Exception) { /* never crash the OTP screen */ }
                }
            }
            try {
                ContextCompat.registerReceiver(
                        context,
                        receiver,
                        IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
                        SmsRetriever.SEND_PERMISSION,
                        null,
                        ContextCompat.RECEIVER_EXPORTED
                )
            } catch (_: Exception) { /* ignore registration failure */ }
            try {
                SmsRetriever.getClient(context).startSmsUserConsent(null)
            } catch (_: Exception) { /* ignore */ }

            onDispose {
                try {
                    context.unregisterReceiver(receiver)
                } catch (_: Exception) { /* receiver may already be gone */ }
            }
        }
    }

    val isLoading = uiState is AuthUiState.Loading

    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    MaterialTheme.colorScheme.background,
                                                                    MaterialTheme.colorScheme
                                                                            .surfaceVariant.copy(alpha = 0.5f)
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

            when (step) {
                AuthStep.PROFILE -> {
                    ClayProfileForm(
                            name = name,
                            email = email,
                            onNameChange = { name = it },
                            onEmailChange = { email = it },
                            onSubmit = { viewModel.submitProfile(name, email) },
                            isLoading = isLoading
                    )
                }
                AuthStep.OTP -> {
                    ClayOtpForm(
                            otp = otp,
                            onOtpChange = { otp = it },
                            onSubmit = { viewModel.verifyCode(otp) },
                            isLoading = isLoading,
                            resendSeconds = resendSeconds,
                            onResend = { activity?.let { viewModel.resendCode(it) } }
                    )
                }
                AuthStep.MOBILE -> {
                    ClayMobileForm(
                            mobileDigits = mobileDigits,
                            onMobileChange = { input ->
                                mobileDigits = input.filter { it.isDigit() }.take(10)
                            },
                            onSubmit = { showConfirmDialog = true },
                            isLoading = isLoading
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

        if (showConfirmDialog) {
            AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    text = {
                        Text(
                                "We will be verifying the phone number:\n\n" +
                                        "+91 $mobileDigits\n\n" +
                                        "Is this OK, or would you like to edit the number?"
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showConfirmDialog = false
                            activity?.let { viewModel.sendCode(it, "+91$mobileDigits") }
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) { Text("EDIT") }
                    }
            )
        }
    }
}

@Composable
fun ClayMobileForm(
        mobileDigits: String,
        onMobileChange: (String) -> Unit,
        onSubmit: () -> Unit,
        isLoading: Boolean
) {
    val isValid = INDIAN_MOBILE_REGEX.matches(mobileDigits)

    ClayCard {
        Text(text = "Enter your mobile number", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Fixed country code prefix (India).
            Box(
                    modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Text("+91", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(8.dp))

            ClayTextField(
                    value = mobileDigits,
                    onValueChange = onMobileChange,
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f)
            )
        }

        if (mobileDigits.isNotEmpty() && !isValid) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                    text = "Enter a valid 10-digit mobile number.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        ClayButton(
                onClick = onSubmit,
                enabled = isValid && !isLoading,
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
        onOtpChange: (String) -> Unit,
        onSubmit: () -> Unit,
        isLoading: Boolean,
        resendSeconds: Int,
        onResend: () -> Unit
) {
    ClayCard {
        Text("Enter the 6-digit code", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
                "We sent an SMS to your phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        OtpInputBoxes(
                value = otp,
                onValueChange = onOtpChange,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (resendSeconds > 0) {
            TextButton(onClick = {}, enabled = false) { Text("Resend in ${resendSeconds}s") }
        } else {
            TextButton(onClick = onResend, enabled = !isLoading) { Text("Resend OTP") }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                Text("Verify")
            }
        }
    }
}

/**
 * Segmented 6-digit OTP entry: a single hidden text field rendered as 6 boxes.
 * Typing fills the next box; backspace clears and steps back — driven naturally
 * by the underlying field value.
 */
@Composable
fun OtpInputBoxes(
        value: String,
        onValueChange: (String) -> Unit,
        enabled: Boolean,
        length: Int = 6,
        modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // Guard: requesting focus before the field attaches throws IllegalStateException.
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) { /* field not attached yet; user can tap to focus */ }
    }

    BasicTextField(
            value = TextFieldValue(value, selection = TextRange(value.length)),
            onValueChange = { tfv ->
                onValueChange(tfv.text.filter { it.isDigit() }.take(length))
            },
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = modifier
                    .focusRequester(focusRequester)
                    .semantics {
                        contentDescription =
                                "Enter the $length digit OTP. ${value.length} of $length entered."
                    },
            decorationBox = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(length) { index ->
                        val char = value.getOrNull(index)?.toString() ?: ""
                        val focused = index == value.length
                        Box(
                                modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF8FEFF))
                                        .border(
                                                width = if (focused) 2.dp else 1.dp,
                                                color = if (focused) MaterialTheme.colorScheme.primary
                                                        else Color(0xFFCBD5E1),
                                                shape = RoundedCornerShape(12.dp)
                                        ),
                                contentAlignment = Alignment.Center
                        ) {
                            Text(
                                    text = char,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A2234)
                            )
                        }
                    }
                }
            }
    )
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
