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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay

@Composable
fun BookingConfirmationScreen(
        userId: Long,
        slotId: Long,
        startTime: String,
        endTime: String,
        vehicleType: String,
        onBack: () -> Unit,
        onViewBookings: () -> Unit,
        onGoHome: () -> Unit,
        viewModel: BookingViewModel = viewModel()
) {
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(Unit) {
                viewModel.createBooking(userId, slotId, startTime, endTime, vehicleType)
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
                                                text = "Creating your booking...",
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

                                        // Grace period countdown — updates every second
                                        var graceSecondsLeft by remember { mutableStateOf<Long?>(null) }
                                        LaunchedEffect(booking.expiresAt) {
                                                val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                                                val expiry = booking.expiresAt?.let {
                                                        try { LocalDateTime.parse(it, fmt) } catch (e: Exception) { null }
                                                }
                                                if (expiry != null) {
                                                        while (true) {
                                                                val secsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), expiry)
                                                                graceSecondsLeft = secsLeft.coerceAtLeast(0)
                                                                if (secsLeft <= 0) break
                                                                delay(1000)
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
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween
                                                ) {
                                                        Text("Booking ID")
                                                        Text(
                                                                "#${booking.id}",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleMedium,
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
                                                val isoFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                                                val displayFmt = DateTimeFormatter.ofPattern("h:mm a, EEE dd MMM")
                                                val arrivalDisplay = try {
                                                        LocalDateTime.parse(booking.startTime, isoFmt)
                                                                .format(displayFmt)
                                                } catch (e: DateTimeParseException) {
                                                        booking.startTime.replace("T", " ")
                                                }
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                        Text("Arrive by",
                                                                style = MaterialTheme.typography.bodyMedium)
                                                        Text(arrivalDisplay,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.SemiBold)
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
                                                                slotId,
                                                                startTime,
                                                                endTime,
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
