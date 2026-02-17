package com.ganesh.ev.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.data.model.ChargerSlot
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.SlotBookingUiState
import com.ganesh.ev.ui.viewmodel.SlotBookingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotBookingScreen(
        slotId: Long,
        onBackClick: () -> Unit,
        onConfirmBooking: (Long, String, String) -> Unit,
        viewModel: SlotBookingViewModel = viewModel()
) {
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(slotId) { viewModel.loadSlot(slotId) }

        Scaffold(
                topBar = {
                        ClayTopBar(
                                title = "Book Slot",
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
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        when (val state = uiState) {
                                is SlotBookingUiState.Loading -> {
                                        Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                        ) { ClayProgressIndicator() }
                                }
                                is SlotBookingUiState.Error -> {
                                        Column(
                                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                                Text(
                                                        text = state.message,
                                                        color = MaterialTheme.colorScheme.error
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                ClayButton(
                                                        onClick = { viewModel.loadSlot(slotId) }
                                                ) { Text("Retry") }
                                        }
                                }
                                is SlotBookingUiState.Success -> {
                                        SlotBookingContent(
                                                slot = state.slot,
                                                onConfirmBooking = onConfirmBooking
                                        )
                                }
                                else -> {}
                        }
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotBookingContent(slot: ChargerSlot, onConfirmBooking: (Long, String, String) -> Unit) {
        var selectedDate by remember { mutableStateOf(Date()) }
        var selectedHour by remember { mutableStateOf(9) }
        var selectedDuration by remember { mutableStateOf(1) }

        val datePickerState =
                rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        var showDatePicker by remember { mutableStateOf(false) }

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val calendar =
                Calendar.getInstance().apply {
                        time = selectedDate
                        set(Calendar.HOUR_OF_DAY, selectedHour)
                        set(Calendar.MINUTE, 0)
                }

        val startTime = timeFormat.format(calendar.time)

        calendar.add(Calendar.HOUR_OF_DAY, selectedDuration)
        val endTime = timeFormat.format(calendar.time)

        val totalHours = selectedDuration
        val pricePerKwh = slot.station?.pricePerKwh ?: 15.0
        val pricePerHour = slot.powerRating * pricePerKwh
        val estimatedPrice = totalHours * pricePerHour

        Column(
                modifier =
                        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // Slot details
                ClayCard {
                        slot.station?.let { station ->
                                Text(
                                        text = station.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                                text = "Slot ${slot.slotNumber ?: "#${slot.id}"}",
                                style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Horizontal detailsrow
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                                text = "Type",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                                text = "${slot.slotType}",
                                                style = MaterialTheme.typography.titleMedium
                                        )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                                text = "Connector",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                                text = "${slot.connectorType}",
                                                style = MaterialTheme.typography.titleMedium
                                        )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                                text = "Power",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                                text = "${slot.powerRating} kW",
                                                style = MaterialTheme.typography.titleMedium
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                                text =
                                        "Rate: ₹${String.format(Locale.US, "%.2f", pricePerKwh)}/kWh",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.End)
                        )
                }

                // Date picker
                ClayClickableCard(onClick = { showDatePicker = true }) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text("Date", style = MaterialTheme.typography.titleMedium)
                                ClayChip(
                                        text = dateFormat.format(selectedDate),
                                        color = MaterialTheme.colorScheme.primary
                                )
                        }
                }

                // Time selection
                ClayCard {
                        Text(text = "Select Time", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Start Hour", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                                value = selectedHour.toFloat(),
                                onValueChange = { selectedHour = it.toInt() },
                                valueRange = 0f..23f,
                                steps = 22,
                                colors =
                                        SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                        )
                        Text(
                                text = "Start Time: $startTime",
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                                "Duration: $selectedDuration hour(s)",
                                style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                                value = selectedDuration.toFloat(),
                                onValueChange = { selectedDuration = it.toInt() },
                                valueRange = 1f..8f,
                                steps = 6,
                                colors =
                                        SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                        )
                }

                // Booking summary
                ClayCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Text(text = "Booking Summary", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text("Date")
                                Text(dateFormat.format(selectedDate))
                        }
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text("Time")
                                Text("$startTime - $endTime")
                        }
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text("Duration")
                                Text("$selectedDuration hour(s)")
                        }
                        ClayDivider()
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text(
                                        text = "Estimated Price",
                                        style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                        text =
                                                "₹${String.format(Locale.US, "%.2f", estimatedPrice)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                )
                        }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ClayButton(
                        onClick = {
                                val bookingStartTime =
                                        SimpleDateFormat(
                                                        "yyyy-MM-dd'T'HH:mm:ss",
                                                        Locale.getDefault()
                                                )
                                                .format(
                                                        calendar.time.apply {
                                                                calendar.time = selectedDate
                                                                calendar.set(
                                                                        Calendar.HOUR_OF_DAY,
                                                                        selectedHour
                                                                )
                                                                calendar.set(Calendar.MINUTE, 0)
                                                        }
                                                )

                                val bookingEndTime =
                                        SimpleDateFormat(
                                                        "yyyy-MM-dd'T'HH:mm:ss",
                                                        Locale.getDefault()
                                                )
                                                .format(
                                                        calendar.time.apply {
                                                                calendar.time = selectedDate
                                                                calendar.set(
                                                                        Calendar.HOUR_OF_DAY,
                                                                        selectedHour
                                                                )
                                                                calendar.set(Calendar.MINUTE, 0)
                                                                calendar.add(
                                                                        Calendar.HOUR_OF_DAY,
                                                                        selectedDuration
                                                                )
                                                        }
                                                )

                                onConfirmBooking(slot.id, bookingStartTime, bookingEndTime)
                        },
                        modifier = Modifier.fillMaxWidth()
                ) { Text("Confirm Booking") }
        }

        if (showDatePicker) {
                DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                datePickerState.selectedDateMillis?.let {
                                                        selectedDate = Date(it)
                                                }
                                                showDatePicker = false
                                        }
                                ) { Text("OK") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                ) { DatePicker(state = datePickerState) }
        }
}
