package com.ganesh.ev.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.ChargingUiState
import com.ganesh.ev.ui.viewmodel.ChargingViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
    val telemetry by viewModel.telemetryData.collectAsState()

    var isCharging by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    // Derived from actual booking
    var ratePerKwh by remember { mutableStateOf(15.0) }
    var vehicleType by remember { mutableStateOf("CAR") }

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
            val booking = state.session.booking
            vehicleType = booking?.vehicleType ?: "CAR"
            ratePerKwh = booking?.slot?.station?.pricePerKwh ?: 15.0
        } else if (state is ChargingUiState.SessionLoaded) {
            val booking = state.session.booking
            vehicleType = booking?.vehicleType ?: "CAR"
            ratePerKwh = booking?.slot?.station?.pricePerKwh ?: 15.0

            if (state.session.endTime == null) {
                isCharging = true
            } else {
                isCompleted = true
            }
        } else if (state is ChargingUiState.SessionStopped) {
            isCompleted = true
            isCharging = false
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
                    Text("Connecting to charger...", style = MaterialTheme.typography.bodyLarge)
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
                if (isCompleted) {
                    // Completion state
                    val session = (uiState as? ChargingUiState.SessionStopped)?.session 
                                ?: (uiState as? ChargingUiState.SessionLoaded)?.session

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
                                        "${String.format(Locale.US, "%.2f", session?.energyKwh ?: 0.0)} kWh",
                                        style = MaterialTheme.typography.titleMedium
                                )
                            }
                            ClayDivider()
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Cost", style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold)
                                Text(
                                        "₹${String.format(Locale.US, "%.2f", session?.totalCost ?: 0.0)}",
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
                    // ── ACTIVE CHARGING STATE ──
                    Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                                text = "⚡ Charging in Progress",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // SoC / Power Dashboard
                        ClayCard(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                cornerRadius = 32.dp
                        ) {
                            Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                        text = "${telemetry?.socPercentage?.toInt() ?: "--"}%",
                                        style = MaterialTheme.typography.displayLarge,
                                        color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                        text = "State of Charge",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Real-time Power & Cost
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ClayCard(modifier = Modifier.weight(1f)) {
                                Text("Power", style = MaterialTheme.typography.labelSmall)
                                Text("${String.format("%.1f", telemetry?.powerKw ?: 0.0)} kW", 
                                     style = MaterialTheme.typography.titleMedium,
                                     color = MaterialTheme.colorScheme.secondary)
                            }
                            ClayCard(modifier = Modifier.weight(1f)) {
                                Text("Cost", style = MaterialTheme.typography.labelSmall)
                                Text("₹${String.format("%.2f", telemetry?.totalCost ?: 0.0)}", 
                                     style = MaterialTheme.typography.titleMedium,
                                     color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Detailed Vitals
                        ClayCard {
                            VitalsRow("Energy", "${String.format("%.3f", telemetry?.energyDispensedKwh ?: 0.0)} kWh")
                            ClayDivider()
                            VitalsRow("Voltage", "${telemetry?.voltageV?.toInt() ?: "--"} V")
                            VitalsRow("Current", "${String.format("%.1f", telemetry?.currentA ?: 0.0)} A")
                            VitalsRow("Temp", "${String.format("%.1f", telemetry?.connectorTempC ?: 0.0)}°C")
                            ClayDivider()
                            VitalsRow("Time Left", "~${telemetry?.minutesRemaining?.toInt() ?: "--"} mins")
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        ClayButton(
                                onClick = {
                                    val currentSessionId = when (val s = uiState) {
                                        is ChargingUiState.SessionStarted -> s.session.id
                                        is ChargingUiState.SessionLoaded -> s.session.id
                                        else -> sessionId ?: 0L
                                    }
                                    if (currentSessionId > 0) {
                                        viewModel.stopCharging(currentSessionId)
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

@Composable
fun VitalsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

