package com.ganesh.ev.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        isNewSession: Boolean = true,
        onBackClick: () -> Unit,
        onComplete: () -> Unit,
        viewModel: ChargingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val telemetry by viewModel.telemetryData.collectAsState()

    var isCharging by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    // Pulsing animation for the progress ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Derived from actual booking
    var ratePerKwh by remember { mutableStateOf(15.0) }
    var vehicleType by remember { mutableStateOf("CAR") }

    LaunchedEffect(Unit) {
        if (sessionId != null && sessionId > 0) {
            viewModel.loadSession(sessionId)
        } else if (bookingId > 0) {
            if (isNewSession) {
                viewModel.startCharging(bookingId)
            } else {
                viewModel.loadSessionByBooking(bookingId)
            }
        }
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is ChargingUiState.Error && state.message.contains("Session not found")) {
            // If we tried to load an existing session and it wasn't there, 
            // then we should actually START a new one.
            viewModel.startCharging(bookingId)
        }
        
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
                                                                    Color(0xFFF0FDFD),
                                                                    Color(0xFFE0F7F9)
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
                    // ── CENTERED ACTIVE CHARGING STATE ──
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Spacer(modifier = Modifier.height(24.dp))

                            // Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color(0xFF00BCD4),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Charging in Progress",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A2234)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Custom Circular SoC Indicator
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(240.dp)
                            ) {
                                val soc = telemetry?.socPercentage ?: 0.0
                                val animatedSoc by animateFloatAsState(
                                    targetValue = soc.toFloat() / 100f,
                                    animationSpec = spring(),
                                    label = "socProgress"
                                )

                                // Background Circle
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(
                                        color = Color(0xFFE0F7F9),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    // Pulsing Outer Glow
                                    drawArc(
                                        color = Color(0xFF00BCD4).copy(alpha = pulseAlpha * 0.2f),
                                        startAngle = -90f,
                                        sweepAngle = animatedSoc * 360f,
                                        useCenter = false,
                                        style = Stroke(width = 32.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    // Progress Arc
                                    drawArc(
                                        color = Color(0xFF00BCD4),
                                        startAngle = -90f,
                                        sweepAngle = animatedSoc * 360f,
                                        useCenter = false,
                                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "${soc.toInt()}",
                                            style = MaterialTheme.typography.displayLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF1A2234),
                                                fontSize = 72.sp
                                            )
                                        )
                                        Text(
                                            text = "%",
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1A2234)
                                            ),
                                            modifier = Modifier.padding(bottom = 12.dp, start = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "STATE OF CHARGE",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            letterSpacing = 1.sp,
                                            color = Color(0xFF00BCD4),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(40.dp))

                            // Quick Stats Grid (Row 1: Power & Cost)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                StatCard(
                                    label = "POWER",
                                    value = String.format("%.1f", telemetry?.powerKw ?: 0.0),
                                    unit = "kW",
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    label = "COST",
                                    value = "₹${String.format("%.2f", telemetry?.totalCost ?: 0.0)}",
                                    unit = "",
                                    modifier = Modifier.weight(1f),
                                    valueColor = Color(0xFF1A2234)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Quick Stats Grid (Row 2: Energy & Time Left)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                StatCard(
                                    label = "ENERGY",
                                    value = String.format("%.2f", telemetry?.energyDispensedKwh ?: 0.0),
                                    unit = "kWh",
                                    modifier = Modifier.weight(1f),
                                    valueColor = Color(0xFF22C55E)
                                )
                                StatCard(
                                    label = "TIME LEFT",
                                    value = "${telemetry?.minutesRemaining?.toInt() ?: "--"}",
                                    unit = "mins",
                                    modifier = Modifier.weight(1f),
                                    valueColor = Color(0xFF00BCD4)
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Premium Stop Button
                            Button(
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(Color(0xFFFF4B61), Color(0xFFFF758C))
                                            ),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Stop Charging",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color(0xFF00BCD4)
) {
    ClayCard(
        modifier = modifier,
        cornerRadius = 24.dp,
        containerColor = Color(0xFFF8FEFF)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = valueColor
                    )
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = valueColor
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VitalsRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color(0xFF1A2234),
    iconTint: Color = Color(0xFF22C55E)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconTint.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        )
    }
}
