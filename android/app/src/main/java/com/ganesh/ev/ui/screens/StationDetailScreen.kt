package com.ganesh.ev.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.data.model.ChargerSlot
import com.ganesh.ev.data.model.LivePowerData
import com.ganesh.ev.data.model.SlotStatus
import com.ganesh.ev.data.model.Station
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
import java.util.Random

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
                val station = state.station
                val slots = state.slots
                val powerData = state.powerData

                ClayStationDetailContent(
                        station = station,
                        slots = slots,
                        powerData = powerData,
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
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(station.latitude, station.longitude), 15f)
    }

    // Generate consistent random scores for visualization
    val random = remember(station.id) { Random(station.id.hashCode().toLong()) }
    val trafficScore = 0.5 + (0.49 * random.nextDouble())
    val gridScore = 0.5 + (0.49 * random.nextDouble())
    val parkingScore = 0.5 + (0.49 * random.nextDouble())
    val accessScore = 0.5 + (0.49 * random.nextDouble())
    val overallScore =
            (trafficScore * 0.35) + (gridScore * 0.3) + (parkingScore * 0.2) + (accessScore * 0.15)

    LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ClayCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                            modifier =
                                    Modifier.size(48.dp)
                                            .claySurface(
                                                    cornerRadius = 16.dp,
                                                    shadowElevation = 3.dp
                                            )
                                            .background(
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(16.dp)
                                            ),
                            contentAlignment = Alignment.Center
                    ) {
                        Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = station.name, style = MaterialTheme.typography.headlineSmall)
                        Text(
                                text = station.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

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

        // --- Live Power & IoT Section ---
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
                        // Loading state for IoT
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

        // --- Station Scoring Model Section ---
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

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Available Slots", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.width(8.dp))
                ClayChip(
                        text = "${slots.count { it.status == SlotStatus.AVAILABLE }}",
                        color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (slots.isEmpty()) {
            item {
                ClayCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                            text = "No slots available at this station",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(slots) { slot -> ClaySlotCard(slot = slot, onClick = { onSlotClick(slot.id) }) }
        }
    }
}

@Composable
fun PowerMetricItem(label: String, value: String) {
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
fun ScoreRow(label: String, score: Double, color: Color) {
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

@Composable
fun ClaySlotCard(slot: ChargerSlot, onClick: () -> Unit) {
    val isAvailable = slot.status == SlotStatus.AVAILABLE
    val slotColor =
            when (slot.status) {
                SlotStatus.AVAILABLE -> ClaySlotAvailable
                SlotStatus.BOOKED -> ClaySlotBooked
                SlotStatus.CHARGING -> ClaySlotCharging
                SlotStatus.RESERVED -> ClaySlotReserved
                SlotStatus.MAINTENANCE -> ClaySlotMaintenance
                SlotStatus.OCCUPIED -> ClaySlotOccupied
            }
    val statusColor =
            when (slot.status) {
                SlotStatus.AVAILABLE -> MaterialTheme.colorScheme.primary
                SlotStatus.MAINTENANCE, SlotStatus.OCCUPIED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

    ClayClickableCard(onClick = onClick, containerColor = slotColor) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                        text = "Slot ${slot.slotNumber ?: "#${slot.id}"}",
                        style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = "${slot.slotType} • ${slot.connectorType}",
                        style = MaterialTheme.typography.bodyMedium
                )
                Text(
                        text = "${slot.powerRating} kW",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ClayChip(text = slot.status.name, color = statusColor)
        }
    }
}
