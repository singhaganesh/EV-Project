package com.ganesh.ev.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.ev.data.model.ConnectorType
import com.ganesh.ev.ui.theme.*

/**
 * "Book Now" screen — user picks connector type + vehicle type, then taps Book Now.
 * No time picker. Backend assigns a random available connector and sets timestamps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotBookingScreen(
        stationId: Long,
        userId: Long?,
        viewModel: com.ganesh.ev.ui.viewmodel.BookingViewModel,
        onBackClick: () -> Unit,
        onBookingSuccess: () -> Unit
) {
        var selectedConnectorType by remember { mutableStateOf(ConnectorType.CCS2) }
        var selectedVehicle by remember { mutableStateOf("CAR") }
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(uiState) {
                if (uiState is com.ganesh.ev.ui.viewmodel.BookingUiState.BookingCreated) {
                        onBookingSuccess()
                }
        }

        if (uiState is com.ganesh.ev.ui.viewmodel.BookingUiState.Error) {
                AlertDialog(
                        onDismissRequest = { viewModel.resetState() },
                        title = { Text("Booking Failed", fontWeight = FontWeight.Bold) },
                        text = { Text((uiState as com.ganesh.ev.ui.viewmodel.BookingUiState.Error).message) },
                        confirmButton = {
                                TextButton(onClick = { viewModel.resetState() }) {
                                        Text("OK")
                                }
                        }
                )
        }

        if (uiState is com.ganesh.ev.ui.viewmodel.BookingUiState.PromptTruckFallback) {
                val truckPrice = viewModel.truckPrice.value
                
                AlertDialog(
                        onDismissRequest = { viewModel.resetState() },
                        title = { Text("Only Truck Slots Available", fontWeight = FontWeight.Bold) },
                        text = { Text("Unfortunately, all standard car slots are currently occupied. However, a truck slot is available. Would you like to book the truck slot instead? Please note that truck slots are priced at ₹ $truckPrice/kWh.") },
                        confirmButton = {
                                TextButton(onClick = { 
                                        userId?.let { uid ->
                                                viewModel.createBooking(
                                                        uid,
                                                        stationId,
                                                        selectedConnectorType.name,
                                                        selectedVehicle,
                                                        allowTruckSlotFallback = true
                                                )
                                        }
                                }) {
                                        Text("Book Truck Slot")
                                }
                        },
                        dismissButton = {
                                TextButton(onClick = { viewModel.resetState() }) {
                                        Text("Cancel", color = MaterialTheme.colorScheme.error)
                                }
                        }
                )
        }

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
                Column(
                        modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // ── Book Now Info Banner ──────────────────────────────────────
                        ClayCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                        Text("⚡", fontSize = 24.sp)
                                        Column {
                                                Text(
                                                        "Instant Booking",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Text(
                                                        "Book now & arrive within 20 minutes. The system will find the best available connector for you.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                )
                                        }
                                }
                        }

                        // ── Connector Type Selector ───────────────────────────────────
                        ClayCard {
                                Text(
                                        "Select Connector Type",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                                ConnectorTypeChip(
                                                        label = "CCS2",
                                                        subLabel = "DC Fast",
                                                        emoji = "🔌",
                                                        selected = selectedConnectorType == ConnectorType.CCS2,
                                                        onClick = { selectedConnectorType = ConnectorType.CCS2 },
                                                        modifier = Modifier.weight(1f)
                                                )
                                                ConnectorTypeChip(
                                                        label = "Type 2",
                                                        subLabel = "AC",
                                                        emoji = "🔋",
                                                        selected = selectedConnectorType == ConnectorType.TYPE_2,
                                                        onClick = { selectedConnectorType = ConnectorType.TYPE_2 },
                                                        modifier = Modifier.weight(1f)
                                                )
                                        }
                                }
                        }

                        // ── Vehicle Selector ──────────────────────────────────────────
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
                                                selected = selectedVehicle == "CAR",
                                                onClick = { selectedVehicle = "CAR" },
                                                modifier = Modifier.weight(1f)
                                        )
                                        VehicleTypeCard(
                                                emoji = "🚛",
                                                label = "Truck",
                                                selected = selectedVehicle == "TRUCK",
                                                onClick = { selectedVehicle = "TRUCK" },
                                                modifier = Modifier.weight(1f)
                                        )
                                }
                        }

                        // ── Booking Summary ───────────────────────────────────────────
                        ClayCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                        "Summary",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                SummaryRow("Connector", "${selectedConnectorType.name.replace("_", " ")}")
                                SummaryRow("Vehicle", if (selectedVehicle == "CAR") "🚗 Car" else "🚛 Truck")
                                SummaryRow("Grace Period", "20 minutes to arrive")
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

                        // ── Book Now Button ───────────────────────────────────────────
                        ClayButton(
                                onClick = {
                                        if (uiState !is com.ganesh.ev.ui.viewmodel.BookingUiState.Loading) {
                                                userId?.let { uid ->
                                                        viewModel.createBooking(
                                                                uid,
                                                                stationId,
                                                                selectedConnectorType.name,
                                                                selectedVehicle
                                                        )
                                                }
                                        }
                                },
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                if (uiState is com.ganesh.ev.ui.viewmodel.BookingUiState.Loading) {
                                        CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                        )
                                } else {
                                        Text("⚡ Book Now")
                                }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                }
        }
}

// ── Connector Type Chip ────────────────────────────────────────────────────────

@Composable
private fun ConnectorTypeChip(
        label: String,
        subLabel: String,
        emoji: String,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        val bgColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
                              else MaterialTheme.colorScheme.surface,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "ConnectorChip"
        )
        Surface(
                onClick = onClick,
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
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
                ) {
                        Text(emoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                                label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                                subLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
        }
}

// ── Vehicle Type Card ──────────────────────────────────────────────────────────

@Composable
private fun VehicleTypeCard(
        emoji: String,
        label: String,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        val bgColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
                              else MaterialTheme.colorScheme.surface,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "VehicleCard"
        )
        Surface(
                onClick = onClick,
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
                                color = MaterialTheme.colorScheme.onSurface
                        )
                }
        }
}

// ── Summary Row ────────────────────────────────────────────────────────────────

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
