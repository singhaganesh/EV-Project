package com.ganesh.ev.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.data.model.BookingStatus
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.BookingUiState
import com.ganesh.ev.ui.viewmodel.BookingViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay

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
                                                modifier =
                                                        Modifier.fillMaxSize()
                                                                .padding(paddingValues),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Text(
                                                        "Booking not found",
                                                        color = MaterialTheme.colorScheme.error
                                                )
                                        }
                                } else {
                                        var showCancelDialog by remember { mutableStateOf(false) }

                                        val stationName = booking.slot?.station?.name
                                        val stationAddress = booking.slot?.station?.address
                                        val slotNumber = booking.slot?.slotNumber

                                        Column(
                                                modifier =
                                                        Modifier.fillMaxSize()
                                                                .padding(paddingValues)
                                                                .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                                // Status badge row
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                text = "Booking #${booking.id}",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .headlineSmall
                                                        )
                                                        val statusColor =
                                                                when (booking.status) {
                                                                        BookingStatus.CONFIRMED ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                        BookingStatus.ONGOING ->
                                                                                ClayInfo
                                                                        BookingStatus.COMPLETED ->
                                                                                ClaySuccess
                                                                        BookingStatus.CANCELLED ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .error
                                                                        BookingStatus.PENDING ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .secondary
                                                                        BookingStatus.EXPIRED ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .error
                                                                }
                                                        StatusChip(
                                                                text = booking.status.name,
                                                                color = statusColor
                                                        )
                                                }

                                                // Status Info Card based on booking status
                                                when (booking.status) {
                                                        BookingStatus.CONFIRMED -> {
                                                                // Show countdown timer
                                                                if (booking.expiresAt != null) {
                                                                        var timeRemainingMs by remember {
                                                                                mutableStateOf(0L)
                                                                        }
                                                                        var isExpired by remember {
                                                                                mutableStateOf(
                                                                                        false
                                                                                )
                                                                        }

                                                                        LaunchedEffect(
                                                                                booking.expiresAt
                                                                        ) {
                                                                                try {
                                                                                        val format =
                                                                                                SimpleDateFormat(
                                                                                                        "yyyy-MM-dd'T'HH:mm:ss",
                                                                                                        Locale.getDefault()
                                                                                                )
                                                                                        val parsedDate =
                                                                                                try {
                                                                                                        format.parse(
                                                                                                                booking.expiresAt
                                                                                                        )
                                                                                                } catch (
                                                                                                        e:
                                                                                                                Exception) {
                                                                                                        SimpleDateFormat(
                                                                                                                        "yyyy-MM-dd'T'HH:mm",
                                                                                                                        Locale.getDefault()
                                                                                                                )
                                                                                                                .parse(
                                                                                                                        booking.expiresAt
                                                                                                                )
                                                                                                }
                                                                                        if (parsedDate !=
                                                                                                        null
                                                                                        ) {
                                                                                                val expiresAtMillis =
                                                                                                        parsedDate
                                                                                                                .time
                                                                                                while (true) {
                                                                                                        val remaining =
                                                                                                                expiresAtMillis -
                                                                                                                        System.currentTimeMillis()
                                                                                                        timeRemainingMs =
                                                                                                                if (remaining >
                                                                                                                                0
                                                                                                                )
                                                                                                                        remaining
                                                                                                                else
                                                                                                                        0
                                                                                                        if (remaining <=
                                                                                                                        0
                                                                                                        ) {
                                                                                                                isExpired =
                                                                                                                        true
                                                                                                                break
                                                                                                        }
                                                                                                        delay(
                                                                                                                1000
                                                                                                        )
                                                                                                }
                                                                                        }
                                                                                } catch (
                                                                                        e:
                                                                                                Exception) {}
                                                                        }

                                                                        if (!isExpired &&
                                                                                        timeRemainingMs >
                                                                                                0
                                                                        ) {
                                                                                val hours =
                                                                                        timeRemainingMs /
                                                                                                3600000
                                                                                val minutes =
                                                                                        (timeRemainingMs %
                                                                                                3600000) /
                                                                                                60000
                                                                                val seconds =
                                                                                        (timeRemainingMs %
                                                                                                60000) /
                                                                                                1000

                                                                                Surface(
                                                                                        modifier =
                                                                                                Modifier.fillMaxWidth(),
                                                                                        shape =
                                                                                                RoundedCornerShape(
                                                                                                        12.dp
                                                                                                ),
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .errorContainer
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.5f
                                                                                                        )
                                                                                ) {
                                                                                        Row(
                                                                                                modifier =
                                                                                                        Modifier.padding(
                                                                                                                        16.dp
                                                                                                                )
                                                                                                                .fillMaxWidth(),
                                                                                                horizontalArrangement =
                                                                                                        Arrangement
                                                                                                                .SpaceBetween,
                                                                                                verticalAlignment =
                                                                                                        Alignment
                                                                                                                .CenterVertically
                                                                                        ) {
                                                                                                Text(
                                                                                                        text =
                                                                                                                "Time Remaining",
                                                                                                        style =
                                                                                                                MaterialTheme
                                                                                                                        .typography
                                                                                                                        .titleMedium,
                                                                                                        color =
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .onErrorContainer
                                                                                                )
                                                                                                Text(
                                                                                                        text =
                                                                                                                if (hours >
                                                                                                                                0
                                                                                                                ) {
                                                                                                                        String.format(
                                                                                                                                "%02d:%02d:%02d",
                                                                                                                                hours,
                                                                                                                                minutes,
                                                                                                                                seconds
                                                                                                                        )
                                                                                                                } else {
                                                                                                                        String.format(
                                                                                                                                "%02d:%02d",
                                                                                                                                minutes,
                                                                                                                                seconds
                                                                                                                        )
                                                                                                                },
                                                                                                        style =
                                                                                                                MaterialTheme
                                                                                                                        .typography
                                                                                                                        .headlineSmall,
                                                                                                        fontWeight =
                                                                                                                FontWeight
                                                                                                                        .Bold,
                                                                                                        color =
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .error
                                                                                                )
                                                                                        }
                                                                                }
                                                                        } else if (isExpired) {
                                                                                Surface(
                                                                                        modifier =
                                                                                                Modifier.fillMaxWidth(),
                                                                                        shape =
                                                                                                RoundedCornerShape(
                                                                                                        12.dp
                                                                                                ),
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .errorContainer
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.5f
                                                                                                        )
                                                                                ) {
                                                                                        Text(
                                                                                                text =
                                                                                                        "Booking Expired",
                                                                                                style =
                                                                                                        MaterialTheme
                                                                                                                .typography
                                                                                                                .titleMedium,
                                                                                                color =
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .error,
                                                                                                modifier =
                                                                                                        Modifier.padding(
                                                                                                                16.dp
                                                                                                        )
                                                                                        )
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                        BookingStatus.CANCELLED -> {
                                                                Surface(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        12.dp
                                                                                ),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .errorContainer
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.5f
                                                                                        )
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        "This booking has been cancelled",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyMedium,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .error,
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                16.dp
                                                                                        )
                                                                        )
                                                                }
                                                        }
                                                        BookingStatus.EXPIRED -> {
                                                                Surface(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        12.dp
                                                                                ),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .errorContainer
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.5f
                                                                                        )
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        "This booking has expired",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyMedium,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .error,
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                16.dp
                                                                                        )
                                                                        )
                                                                }
                                                        }
                                                        BookingStatus.COMPLETED -> {
                                                                Surface(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        12.dp
                                                                                ),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primaryContainer
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.5f
                                                                                        )
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        "Charging completed successfully!",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyMedium,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onPrimaryContainer,
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                16.dp
                                                                                        )
                                                                        )
                                                                }
                                                        }
                                                        BookingStatus.ONGOING -> {
                                                                Surface(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        12.dp
                                                                                ),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primaryContainer
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.5f
                                                                                        )
                                                                ) {
                                                                        Row(
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                        16.dp
                                                                                                )
                                                                                                .fillMaxWidth(),
                                                                                horizontalArrangement =
                                                                                        Arrangement
                                                                                                .SpaceBetween,
                                                                                verticalAlignment =
                                                                                        Alignment
                                                                                                .CenterVertically
                                                                        ) {
                                                                                Text(
                                                                                        text =
                                                                                                "Charging in Progress",
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .titleMedium,
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onPrimaryContainer
                                                                                )
                                                                                Text(
                                                                                        text =
                                                                                                "ACTIVE",
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .titleMedium,
                                                                                        fontWeight =
                                                                                                FontWeight
                                                                                                        .Bold,
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .primary
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                        BookingStatus.PENDING -> {
                                                                Surface(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        12.dp
                                                                                ),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .secondaryContainer
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.5f
                                                                                        )
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        "Your booking is being processed",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyMedium,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSecondaryContainer,
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                16.dp
                                                                                        )
                                                                        )
                                                                }
                                                        }
                                                }

                                                // Station details
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                        Text(
                                                                text = "Station Details",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleMedium
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        if (stationName != null) {
                                                                Text(
                                                                        text = stationName,
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyLarge
                                                                )
                                                        }
                                                        if (stationAddress != null) {
                                                                Text(
                                                                        text = stationAddress,
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyMedium,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                )
                                                        }
                                                        if (slotNumber != null) {
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        4.dp
                                                                                )
                                                                )
                                                                StatusChip(
                                                                        text = "Slot $slotNumber",
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                )
                                                        }
                                                }

                                                // Time & Pricing
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                        Text(
                                                                text = "Time & Pricing",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleMedium
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement =
                                                                        Arrangement.SpaceBetween
                                                        ) {
                                                                Text("Start")
                                                                Text(
                                                                        booking.startTime?.replace(
                                                                                "T",
                                                                                " "
                                                                        )
                                                                                ?: "-"
                                                                )
                                                        }
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement =
                                                                        Arrangement.SpaceBetween
                                                        ) {
                                                                Text("End")
                                                                Text(
                                                                        booking.endTime?.replace(
                                                                                "T",
                                                                                " "
                                                                        )
                                                                                ?: "-"
                                                                )
                                                        }
                                                        HorizontalDivider(
                                                                modifier =
                                                                        Modifier.padding(
                                                                                vertical = 8.dp
                                                                        )
                                                        )
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement =
                                                                        Arrangement.SpaceBetween
                                                        ) {
                                                                Text(
                                                                        "Total Price",
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

                                                Spacer(modifier = Modifier.weight(1f))

                                                // Actions based on status
                                                when (booking.status) {
                                                        BookingStatus.CONFIRMED -> {
                                                                ClayButton(
                                                                        onClick = {
                                                                                onStartCharging(
                                                                                        booking.id
                                                                                )
                                                                        },
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                ) { Text("Start Charging") }
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        8.dp
                                                                                )
                                                                )
                                                                ClayOutlinedButton(
                                                                        onClick = {
                                                                                showCancelDialog =
                                                                                        true
                                                                        },
                                                                        borderColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .error,
                                                                        contentColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .error,
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                ) { Text("Cancel Booking") }
                                                        }
                                                        BookingStatus.ONGOING -> {
                                                                ClayButton(
                                                                        onClick = {
                                                                                onGoToCharging(
                                                                                        booking.id
                                                                                )
                                                                        },
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                ) { Text("Go to Charging") }
                                                        }
                                                        else -> {}
                                                }
                                        }

                                        // Cancel Confirmation Dialog
                                        if (showCancelDialog) {
                                                AlertDialog(
                                                        onDismissRequest = {
                                                                showCancelDialog = false
                                                        },
                                                        title = { Text("Cancel Booking") },
                                                        text = {
                                                                Text(
                                                                        "Are you sure you want to cancel this booking?"
                                                                )
                                                        },
                                                        confirmButton = {
                                                                TextButton(
                                                                        onClick = {
                                                                                viewModel
                                                                                        .cancelBooking(
                                                                                                booking.id
                                                                                        )
                                                                                showCancelDialog =
                                                                                        false
                                                                                onBackClick()
                                                                        }
                                                                ) {
                                                                        Text(
                                                                                "Yes, Cancel",
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .error
                                                                        )
                                                                }
                                                        },
                                                        dismissButton = {
                                                                TextButton(
                                                                        onClick = {
                                                                                showCancelDialog =
                                                                                        false
                                                                        }
                                                                ) { Text("No") }
                                                        }
                                                )
                                        }
                                }
                        }
                        else -> {}
                }
        }
}
