package com.ganesh.ev.ui.screens

import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.ev.data.network.RetrofitClient
import com.ganesh.ev.data.repository.UserPreferencesRepository
import com.ganesh.ev.ui.theme.ClayProgressIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.json.JSONObject

@Composable
fun SplashScreen(
        userPreferencesRepository: UserPreferencesRepository,
        onAuthValid: (token: String) -> Unit,
        onAuthExpired: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    // Trigger fade-in immediately
    LaunchedEffect(Unit) { visible = true }

    // Run the token check in the background
    LaunchedEffect(Unit) {
        // Minimum splash display time so the branding doesn't flash
        val minDisplayTime = 1500L
        val startTime = System.currentTimeMillis()

        var tokenValid = false
        var validToken: String? = null

        try {
            val token = userPreferencesRepository.authToken.first()
            if (!token.isNullOrEmpty() && isTokenStillValid(token)) {
                RetrofitClient.setAuthToken(token)
                tokenValid = true
                validToken = token
            }
        } catch (_: Exception) {
            // Any error → treat as expired
        }

        // Wait the remaining time so splash shows for at least 1.5s
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < minDisplayTime) {
            delay(minDisplayTime - elapsed)
        }

        if (tokenValid && validToken != null) {
            onAuthValid(validToken)
        } else {
            onAuthExpired()
        }
    }

    // ── UI ──
    Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(
                    visible = visible,
                    enter = scaleIn(animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Lightning bolt emoji as placeholder icon
                    Text(text = "⚡", fontSize = 64.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                            text = "Plugsy",
                            style =
                                    MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 36.sp,
                                            letterSpacing = 1.sp
                                    ),
                            color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                            text = "Smart EV Charging",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            ClayProgressIndicator()
        }
    }
}

/**
 * Decode a JWT token's payload (Base64) and check if the `exp` claim is still in the future.
 * Returns false if anything fails.
 */
private fun isTokenStillValid(token: String): Boolean {
    return try {
        val parts = token.split(".")
        if (parts.size < 2) return false

        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
        val json = JSONObject(payload)
        val exp = json.getLong("exp") // seconds since epoch

        val nowSeconds = System.currentTimeMillis() / 1000
        exp > nowSeconds
    } catch (_: Exception) {
        false
    }
}
