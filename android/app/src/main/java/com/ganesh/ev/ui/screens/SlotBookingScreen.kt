package com.ganesh.ev.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.data.model.ChargerSlot
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.SlotBookingUiState
import com.ganesh.ev.ui.viewmodel.SlotBookingViewModel
import java.text.SimpleDateFormat
import java.util.*

// Item height in the drum scroll picker
private val DRUM_ITEM_HEIGHT = 56.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotBookingScreen(
        slotId: Long,
        onBackClick: () -> Unit,
        onConfirmBooking: (Long, String, String, String) -> Unit,
        viewModel: SlotBookingViewModel = viewModel()
) {
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(slotId) { viewModel.loadSlot(slotId) }

        Scaffold(
                topBar = {
                        ClayTopBar(
                                title = "Book Charger",
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
                                                ClayButton(onClick = { viewModel.loadSlot(slotId) }) {
                                                        Text("Retry")
                                                }
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

@Composable
fun SlotBookingContent(
        slot: ChargerSlot,
        onConfirmBooking: (Long, String, String, String) -> Unit
) {
        // Lock to today's date always
        val today = remember { Calendar.getInstance() }
        val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())

        // Time picker state — defaults to the next full hour
        val nowHour = remember { Calendar.getInstance().get(Calendar.HOUR) }
        val nowMins = 0
        val nowAmPm = remember { Calendar.getInstance().get(Calendar.AM_PM) }

        var selectedHour by remember { mutableStateOf(if (nowHour == 0) 12 else nowHour) } // 1-12
        var selectedMinute by remember { mutableStateOf(nowMins) }                           // 0, 15, 30, 45
        var isAm by remember { mutableStateOf(nowAmPm == Calendar.AM) }

        var selectedVehicle by remember { mutableStateOf("CAR") }
        val isTruckAvailable = slot.dispensary?.acceptsTrucks ?: true

        val pricePerKwh =
                if (selectedVehicle == "TRUCK" && slot.station?.truckPricePerKwh != null)
                        slot.station.truckPricePerKwh
                else slot.station?.pricePerKwh ?: 15.0

        // Build display start-time string for summary
        val amPmLabel = if (isAm) "AM" else "PM"
        val minuteLabel = "%02d".format(selectedMinute)
        val startTimeDisplay = "$selectedHour:$minuteLabel $amPmLabel"

        Column(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                Spacer(modifier = Modifier.height(4.dp))

                // ── Station Info Card ──────────────────────────────────────────
                ClayCard {
                        slot.station?.let { station ->
                                Text(
                                        text = station.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                        text = station.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                                SlotInfoChip(label = "Charger", value = "${slot.slotType}")
                                SlotInfoChip(label = "Connector", value = "${slot.connectorType}")
                                SlotInfoChip(label = "Power", value = "${slot.powerRating} kW")
                        }
                }

                // ── Today Only Banner ──────────────────────────────────────────
                ClayCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                                Text("📅", fontSize = 20.sp)
                                Column {
                                        Text(
                                                "Booking for Today",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                                dateFormat.format(today.time),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                }
                        }
                }

                // ── Vehicle Selector ───────────────────────────────────────────
                ClayCard {
                        Text(
                                "Select Vehicle",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                VehicleTypeCard(
                                        emoji = "🚗",
                                        label = "Car",
                                        rate = "₹${slot.station?.pricePerKwh ?: 15}/kWh",
                                        selected = selectedVehicle == "CAR",
                                        onClick = { selectedVehicle = "CAR" },
                                        modifier = Modifier.weight(1f)
                                )
                                VehicleTypeCard(
                                        emoji = "🚛",
                                        label = "Truck",
                                        rate = "₹${slot.station?.truckPricePerKwh ?: 20}/kWh",
                                        selected = selectedVehicle == "TRUCK",
                                        enabled = isTruckAvailable,
                                        onClick = { if (isTruckAvailable) selectedVehicle = "TRUCK" },
                                        modifier = Modifier.weight(1f)
                                )
                        }
                        if (selectedVehicle == "TRUCK" && !isTruckAvailable) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                        Text("⚠️", fontSize = 16.sp)
                                        Text(
                                                "This dispensary does not accept trucks.",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                }
                        }
                }

                // ── Arrival Time — Drum Picker ─────────────────────────────────
                ClayCard {
                        Text(
                                "Select Arrival Time",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                        Text(
                                "You can start charging any time after you arrive",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // The three drum columns: Hour | Minute | AM/PM
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // Hour drum (1-12)
                                DrumColumn(
                                        items = (1..12).map { it.toString() },
                                        selectedIndex = selectedHour - 1,
                                        onSelectedIndexChange = { selectedHour = it + 1 },
                                        modifier = Modifier.width(72.dp)
                                )

                                Text(
                                        ":",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                // Minute drum (00-59)
                                val minuteOptions = (0..59).toList()
                                DrumColumn(
                                        items = minuteOptions.map { "%02d".format(it) },
                                        selectedIndex = minuteOptions.indexOf(selectedMinute).coerceAtLeast(0),
                                        onSelectedIndexChange = { selectedMinute = minuteOptions[it] },
                                        modifier = Modifier.width(72.dp)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                // AM/PM toggle
                                Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                        AmPmButton(label = "AM", selected = isAm, onClick = { isAm = true })
                                        AmPmButton(label = "PM", selected = !isAm, onClick = { isAm = false })
                                }
                        }
                }

                // ── Booking Summary ────────────────────────────────────────────
                ClayCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                                "Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SummaryRow("Vehicle", if (selectedVehicle == "CAR") "🚗 Car" else "🚛 Truck")
                        SummaryRow("Date", "Today — ${dateFormat.format(today.time)}")
                        SummaryRow("Arrive by", startTimeDisplay)
                        SummaryRow("Rate", "₹${String.format(Locale.US, "%.2f", pricePerKwh)}/kWh")
                        ClayDivider()
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text(
                                        "Price",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                        "Calculated after charging",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }

                // ── Confirm Button ─────────────────────────────────────────────
                val canBook = selectedVehicle == "CAR" || isTruckAvailable
                ClayButton(
                        onClick = {
                                val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

                                // Convert 12h to 24h
                                val hour24 = when {
                                        isAm && selectedHour == 12 -> 0
                                        !isAm && selectedHour != 12 -> selectedHour + 12
                                        else -> selectedHour
                                }
                                val startCal = Calendar.getInstance().apply {
                                        time = today.time
                                        set(Calendar.HOUR_OF_DAY, hour24)
                                        set(Calendar.MINUTE, selectedMinute)
                                        set(Calendar.SECOND, 0)
                                }
                                // endTime = startTime + 4 hours (reservation window; actual billing from session)
                                val endCal = (startCal.clone() as Calendar).apply {
                                        add(Calendar.HOUR_OF_DAY, 4)
                                }
                                onConfirmBooking(
                                        slot.id,
                                        fmt.format(startCal.time),
                                        fmt.format(endCal.time),
                                        selectedVehicle
                                )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canBook
                ) { Text("Reserve Slot") }

                Spacer(modifier = Modifier.height(24.dp))
        }
}

// ── Drum Scroll Picker ─────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrumColumn(
        items: List<String>,
        selectedIndex: Int,
        onSelectedIndexChange: (Int) -> Unit,
        modifier: Modifier = Modifier
) {
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
        val flingBehavior = rememberSnapFlingBehavior(listState)

        // Sync scroll position → selection
        LaunchedEffect(listState.firstVisibleItemIndex) {
                val newIndex = listState.firstVisibleItemIndex.coerceIn(0, items.lastIndex)
                if (newIndex != selectedIndex) {
                        onSelectedIndexChange(newIndex)
                }
        }

        // Sync selection → scroll (when changed externally)
        LaunchedEffect(selectedIndex) {
                if (listState.firstVisibleItemIndex != selectedIndex) {
                        listState.animateScrollToItem(selectedIndex)
                }
        }

        Box(
                modifier = modifier.height(DRUM_ITEM_HEIGHT * 3),
                contentAlignment = Alignment.Center
        ) {
                // Selection highlight bar
                Surface(
                        modifier = Modifier
                                .fillMaxWidth()
                                .height(DRUM_ITEM_HEIGHT),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {}

                LazyColumn(
                        state = listState,
                        flingBehavior = flingBehavior,
                        contentPadding = PaddingValues(vertical = DRUM_ITEM_HEIGHT),
                        modifier = Modifier.fillMaxSize()
                ) {
                        items(items.size) { index ->
                                val isCurrent = index == selectedIndex
                                val distanceFromCenter = (index - selectedIndex).let {
                                        if (it < 0) -it else it
                                }
                                val alpha = when (distanceFromCenter) {
                                        0 -> 1f
                                        1 -> 0.45f
                                        else -> 0.15f
                                }
                                Box(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .height(DRUM_ITEM_HEIGHT),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Text(
                                                text = items[index],
                                                fontSize = if (isCurrent) 36.sp else 22.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.alpha(alpha),
                                                textAlign = TextAlign.Center
                                        )
                                }
                        }
                }
        }
}

// ── AM/PM Toggle Button ────────────────────────────────────────────────────────

@Composable
private fun AmPmButton(label: String, selected: Boolean, onClick: () -> Unit) {
        val bg by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "AmPm"
        )
        val textColor by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "AmPmText"
        )
        Surface(
                onClick = onClick,
                shape = MaterialTheme.shapes.medium,
                color = bg,
                modifier = Modifier.width(56.dp)
        ) {
                Text(
                        label,
                        color = textColor,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center
                )
        }
}

// ── Reusable helpers ───────────────────────────────────────────────────────────

@Composable
private fun VehicleTypeCard(
        emoji: String,
        label: String,
        rate: String,
        selected: Boolean,
        enabled: Boolean = true,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        val bgColor by animateColorAsState(
                targetValue = when {
                        selected -> MaterialTheme.colorScheme.primaryContainer
                        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surface
                },
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "VehicleCard"
        )
        Surface(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier,
                shape = MaterialTheme.shapes.large,
                color = bgColor,
                border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                )
        ) {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 20.dp, horizontal = 8.dp)
                ) {
                        Text(emoji, fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                                rate,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!enabled) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        "Not Available",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                )
                        }
                }
        }
}

@Composable
private fun SlotInfoChip(label: String, value: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        value,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                )
        }
}

@Composable
private fun SummaryRow(label: String, value: String) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                )
        }
}
