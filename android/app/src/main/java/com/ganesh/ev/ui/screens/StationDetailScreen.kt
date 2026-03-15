package com.ganesh.ev.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
        onBookSlot: (Long, Double?) -> Unit,
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
                        onBookStation = { onBookSlot(stationId, state.station.truckPricePerKwh) },
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
        onBookStation: () -> Unit,
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
            0 -> ChargerTabContent(slots = slots, onBookStation = onBookStation)
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
private fun ChargerTabContent(slots: List<ChargerSlot>, onBookStation: () -> Unit) {
    var filterAvailable by remember { mutableStateOf(false) }
    var filterAC by remember { mutableStateOf(false) }
    var filterDC by remember { mutableStateOf(false) }

    val filteredSlots =
            slots.filter { slot ->
                val passAvailable = !filterAvailable || slot.status == SlotStatus.AVAILABLE
                val passType =
                        when {
                            filterAC && filterDC -> true
                            filterAC -> slot.slotType == SlotType.AC
                            filterDC -> slot.slotType == SlotType.DC
                            else -> true
                        }
                passAvailable && passType
            }

    // Group connectors by dispensary (physical machine)
    val groupedSlots = filteredSlots.groupBy { it.dispensary }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
                contentPadding =
                        PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = 80.dp // space for sticky button
                        ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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

            // Grouped by Dispensary (Machine Level)
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
                groupedSlots.forEach { (dispensary, slotsForMachine) ->
                    item {
                        DispensaryCard(
                                dispensary = dispensary,
                                slots = slotsForMachine,
                                station = slotsForMachine.firstOrNull()?.station
                        )
                    }
                }
            }
        }

        // Sticky "Book a Slot" button at bottom (always visible)
        ClayButton(
                onClick = onBookStation,
                modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                    Icons.Default.ElectricBolt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "⚡ Book a Slot", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
//  Dispensary Card (Machine Level) + Child Connectors
// ═══════════════════════════════════════════════════════════════
@Composable
private fun DispensaryCard(dispensary: Dispensary?, slots: List<ChargerSlot>, station: Station?) {
    val machineName = dispensary?.name ?: "Main Charger Unit"
    val isAc = slots.any { it.slotType == SlotType.AC }
    val isDc = slots.any { it.slotType == SlotType.DC }
    val typeLabel = if (isDc) "DC" else if (isAc) "AC" else "Unknown"
    val powerRating = dispensary?.totalPowerKw ?: slots.sumOf { it.powerRating }
    val priceText = if (dispensary?.acceptsTrucks == true) {
        station?.truckPricePerKwh?.let { "₹ $it/kWh" } ?: station?.pricePerKwh?.let { "₹ $it/kWh" } ?: ""
    } else {
        station?.pricePerKwh?.let { "₹ $it/kWh" } ?: ""
    }

    ClayCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), 
            cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Name
            Text(
                    text = machineName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-header Row: Type | Power | Price
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = "$typeLabel | ${powerRating.toInt()}kW",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text = "Last used recently",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            if (priceText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = priceText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Analytics Section
            Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF8E1).copy(alpha = 0.6f)
            ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                                text = "Charger Analytics",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                    text = "100+ charging sessions done so far",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List of guns/connectors
            slots.forEachIndexed { index, slot ->
                ConnectorRow(slot = slot, index = index + 1)
                if (index < slots.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Nested Connector Row (Child of Dispensary)
// ═══════════════════════════════════════════════════════════════
@Composable
private fun ConnectorRow(slot: ChargerSlot, index: Int) {
    Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            // Connector icon based on type (simulated visual distinction)
            Column(
                    modifier = Modifier.width(60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                        imageVector = Icons.Default.EvStation,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = formatConnectorType(slot.connectorType),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = "Connector $index",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Availability badge (design matching the screenshot green outline)
                    Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Transparent,
                            border = BorderStroke(
                                    1.dp,
                                    if (slot.status == SlotStatus.AVAILABLE) Color(0xFF4CAF50)
                                    else Color(0xFF9E9E9E)
                            )
                    ) {
                        Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (slot.status == SlotStatus.AVAILABLE) {
                                Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = Color(0xFF4CAF50)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                    text = if (slot.status == SlotStatus.AVAILABLE) "Available" else slot.status.name.lowercase().replaceFirstChar { it.uppercase() },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (slot.status == SlotStatus.AVAILABLE) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                            )
                        }
                    }

                    // Power Rating Badge (gray outline)
                    Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                    Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = Color(0xFF757575)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                    text = "Upto ${slot.powerRating.toInt()}kW",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF757575)
                            )
                        }
                    }
                }
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
        ConnectorType.TYPE_2 -> "Type 2"
    }
}
