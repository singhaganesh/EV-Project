package com.ganesh.ev.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
        userId: Long,
        onBookingClick: (Long) -> Unit,
        viewModel: BookingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) { viewModel.loadUserBookings(userId) }

    Scaffold(
            topBar = { ClayTopBar(title = "My Bookings") },
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
                if (state.bookings.isEmpty()) {
                    Box(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint =
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.5f
                                            )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                    text = "No bookings yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.bookings) { booking ->
                            ClayBookingCard(
                                    booking = booking,
                                    onClick = { onBookingClick(booking.id) }
                            )
                        }
                    }
                }
            }
            is BookingUiState.Error -> {
                Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ClayCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ClayButton(onClick = { viewModel.loadUserBookings(userId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun ClayBookingCard(booking: Booking, onClick: () -> Unit) {
    val statusColor =
            when (booking.status) {
                BookingStatus.CONFIRMED -> MaterialTheme.colorScheme.primary
                BookingStatus.ONGOING -> ClayInfo
                BookingStatus.COMPLETED -> ClaySuccess
                BookingStatus.CANCELLED -> MaterialTheme.colorScheme.error
                BookingStatus.PENDING -> MaterialTheme.colorScheme.secondary
                BookingStatus.EXPIRED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

    val stationName = booking.slot?.station?.name
    val slotNumber = booking.slot?.slotNumber

    ClayClickableCard(onClick = onClick) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Booking #${booking.id}", style = MaterialTheme.typography.titleMedium)
            ClayChip(text = booking.status.name, color = statusColor)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (stationName != null) {
            Text(text = stationName, style = MaterialTheme.typography.bodyLarge)
        }

        if (slotNumber != null) {
            Text(
                    text = "Slot $slotNumber",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (booking.status == BookingStatus.CONFIRMED && booking.expiresAt != null) {
            var timeRemainingMs by remember { mutableStateOf(0L) }
            LaunchedEffect(booking.status, booking.expiresAt) {
                try {
                    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    // Sometimes format can be missing seconds so fallback to HH:mm
                    val parsedDate =
                            try {
                                format.parse(booking.expiresAt)
                            } catch (e: Exception) {
                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                                        .parse(booking.expiresAt)
                            }
                    if (parsedDate != null) {
                        val expiresAtMillis = parsedDate.time
                        while (true) {
                            val remaining = expiresAtMillis - System.currentTimeMillis()
                            timeRemainingMs = if (remaining > 0) remaining else 0
                            if (remaining <= 0) break
                            delay(1000)
                        }
                    }
                } catch (e: Exception) {}
            }

            if (timeRemainingMs > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val min = timeRemainingMs / 60000
                val sec = (timeRemainingMs % 60000) / 1000
                Text(
                        text = "Expires in ${min}m ${sec}s",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                    text = booking.startTime?.replace("T", " ")?.take(16) ?: "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                    text = "₹${String.format("%.2f", booking.priceEstimate)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
