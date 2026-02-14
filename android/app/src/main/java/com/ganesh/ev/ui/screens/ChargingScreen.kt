package com.ganesh.ev.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.ChargingUiState
import com.ganesh.ev.ui.viewmodel.ChargingViewModel
import kotlinx.coroutines.delay

@Composable
fun ChargingScreen(
        bookingId: Long,
        sessionId: Long? = null,
        onBackClick: () -> Unit,
        onComplete: () -> Unit,
        viewModel: ChargingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var energyConsumed by remember { mutableStateOf(0.0) }
    var isCharging by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    val ratePerKwh = 15.0

    LaunchedEffect(Unit) {
        if (sessionId != null && sessionId > 0) {
            viewModel.loadSession(sessionId)
        } else {
            viewModel.startCharging(bookingId)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ChargingUiState.SessionStarted) {
            isCharging = true
        }
    }

    // Simulate energy consumption
    LaunchedEffect(isCharging) {
        while (isCharging && energyConsumed < 50.0) {
            delay(2000)
            energyConsumed += 1.0
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
                            ),
            contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is ChargingUiState.Loading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ClayProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Starting charging session...", style = MaterialTheme.typography.bodyLarge)
                }
            }
            is ChargingUiState.Error -> {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                ) {
                    ClayCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ClayButton(onClick = onBackClick) { Text("Go Back") }
                }
            }
            is ChargingUiState.SessionStarted, is ChargingUiState.SessionLoaded -> {
                if (isCompleted) {
                    // Completion state
                    Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                                modifier =
                                        Modifier.size(80.dp)
                                                .claySurface(
                                                        cornerRadius = 40.dp,
                                                        shadowElevation = 6.dp
                                                )
                                                .background(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        shape = CircleShape
                                                ),
                                contentAlignment = Alignment.Center
                        ) { Text("⚡", style = MaterialTheme.typography.displaySmall) }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                                text = "Charging Complete!",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        ClayCard {
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Energy Consumed")
                                Text(
                                        "${String.format("%.1f", energyConsumed)} kWh",
                                        style = MaterialTheme.typography.titleMedium
                                )
                            }
                            ClayDivider()
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Cost", style = MaterialTheme.typography.titleMedium)
                                Text(
                                        "₹${String.format("%.2f", energyConsumed * ratePerKwh)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        ClayButton(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                            Text("Done")
                        }
                    }
                } else {
                    // Active charging state
                    Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                                text = "⚡ Charging in Progress",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        ClayCard(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                cornerRadius = 32.dp
                        ) {
                            Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                        text = "${String.format("%.1f", energyConsumed)}",
                                        style = MaterialTheme.typography.displayLarge,
                                        color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                        text = "kWh",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        ClayCard {
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Rate")
                                Text("₹$ratePerKwh/kWh")
                            }
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Estimated Cost")
                                Text(
                                        "₹${String.format("%.2f", energyConsumed * ratePerKwh)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        ClayButton(
                                onClick = {
                                    isCharging = false
                                    val currentSessionId =
                                            when (state) {
                                                is ChargingUiState.SessionStarted ->
                                                        state.session.id
                                                is ChargingUiState.SessionLoaded -> state.session.id
                                                else -> 0L
                                            }
                                    viewModel.stopCharging(currentSessionId)
                                    isCompleted = true
                                },
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.fillMaxWidth()
                        ) { Text("Stop Charging") }
                    }
                }
            }
            is ChargingUiState.SessionStopped -> {
                isCompleted = true
            }
            else -> {}
        }
    }
}
