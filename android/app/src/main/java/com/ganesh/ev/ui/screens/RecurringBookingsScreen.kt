package com.ganesh.ev.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganesh.ev.data.model.BookingTemplate
import com.ganesh.ev.data.model.BookingTemplateRequest
import com.ganesh.ev.data.model.Station
import com.ganesh.ev.data.model.Vehicle
import com.ganesh.ev.ui.theme.ClayButton
import com.ganesh.ev.ui.theme.ClayCard
import com.ganesh.ev.ui.theme.ClayTopBar
import com.ganesh.ev.ui.viewmodel.RecurringBookingsViewModel

private val DAYS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
private val CONNECTORS = listOf("CCS2" to "CCS2", "TYPE_2" to "Type 2")
private val VEHICLE_TYPES = listOf("CAR" to "Car", "TRUCK" to "Truck")

/**
 * Recurring bookings (G2). First-time users (no templates) see the "Set up"
 * registration form; afterwards they see the list and can add more.
 */
@Composable
fun RecurringBookingsScreen(
        onBack: () -> Unit,
        viewModel: RecurringBookingsViewModel = hiltViewModel()
) {
    val templates by viewModel.templates.collectAsState()
    val stations by viewModel.stations.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current

    var showForm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadAll() }
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            if (it == "Recurring booking saved") showForm = false
            viewModel.clearMessage()
        }
    }
    // First-time: no templates → show the registration form straight away.
    LaunchedEffect(loading, templates) {
        if (!loading && templates.isEmpty()) showForm = true
    }

    Scaffold(
            topBar = {
                ClayTopBar(
                        title = "Recurring Bookings",
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            // Existing templates
            templates.forEach { template ->
                TemplateCard(
                        template = template,
                        stationName = viewModel.stationName(template.stationId),
                        onToggle = { active -> viewModel.setActive(template, active) },
                        onDelete = { viewModel.delete(template.id) }
                )
            }

            if (showForm) {
                RegistrationForm(
                        firstTime = templates.isEmpty(),
                        stations = stations,
                        vehicles = vehicles,
                        onSubmit = { viewModel.create(it) },
                        onCancel = if (templates.isEmpty()) null else { { showForm = false } }
                )
            } else {
                ClayButton(onClick = { showForm = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add recurring booking")
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
        template: BookingTemplate,
        stationName: String,
        onToggle: (Boolean) -> Unit,
        onDelete: () -> Unit
) {
    ClayCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stationName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                        text = "${template.timeOfDay.take(5)} · ${template.daysOfWeek}",
                        style = MaterialTheme.typography.bodyMedium
                )
                Text(
                        text = "${connectorLabel(template.connectorType)} · ${vehicleTypeLabel(template.vehicleType)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = template.active, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationForm(
        firstTime: Boolean,
        stations: List<Station>,
        vehicles: List<Vehicle>,
        onSubmit: (BookingTemplateRequest) -> Unit,
        onCancel: (() -> Unit)?
) {
    val context = LocalContext.current
    var selectedStation by remember { mutableStateOf<Station?>(null) }
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var connector by remember { mutableStateOf("CCS2") }
    var vehicleType by remember { mutableStateOf("CAR") }
    var hour by remember { mutableIntStateOf(9) }
    var minute by remember { mutableIntStateOf(0) }
    val selectedDays = remember { mutableStateListOf<String>() }

    ClayCard(modifier = Modifier.fillMaxWidth()) {
        Text(
                text = if (firstTime) "Set up Recurring Bookings" else "New recurring booking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        if (vehicles.isEmpty()) {
            Text(
                    "Add a vehicle first (Profile › My Vehicles) to set up recurring bookings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
            )
            return@ClayCard
        }
        if (stations.isEmpty()) {
            Text(
                    "No stations available right now. Please try again later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
            )
            return@ClayCard
        }

        // Vehicle picker
        DropdownField(
                label = "Vehicle",
                selectedText = selectedVehicle?.let { "${it.make ?: ""} ${it.model ?: ""}".trim() } ?: "Select vehicle",
                options = vehicles,
                optionLabel = { "${it.make ?: ""} ${it.model ?: ""}".trim() },
                onSelected = {
                    selectedVehicle = it
                    it.connectorType?.let { c -> connector = c } // pre-fill connector from vehicle
                }
        )
        Spacer(Modifier.height(8.dp))

        // Station picker
        DropdownField(
                label = "Station",
                selectedText = selectedStation?.name ?: "Select station",
                options = stations,
                optionLabel = { it.name },
                onSelected = { selectedStation = it }
        )
        Spacer(Modifier.height(12.dp))

        Text("Connector", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CONNECTORS.forEach { (value, label) ->
                FilterChip(selected = connector == value, onClick = { connector = value }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(8.dp))

        Text("Vehicle type", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VEHICLE_TYPES.forEach { (value, label) ->
                FilterChip(selected = vehicleType == value, onClick = { vehicleType = value }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(12.dp))

        // Time
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Time", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = {
                TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, false).show()
            }) { Text("%02d:%02d".format(hour, minute)) }
        }
        Spacer(Modifier.height(12.dp))

        Text("Repeat on", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            DAYS.forEach { day ->
                FilterChip(
                        selected = selectedDays.contains(day),
                        onClick = {
                            if (selectedDays.contains(day)) selectedDays.remove(day) else selectedDays.add(day)
                        },
                        label = { Text(day.take(1)) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        val valid = selectedStation != null && selectedVehicle != null && selectedDays.isNotEmpty()
        ClayButton(
                onClick = {
                    onSubmit(
                            BookingTemplateRequest(
                                    stationId = selectedStation!!.id,
                                    vehicleId = selectedVehicle!!.id,
                                    connectorType = connector,
                                    vehicleType = vehicleType,
                                    timeOfDay = "%02d:%02d".format(hour, minute),
                                    daysOfWeek = DAYS.filter { selectedDays.contains(it) }.joinToString(","),
                                    active = true
                            )
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }

        onCancel?.let {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = it, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownField(
        label: String,
        selectedText: String,
        options: List<T>,
        optionLabel: (T) -> String,
        onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
                value = selectedText,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}

private fun connectorLabel(value: String): String =
        CONNECTORS.firstOrNull { it.first == value }?.second ?: value

private fun vehicleTypeLabel(value: String): String =
        VEHICLE_TYPES.firstOrNull { it.first == value }?.second ?: value
