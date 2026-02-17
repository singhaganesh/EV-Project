package com.ganesh.ev.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.data.model.*
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.StationUiState
import com.ganesh.ev.ui.viewmodel.StationViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationDetailScreen(
        stationId: Long,
        onBackClick: () -> Unit,
        onBookSlot: (Long) -> Unit,
        viewModel: StationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(stationId) { viewModel.loadStationDetail(stationId) }

    Scaffold(
            topBar = {
                ClayTopBar(
                        title = "Station Details",
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
            is StationUiState.Loading -> {
                Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                ) { ClayProgressIndicator() }
            }
            is StationUiState.Error -> {
                Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                ) {
                    Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                    ) {
                        ClayCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ClayButton(onClick = { viewModel.loadStationDetail(stationId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            is StationUiState.StationDetailLoaded -> {
                ClayStationDetailContent(
                        station = state.station,
                        slots = state.slots,
                        powerData = state.powerData,
                        onSlotClick = { slotId -> onBookSlot(slotId) },
                        modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {}
        }
    }
}

@Composable
fun ClayStationDetailContent(
        station: Station,
        slots: List<ChargerSlot>,
        powerData: LivePowerData?,
        onSlotClick: (Long) -> Unit,
        modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Charger", "Details", "Reviews")

    Column(modifier = modifier.fillMaxSize()) {
        // ── Header Section ──
        StationHeader(station)

        // ── Tab Row ──
        TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                    text = title,
                                    fontWeight =
                                            if (selectedTab == index) FontWeight.Bold
                                            else FontWeight.Normal,
                                    color =
                                            if (selectedTab == index)
                                                    MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                )
            }
        }

        // ── Tab Content ──
        when (selectedTab) {
            0 -> ChargerTabContent(slots = slots, onSlotClick = onSlotClick)
            1 -> DetailsTabContent(station = station, powerData = powerData)
            2 -> ReviewsTabContent()
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Station Header
// ═══════════════════════════════════════════════════════════════
@Composable
private fun StationHeader(station: Station) {
    ClayCard(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            cornerRadius = 20.dp
    ) {
        Column {
            // Name + Open/Closed badge
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = station.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(isOpen = isStationCurrentlyOpen(station.operatingHours))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Address
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                        text = station.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Operating hours + Price row
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Operating hours
                if (station.operatingHours != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                                text = station.operatingHours,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Price
                if (station.pricePerKwh != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                                Icons.Default.CurrencyRupee,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                                text = "${station.pricePerKwh}/kWh",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Status Badge (Open/Closed)
// ═══════════════════════════════════════════════════════════════
@Composable
private fun StatusBadge(isOpen: Boolean) {
    val bgColor = if (isOpen) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val textColor = if (isOpen) Color(0xFF2E7D32) else Color(0xFFC62828)
    val text = if (isOpen) "Open" else "Closed"
    val icon = if (isOpen) Icons.Default.CheckCircle else Icons.Default.Cancel

    Surface(
            shape = RoundedCornerShape(20.dp),
            color = bgColor,
            modifier = Modifier.claySurface(cornerRadius = 20.dp, shadowElevation = 2.dp)
    ) {
        Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = textColor)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Dynamic Open/Closed — auto-calculated from operating hours
// ═══════════════════════════════════════════════════════════════
private fun isStationCurrentlyOpen(operatingHours: String?): Boolean {
    if (operatingHours == null) return true
    if (operatingHours.equals("24 Hours", ignoreCase = true)) return true

    val regex = Regex("""(\d{1,2})\s*(AM|PM)\s*-\s*(\d{1,2})\s*(AM|PM)""", RegexOption.IGNORE_CASE)
    val match = regex.find(operatingHours) ?: return true
    val (openHourStr, openPeriod, closeHourStr, closePeriod) = match.destructured

    val openHour = convertTo24Hour(openHourStr.toInt(), openPeriod)
    val closeHour = convertTo24Hour(closeHourStr.toInt(), closePeriod)
    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

    return if (closeHour > openHour) {
        currentHour in openHour until closeHour
    } else if (closeHour == openHour) {
        true // Same hour → treat as 24 hours
    } else {
        // Wraps around midnight (e.g. "5 AM - 12 AM" = 5:00–24:00)
        currentHour >= openHour || currentHour < closeHour
    }
}

private fun convertTo24Hour(hour: Int, period: String): Int {
    return when {
        period.equals("AM", ignoreCase = true) && hour == 12 -> 0
        period.equals("AM", ignoreCase = true) -> hour
        period.equals("PM", ignoreCase = true) && hour == 12 -> 12
        else -> hour + 12
    }
}

// ═══════════════════════════════════════════════════════════════
//  Charger Tab — Filter Chips + Connector Cards
// ═══════════════════════════════════════════════════════════════
@Composable
private fun ChargerTabContent(slots: List<ChargerSlot>, onSlotClick: (Long) -> Unit) {
    var filterAvailable by remember { mutableStateOf(false) }
    var filterAC by remember { mutableStateOf(false) }
    var filterDC by remember { mutableStateOf(false) }
    var selectedSlotId by remember { mutableStateOf<Long?>(null) }

    val filteredSlots =
            slots.filter { slot ->
                val passAvailable = !filterAvailable || slot.status == SlotStatus.AVAILABLE
                val passType =
                        when {
                            filterAC && filterDC -> true // both selected = no filter
                            filterAC -> slot.slotType == SlotType.AC
                            filterDC -> slot.slotType == SlotType.DC
                            else -> true
                        }
                passAvailable && passType
            }

    // Clear selection if selected slot is no longer in filtered list
    if (selectedSlotId != null && filteredSlots.none { it.id == selectedSlotId }) {
        selectedSlotId = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
                contentPadding =
                        PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = if (selectedSlotId != null) 80.dp else 16.dp
                        ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter chips
            item {
                Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChipItem(
                            label = "Available",
                            selected = filterAvailable,
                            onClick = { filterAvailable = !filterAvailable }
                    )
                    FilterChipItem(
                            label = "AC",
                            selected = filterAC,
                            onClick = { filterAC = !filterAC }
                    )
                    FilterChipItem(
                            label = "DC",
                            selected = filterDC,
                            onClick = { filterDC = !filterDC }
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                            text = "${filteredSlots.size} connectors",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Connector cards
            if (filteredSlots.isEmpty()) {
                item {
                    ClayCard(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                                text = "No connectors match selected filters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredSlots) { slot ->
                    val isAvailable = slot.status == SlotStatus.AVAILABLE
                    ConnectorCard(
                            slot = slot,
                            isSelected = selectedSlotId == slot.id,
                            onClick = {
                                if (isAvailable) {
                                    selectedSlotId =
                                            if (selectedSlotId == slot.id) null else slot.id
                                }
                            }
                    )
                }
            }
        }

        // Sticky "Book Slot" button at bottom
        AnimatedVisibility(
                visible = selectedSlotId != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier =
                        Modifier.align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ClayButton(
                    onClick = { selectedSlotId?.let { onSlotClick(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                        Icons.Default.ElectricBolt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Book This Slot", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label, fontSize = 13.sp) },
            shape = RoundedCornerShape(16.dp),
            colors =
                    FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
    )
}

// ═══════════════════════════════════════════════════════════════
//  Connector Card — HP Charge Style
// ═══════════════════════════════════════════════════════════════
@Composable
private fun ConnectorCard(slot: ChargerSlot, isSelected: Boolean = false, onClick: () -> Unit) {
    val isAvailable = slot.status == SlotStatus.AVAILABLE
    val borderColor =
            when {
                isSelected -> Color(0xFF2E7D32) // Green border for selected
                else -> Color.Transparent
            }

    ClayClickableCard(
            onClick = onClick,
            modifier =
                    Modifier.fillMaxWidth()
                            .then(
                                    if (isSelected)
                                            Modifier.border(
                                                    width = 2.dp,
                                                    color = borderColor,
                                                    shape = RoundedCornerShape(16.dp)
                                            )
                                    else Modifier
                            )
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Connector icon
            Box(
                    modifier =
                            Modifier.size(48.dp)
                                    .claySurface(cornerRadius = 14.dp, shadowElevation = 2.dp)
                                    .background(
                                            if (isSelected) Color(0xFFE8F5E9)
                                            else MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(14.dp)
                                    ),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector =
                                if (isSelected) Icons.Default.CheckCircle
                                else Icons.Default.EvStation,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint =
                                if (isSelected) Color(0xFF2E7D32)
                                else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Middle section: label + connector type
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = "Connector ${slot.slotNumber ?: slot.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = formatConnectorType(slot.connectorType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Badges row
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Availability badge
                    SlotStatusBadge(status = slot.status)
                    // Power badge
                    PowerBadge(powerKw = slot.powerRating, slotType = slot.slotType)
                }
            }

            // Selection indicator for available slots
            if (isAvailable) {
                Icon(
                        imageVector =
                                if (isSelected) Icons.Default.RadioButtonChecked
                                else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected" else "Tap to select",
                        modifier = Modifier.size(24.dp),
                        tint =
                                if (isSelected) Color(0xFF2E7D32)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun SlotStatusBadge(status: SlotStatus) {
    val (bgColor, textColor, label) =
            when (status) {
                SlotStatus.AVAILABLE -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Available")
                SlotStatus.CHARGING -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "Charging")
                SlotStatus.OCCUPIED -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Occupied")
                SlotStatus.BOOKED -> Triple(Color(0xFFF3E5F5), Color(0xFF6A1B9A), "Booked")
                SlotStatus.RESERVED -> Triple(Color(0xFFFFF8E1), Color(0xFFF9A825), "Reserved")
                SlotStatus.MAINTENANCE ->
                        Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Maintenance")
            }

    Surface(shape = RoundedCornerShape(12.dp), color = bgColor) {
        Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            if (status == SlotStatus.AVAILABLE) {
                Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = textColor
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                    text = label,
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PowerBadge(powerKw: Double, slotType: SlotType) {
    Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    Icons.Default.ElectricBolt,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                    text = "${slotType.name} • ${powerKw.toInt()}kW",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Details Tab — Map, IoT, Scores
// ═══════════════════════════════════════════════════════════════
@Composable
private fun DetailsTabContent(station: Station, powerData: LivePowerData?) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(station.latitude, station.longitude), 15f)
    }

    // Generate consistent scores based on station id
    val random = remember(station.id) { java.util.Random(station.id.hashCode().toLong()) }
    val trafficScore = 0.5 + (0.49 * random.nextDouble())
    val gridScore = 0.5 + (0.49 * random.nextDouble())
    val parkingScore = 0.5 + (0.49 * random.nextDouble())
    val accessScore = 0.5 + (0.49 * random.nextDouble())
    val overallScore =
            (trafficScore * 0.35) + (gridScore * 0.3) + (parkingScore * 0.2) + (accessScore * 0.15)

    LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Map
        item {
            ClayCard(cornerRadius = 28.dp) {
                GoogleMap(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(20.dp)),
                        cameraPositionState = cameraPositionState,
                        uiSettings =
                                MapUiSettings(
                                        zoomControlsEnabled = false,
                                        scrollGesturesEnabled = false,
                                        zoomGesturesEnabled = false,
                                        tiltGesturesEnabled = false,
                                        rotationGesturesEnabled = false
                                )
                ) {
                    Marker(
                            state =
                                    MarkerState(
                                            position = LatLng(station.latitude, station.longitude)
                                    ),
                            title = station.name
                    )
                }
            }
        }

        // IoT Sensor Data
        item {
            ClayCard(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                                Icons.Default.Sensors,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = "Live IoT Sensor Data",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (powerData != null) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PowerMetricItem(
                                    "Voltage",
                                    "${String.format(Locale.US, "%.1f", powerData.voltage)} V"
                            )
                            PowerMetricItem(
                                    "Current",
                                    "${String.format(Locale.US, "%.1f", powerData.current)} A"
                            )
                            PowerMetricItem(
                                    "Power",
                                    "${String.format(Locale.US, "%.1f", powerData.power)} kW"
                            )
                        }
                        if (powerData.forecastedLoad != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            ClayDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                        Icons.Default.Speed,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        "Forecasted Load: ${String.format(Locale.US, "%.1f", powerData.forecastedLoad)} kW",
                                        style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                    "Connecting to grid sensors...",
                                    style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Station Score
        item {
            ClayCard {
                Column {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Station Score", style = MaterialTheme.typography.titleLarge)
                        ClayChip(
                                text = String.format(Locale.US, "%.2f", overallScore),
                                color =
                                        if (overallScore > 0.8) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ScoreRow("Traffic Flow", trafficScore, Color(0xFF4CAF50))
                    ScoreRow("Grid Stability", gridScore, Color(0xFF2196F3))
                    ScoreRow("Parking Availability", parkingScore, Color(0xFFFFC107))
                    ScoreRow("Accessibility", accessScore, Color(0xFF9C27B0))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Reviews Tab — Placeholder
// ═══════════════════════════════════════════════════════════════
@Composable
private fun ReviewsTabContent() {
    Box(modifier = Modifier.fillMaxSize().padding(48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                    Icons.Default.RateReview,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                    text = "No reviews yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = "Be the first to review this station",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Helper Composables
// ═══════════════════════════════════════════════════════════════
@Composable
private fun PowerMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScoreRow(label: String, score: Double, color: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                    String.format(Locale.US, "%.2f", score),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
                progress = { score.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
        )
    }
}

private fun formatConnectorType(type: ConnectorType): String {
    return when (type) {
        ConnectorType.CCS2 -> "CCS-2"
        ConnectorType.CHADEMO -> "CHAdeMO"
        ConnectorType.TYPE_2 -> "Type 2"
        ConnectorType.TESLA -> "Tesla"
        ConnectorType.GB_T -> "GB/T"
    }
}
