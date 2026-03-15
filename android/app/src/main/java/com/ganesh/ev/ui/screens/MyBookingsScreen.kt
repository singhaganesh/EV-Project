package com.ganesh.ev.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
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

enum class BookingFilter {
    ALL,
    ACTIVE,
    PAST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
        userId: Long,
        onBookingClick: (Long) -> Unit,
        viewModel: BookingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf(BookingFilter.ALL) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(userId) { viewModel.loadUserBookings(userId) }

    // Handle refresh
    fun refresh() {
        isRefreshing = true
        viewModel.loadUserBookings(userId)
    }

    // Update refreshing state based on UI state
    LaunchedEffect(uiState) {
        if (uiState is BookingUiState.BookingsLoaded || uiState is BookingUiState.Error) {
            isRefreshing = false
        }
    }

    Scaffold(
            topBar = { ClayTopBar(title = "My Bookings") },
            containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Filter Tabs
            TabRow(
                    selectedTabIndex = selectedFilter.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                        selected = selectedFilter == BookingFilter.ALL,
                        onClick = { selectedFilter = BookingFilter.ALL },
                        text = { Text("All") }
                )
                Tab(
                        selected = selectedFilter == BookingFilter.ACTIVE,
                        onClick = { selectedFilter = BookingFilter.ACTIVE },
                        text = { Text("Active") }
                )
                Tab(
                        selected = selectedFilter == BookingFilter.PAST,
                        onClick = { selectedFilter = BookingFilter.PAST },
                        text = { Text("Past") }
                )
            }

            when (val state = uiState) {
                is BookingUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ClayProgressIndicator()
                    }
                }
                is BookingUiState.BookingsLoaded -> {
                    // Filter bookings based on selected filter
                    val filteredBookings =
                            when (selectedFilter) {
                                BookingFilter.ALL -> state.bookings
                                BookingFilter.ACTIVE ->
                                        state.bookings.filter { booking ->
                                            booking.status == BookingStatus.CONFIRMED ||
                                                    booking.status == BookingStatus.ONGOING ||
                                                    booking.status == BookingStatus.PENDING
                                        }
                                BookingFilter.PAST ->
                                        state.bookings.filter { booking ->
                                            booking.status == BookingStatus.COMPLETED ||
                                                    booking.status == BookingStatus.CANCELLED ||
                                                    booking.status == BookingStatus.EXPIRED
                                        }
                            }

                    if (filteredBookings.isEmpty()) {
                        Box(
                                modifier = Modifier.fillMaxSize(),
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
                                        text =
                                                when (selectedFilter) {
                                                    BookingFilter.ALL -> "No bookings yet"
                                                    BookingFilter.ACTIVE -> "No active bookings"
                                                    BookingFilter.PAST -> "No past bookings"
                                                },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp)
                        ) {
                            items(filteredBookings) { booking ->
                                BookingListItem(
                                        booking = booking,
                                        onClick = { onBookingClick(booking.id) }
                                )
                                if (booking != filteredBookings.last()) {
                                    HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
                is BookingUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
    val vehicleType = booking.vehicleType
    val connectorType = booking.slot?.connectorType?.name

    ClayClickableCard(onClick = onClick) {
        // Top row: Booking ID and Status
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Vehicle type icon
                Icon(
                        imageVector =
                                if (vehicleType == "TRUCK") Icons.Default.LocalShipping
                                else Icons.Default.DirectionsCar,
                        contentDescription = vehicleType,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Booking #${booking.id}", style = MaterialTheme.typography.titleMedium)
            }
            StatusChip(text = booking.status.name, color = statusColor)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Station name
        if (stationName != null) {
            Text(text = stationName, style = MaterialTheme.typography.bodyLarge)
        }

        // Slot and vehicle info row
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (slotNumber != null) {
                Text(
                        text = "Slot $slotNumber",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (connectorType != null) {
                Text(
                        text = connectorType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (vehicleType != null) {
                Text(
                        text = vehicleType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Countdown timer for confirmed bookings
        if (booking.status == BookingStatus.CONFIRMED && booking.expiresAt != null) {
            var timeRemainingMs by remember { mutableStateOf(0L) }
            LaunchedEffect(booking.status, booking.expiresAt) {
                try {
                    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
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

        // Time and price row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                    text = booking.startTime?.replace("T", " ")?.take(16) ?: "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                    text = "₹${String.format("%.2f", booking.priceEstimate ?: 0.0)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Simple list item without card design
@Composable
fun BookingListItem(booking: Booking, onClick: () -> Unit) {
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
    val vehicleType = booking.vehicleType
    val connectorType = booking.slot?.connectorType?.name

    Column(
            modifier =
                    Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp)
    ) {
        // Top row: Booking ID and Status
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        imageVector =
                                if (vehicleType == "TRUCK") Icons.Default.LocalShipping
                                else Icons.Default.DirectionsCar,
                        contentDescription = vehicleType,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Booking #${booking.id}", style = MaterialTheme.typography.titleMedium)
            }
            StatusChip(text = booking.status.name, color = statusColor)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Station name
        if (stationName != null) {
            Text(
                    text = stationName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Slot, connector and vehicle info
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (slotNumber != null) {
                Text(
                        text = "Slot $slotNumber",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (connectorType != null) {
                Text(
                        text = connectorType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Time and price
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                    text = booking.startTime?.replace("T", " ")?.take(16) ?: "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                    text = "₹${String.format("%.2f", booking.priceEstimate ?: 0.0)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
