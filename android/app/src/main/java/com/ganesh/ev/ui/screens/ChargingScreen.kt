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
import java.util.Locale
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
    val powerData by viewModel.powerDataState.collectAsState()

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
        val state = uiState
        if (state is ChargingUiState.SessionStarted) {
            isCharging = true
            state.session.booking?.slot?.station?.id?.let { stationId ->
                viewModel.startPollingPower(stationId)
            }
        } else if (state is ChargingUiState.SessionLoaded) {
            // Check if session is active? For now assume valid if loaded here
            if (state.session.endTime == null) {
                isCharging = true
                state.session.booking?.slot?.station?.id?.let { stationId ->
                    viewModel.startPollingPower(stationId)
                }
                // Resume energy consumed from server + local accumulation
                energyConsumed = state.session.energyConsumed ?: 0.0
            } else {
                isCompleted = true
                energyConsumed = state.session.energyConsumed ?: 0.0
            }
        } else if (state is ChargingUiState.SessionStopped) {
            isCompleted = true
            isCharging = false
            viewModel.stopPolling()
        }
    }

    // Stop polling on dispose
    DisposableEffect(Unit) { onDispose { viewModel.stopPolling() } }

    // Accumulate energy based on real power if available, otherwise simulate
    LaunchedEffect(isCharging, powerData) {
        while (isCharging) {
            delay(1000)
            if (powerData != null) {
                // Energy (kWh) += Power (kW) * (1 second / 3600)
                energyConsumed += (powerData!!.power / 3600.0)
            } else {
                // Fallback simulation if no sensor data
                if (energyConsumed < 50.0) {
                    energyConsumed += 0.005 // Slower simulation
                }
            }
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
            else -> {
                // Handle success states (SessionStarted, Loaded, Stopped, PowerDataLoaded handled
                // via separate flow)
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
                                        "${String.format(Locale.US, "%.2f", energyConsumed)} kWh",
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
                                        "₹${String.format(Locale.US, "%.2f", energyConsumed * ratePerKwh)}",
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
                    // Active charging state (or loaded active session)
                    if (state is ChargingUiState.SessionStarted ||
                                    state is ChargingUiState.SessionLoaded ||
                                    state is ChargingUiState.PowerDataLoaded
                    ) {
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
                                            text =
                                                    "${String.format(Locale.US, "%.3f", energyConsumed)}",
                                            style = MaterialTheme.typography.displayLarge,
                                            color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                            text = "kWh Delivered",
                                            style = MaterialTheme.typography.titleLarge,
                                            color =
                                                    MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.7f
                                                    )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Live Power Data Card
                            if (powerData != null) {
                                ClayCard(
                                        containerColor =
                                                MaterialTheme.colorScheme.secondaryContainer.copy(
                                                        alpha = 0.2f
                                                )
                                ) {
                                    Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                                "Live Station Output",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                text =
                                                        "${String.format(Locale.US, "%.1f", powerData!!.power)} kW",
                                                style = MaterialTheme.typography.headlineMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                                text =
                                                        "${String.format(Locale.US, "%.1f", powerData!!.voltage)}V • ${String.format(Locale.US, "%.1f", powerData!!.current)}A",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

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
                                            "₹${String.format(Locale.US, "%.2f", energyConsumed * ratePerKwh)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            ClayButton(
                                    onClick = {
                                        // Stop Charging
                                        val currentSessionId =
                                                when (state) {
                                                    is ChargingUiState.SessionStarted ->
                                                            state.session.id
                                                    is ChargingUiState.SessionLoaded ->
                                                            state.session.id
                                                    else -> 0L
                                                }
                                        if (currentSessionId > 0) {
                                            viewModel.stopCharging(currentSessionId)
                                        } else {
                                            // Fallback cleanup if session ID lost?
                                            isCompleted = true
                                            isCharging = false
                                            viewModel.stopPolling()
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.fillMaxWidth()
                            ) { Text("Stop Charging") }
                        }
                    }
                }
            }
        }
    }
}
