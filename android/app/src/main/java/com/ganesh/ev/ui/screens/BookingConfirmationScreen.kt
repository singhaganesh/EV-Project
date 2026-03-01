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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.BookingUiState
import com.ganesh.ev.ui.viewmodel.BookingViewModel

@Composable
fun BookingConfirmationScreen(
        userId: Long,
        slotId: Long,
        startTime: String,
        endTime: String,
        vehicleType: String,
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
                                                color = MaterialTheme.colorScheme.primary
                                        )

                                        Spacer(modifier = Modifier.height(24.dp))

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
                                                                                .titleMedium
                                                        )
                                                }
                                                ClayDivider()
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween
                                                ) {
                                                        Text("Start")
                                                        Text(startTime.replace("T", " "))
                                                }
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween
                                                ) {
                                                        Text("End")
                                                        Text(endTime.replace("T", " "))
                                                }
                                                if (booking.priceEstimate != null) {
                                                        ClayDivider()
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement =
                                                                        Arrangement.SpaceBetween
                                                        ) {
                                                                Text(
                                                                        "Price",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .titleMedium
                                                                )
                                                                Text(
                                                                        "₹${String.format("%.2f", booking.priceEstimate)}",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .titleMedium,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                )
                                                        }
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
                                        ClayOutlinedButton(onClick = onGoHome) { Text("Go Back") }
                                }
                        }
                        else -> {}
                }
        }
}
