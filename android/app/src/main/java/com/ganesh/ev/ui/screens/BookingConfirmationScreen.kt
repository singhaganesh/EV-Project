package com.ganesh.ev.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.BookingUiState
import com.ganesh.ev.ui.viewmodel.BookingViewModel
import com.ganesh.ev.util.formatBookingDateTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun BookingConfirmationScreen(
        userId: Long,
        stationId: Long,
        connectorType: String,
        vehicleType: String,
        onBack: () -> Unit,
        onViewBookings: () -> Unit,
        onGoHome: () -> Unit,
        viewModel: BookingViewModel = viewModel()
) {
        val uiState by viewModel.uiState.collectAsState()

                // We observe uiState which already holds BookingCreated(booking) 
                // because SlotBookingScreen called viewModel.createBooking() using the shared ViewModel


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
                                                                                .surfaceVariant
                                                                                .copy(alpha = 0.5f)
                                                                )
                                                )
                                ),
                contentAlignment = Alignment.Center
        ) {
                when (val state = uiState) {
                        is BookingUiState.Loading -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        ClayProgressIndicator()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                                text = "Finding the best connector for you...",
                                                style = MaterialTheme.typography.bodyLarge
                                        )
                                }
                        }
                        is BookingUiState.BookingCreated -> {
                                Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                        val booking = state.booking

                                        // ── GRACE PERIOD CALCULATION ──
                                        // To avoid timezone/mismatch errors, we calculate the TOTAL duration 
                                        // (e.g., 20 mins) from the server response and count down locally 
                                        // from the moment the booking was received.
                                        var graceSecondsLeft by remember { mutableStateOf<Long?>(null) }
                                        
                                        LaunchedEffect(booking.id) {
                                            val start = parseServerDateTime(booking.startTime)
                                            val expiry = parseServerDateTime(booking.expiresAt)
                                            
                                            if (start != null && expiry != null) {
                                                // Calculate total allowed seconds (usually 1200 for 20 mins)
                                                val totalGraceSeconds = ChronoUnit.SECONDS.between(start, expiry)
                                                
                                                // Start countdown from totalGraceSeconds
                                                var current = totalGraceSeconds
                                                while (current >= 0) {
                                                    graceSecondsLeft = current
                                                    delay(1000)
                                                    current--
                                                }
                                            } else {
                                                // Fallback if parsing fails: assume 20 minutes
                                                var current = 20 * 60L
                                                while (current >= 0) {
                                                    graceSecondsLeft = current
                                                    delay(1000)
                                                    current--
                                                }
                                            }
                                        }

                                        Box(
                                                modifier =
                                                        Modifier.size(80.dp)
                                                                .claySurface(
                                                                        cornerRadius = 40.dp,
                                                                        shadowElevation = 6.dp
                                                                )
                                                                .background(
                                                                        MaterialTheme.colorScheme
                                                                                .primaryContainer,
                                                                        shape = CircleShape
                                                                ),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Success",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(48.dp)
                                                )
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        Text(
                                                text = "Booking Confirmed!",
                                                style = MaterialTheme.typography.headlineMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Grace period countdown banner
                                        val secsLeft = graceSecondsLeft
                                        if (secsLeft != null && secsLeft > 0) {
                                                val mins = secsLeft / 60
                                                val secs = secsLeft % 60
                                                ClayCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                                Text("⏳", style = MaterialTheme.typography.titleLarge)
                                                                Column {
                                                                        Text(
                                                                                "Arrive within",
                                                                                style = MaterialTheme.typography.labelMedium,
                                                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                                                        )
                                                                        Text(
                                                                                "%02d:%02d remaining".format(mins, secs),
                                                                                style = MaterialTheme.typography.titleLarge,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = MaterialTheme.colorScheme.tertiary
                                                                        )
                                                                }
                                                        }
                                                }
                                        } else if (secsLeft != null && secsLeft <= 0) {
                                                ClayCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                                Text("⚠️", style = MaterialTheme.typography.titleLarge)
                                                                Text(
                                                                        "Grace period expired. Booking may be cancelled.",
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        color = MaterialTheme.colorScheme.error
                                                                )
                                                        }
                                                }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        ClayCard {
                                                val booking = state.booking

                                                // Assigned connector info from the response
                                                val assignedSlot = booking.slot
                                                if (assignedSlot != null) {
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                                Text("Assigned Connector")
                                                                Text(
                                                                        "#${assignedSlot.id} (${assignedSlot.connectorType})",
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        }
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                                Text("Power")
                                                                Text(
                                                                        "${assignedSlot.powerRating} kW",
                                                                        style = MaterialTheme.typography.titleSmall,
                                                                        fontWeight = FontWeight.SemiBold
                                                                )
                                                        }
                                                        ClayDivider()
                                                }

                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                        Text("Booking ID")
                                                        Text(
                                                                "#${booking.id}",
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                        Text("Vehicle")
                                                        Text(
                                                                if (booking.vehicleType == "TRUCK") "🚛 Truck" else "🚗 Car",
                                                                style = MaterialTheme.typography.titleSmall,
                                                                fontWeight = FontWeight.SemiBold
                                                        )
                                                }
                                                ClayDivider()
                                                // Arrival time — formatted nicely
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                        Text("Booked at",
                                                                style = MaterialTheme.typography.bodyMedium)
                                                        Text(
                                                                formatBookingDateTime(booking.startTime),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.SemiBold
                                                        )
                                                }
                                                ClayDivider()
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                        Text("Price",
                                                                style = MaterialTheme.typography.bodyMedium)
                                                        Text("Calculated after charging",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        ClayButton(
                                                onClick = onViewBookings,
                                                modifier = Modifier.fillMaxWidth()
                                        ) { Text("View My Bookings") }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        ClayOutlinedButton(
                                                onClick = onGoHome,
                                                modifier = Modifier.fillMaxWidth()
                                        ) { Text("Go to Home") }
                                }
                        }
                        is BookingUiState.Error -> {
                                Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                        ClayCard(
                                                containerColor =
                                                        MaterialTheme.colorScheme.errorContainer
                                        ) {
                                                Text(
                                                        text = state.message,
                                                        color = MaterialTheme.colorScheme.error,
                                                        style = MaterialTheme.typography.bodyMedium
                                                )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        ClayButton(
                                                onClick = {
                                                        viewModel.createBooking(
                                                                userId,
                                                                stationId,
                                                                connectorType,
                                                                vehicleType
                                                        )
                                                }
                                        ) { Text("Retry") }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ClayOutlinedButton(onClick = onBack) { Text("Go Back") }
                                }
                        }
                        else -> {}
                }
        }
}

/**
 * Robust date parser for server timestamps
 */
fun parseServerDateTime(raw: String?): LocalDateTime? {
    if (raw == null) return null
    return try {
        // Handle common variations (T-separator or space, with/without fractional seconds)
        val clean = raw.replace(Regex("\\.\\d+$"), "")
        if (clean.contains("T")) {
            LocalDateTime.parse(clean, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        } else {
            LocalDateTime.parse(clean, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        }
    } catch (e: Exception) {
        null
    }
}
