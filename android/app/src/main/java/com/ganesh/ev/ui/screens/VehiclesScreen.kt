package com.ganesh.ev.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganesh.ev.data.model.Vehicle
import com.ganesh.ev.ui.theme.ClayCard
import com.ganesh.ev.ui.theme.ClayTopBar
import com.ganesh.ev.ui.viewmodel.VehicleViewModel

private val CONNECTORS = listOf("CCS2" to "CCS2", "TYPE_2" to "Type 2")

/** Vehicle garage (C1): list, add and remove the user's vehicles. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesScreen(
        onBack: () -> Unit,
        viewModel: VehicleViewModel = hiltViewModel()
) {
    val vehicles by viewModel.vehicles.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadVehicles() }
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
            topBar = {
                ClayTopBar(
                        title = "My Vehicles",
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAdd = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add vehicle")
                }
            },
            containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (vehicles.isEmpty()) {
            Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No vehicles yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                            "Add your EV to speed up booking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vehicles) { vehicle ->
                    VehicleCard(vehicle = vehicle, onDelete = { viewModel.deleteVehicle(vehicle.id) })
                }
            }
        }
    }

    if (showAdd) {
        AddVehicleDialog(
                onDismiss = { showAdd = false },
                onAdd = { make, model, battery, connector ->
                    viewModel.addVehicle(make, model, battery, connector)
                    showAdd = false
                }
        )
    }
}

@Composable
private fun VehicleCard(vehicle: Vehicle, onDelete: () -> Unit) {
    ClayCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = "${vehicle.make ?: ""} ${vehicle.model ?: ""}".trim(),
                        style = MaterialTheme.typography.titleMedium
                )
                val details = buildList {
                    vehicle.batteryKwh?.let { add("${it} kWh") }
                    vehicle.connectorType?.let { add(connectorLabel(it)) }
                }.joinToString(" · ")
                if (details.isNotEmpty()) {
                    Text(
                            text = details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVehicleDialog(
        onDismiss: () -> Unit,
        onAdd: (make: String, model: String, batteryKwh: Double?, connector: String?) -> Unit
) {
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var battery by remember { mutableStateOf("") }
    var connector by remember { mutableStateOf<String?>(null) }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add vehicle") },
            text = {
                Column {
                    OutlinedTextField(
                            value = make,
                            onValueChange = { make = it },
                            label = { Text("Make (e.g. Tata)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Model (e.g. Nexon EV)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                            value = battery,
                            onValueChange = { battery = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Battery (kWh)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Connector", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CONNECTORS.forEach { (value, label) ->
                            FilterChip(
                                    selected = connector == value,
                                    onClick = { connector = if (connector == value) null else value },
                                    label = { Text(label) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                        onClick = { onAdd(make, model, battery.toDoubleOrNull(), connector) },
                        enabled = make.isNotBlank() && model.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun connectorLabel(value: String): String =
        CONNECTORS.firstOrNull { it.first == value }?.second ?: value
