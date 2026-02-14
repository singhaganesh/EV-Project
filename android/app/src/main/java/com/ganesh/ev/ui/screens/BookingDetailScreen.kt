package com.ganesh.ev.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.data.model.Booking
import com.ganesh.ev.data.model.BookingStatus
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.BookingUiState
import com.ganesh.ev.ui.viewmodel.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
        bookingId: Long,
        userId: Long,
        onBackClick: () -> Unit,
        onStartCharging: (Long) -> Unit,
        onGoToCharging: (Long) -> Unit,
        viewModel: BookingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) { viewModel.loadUserBookings(userId) }

    Scaffold(
            topBar = {
                ClayTopBar(
                        title = "Booking Details",
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (val state = uiState) {
            is BookingUiState.Loading -> {
                Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                ) { ClayProgressIndicator() }
            }
            is BookingUiState.BookingsLoaded -> {
                val booking = state.bookings.find { it.id == bookingId }
                if (booking == null) {
                    Box(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            contentAlignment = Alignment.Center
                    ) {
                        ClayCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                            Text("Booking not found", color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    var showCancelDialog by remember { mutableStateOf(false) }

                    val stationName = booking.slot?.station?.name
                    val stationAddress = booking.slot?.station?.address
                    val slotNumber = booking.slot?.slotNumber

                    Column(
                            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Status badge
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                    text = "Booking #${booking.id}",
                                    style = MaterialTheme.typography.headlineSmall
                            )
                            val statusColor = when (booking.status) {
                                BookingStatus.CONFIRMED -> MaterialTheme.colorScheme.primary
                                BookingStatus.ONGOING -> ClayInfo
                                BookingStatus.COMPLETED -> ClaySuccess
                                BookingStatus.CANCELLED -> MaterialTheme.colorScheme.error
                                BookingStatus.PENDING -> MaterialTheme.colorScheme.secondary
                                BookingStatus.EXPIRED -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            ClayChip(text = booking.status.name, color = statusColor)
                        }

                        // Station details
                        ClayCard {
                            Text(
                                    text = "Station Details",
                                    style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (stationName != null) {
                                Text(
                                        text = stationName,
                                        style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            if (stationAddress != null) {
                                Text(
                                        text = stationAddress,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (slotNumber != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                ClayChip(
                                        text = "Slot $slotNumber",
                                        color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Time & Pricing
                        ClayCard {
                            Text(
                                    text = "Time & Pricing",
                                    style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Start")
                                Text(booking.startTime?.replace("T", " ") ?: "-")
                            }
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("End")
                                Text(booking.endTime?.replace("T", " ") ?: "-")
                            }
                            ClayDivider()
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                        "Total Price",
                                        style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                        "₹${String.format("%.2f", booking.priceEstimate)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Actions
                        when (booking.status) {
                            BookingStatus.CONFIRMED -> {
                                ClayButton(
                                        onClick = { onStartCharging(booking.id) },
                                        modifier = Modifier.fillMaxWidth()
                                ) { Text("Start Charging") }
                                Spacer(modifier = Modifier.height(8.dp))
                                ClayOutlinedButton(
                                        onClick = { showCancelDialog = true },
                                        borderColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.fillMaxWidth()
                                ) { Text("Cancel Booking") }
                            }
                            BookingStatus.ONGOING -> {
                                ClayButton(
                                        onClick = { onGoToCharging(booking.id) },
                                        modifier = Modifier.fillMaxWidth()
                                ) { Text("Go to Charging") }
                            }
                            else -> {}
                        }
                    }

                    if (showCancelDialog) {
                        AlertDialog(
                                onDismissRequest = { showCancelDialog = false },
                                title = { Text("Cancel Booking") },
                                text = { Text("Are you sure you want to cancel this booking?") },
                                confirmButton = {
                                    TextButton(
                                            onClick = {
                                                viewModel.cancelBooking(booking.id)
                                                showCancelDialog = false
                                                onBackClick()
                                            }
                                    ) {
                                        Text("Yes, Cancel", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCancelDialog = false }) {
                                        Text("No")
                                    }
                                }
                        )
                    }
                }
            }
            else -> {}
        }
    }
}
