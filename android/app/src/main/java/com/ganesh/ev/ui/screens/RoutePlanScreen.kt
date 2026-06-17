package com.ganesh.ev.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.data.model.Station
import com.ganesh.ev.ui.theme.ClayCard
import com.ganesh.ev.ui.theme.ClayClickableCard
import com.ganesh.ev.ui.theme.ClayTopBar
import com.ganesh.ev.ui.viewmodel.RoutePlanningViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Trip / route planning (E2): type a destination, see the driving route and the
 * stations within ~5 km of it.
 */
@SuppressLint("MissingPermission")
@Composable
fun RoutePlanScreen(
        onStationClick: (Long) -> Unit,
        viewModel: RoutePlanningViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var destination by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf<LatLng?>(null) }

    val hasLocation = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(hasLocation) {
        if (hasLocation) {
            try {
                fusedClient.lastLocation.addOnSuccessListener { loc ->
                    loc?.let { origin = LatLng(it.latitude, it.longitude) }
                }
            } catch (_: SecurityException) {
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.5937, 78.9629), 5f) // India
    }

    // Fit the camera to the route once it loads.
    LaunchedEffect(state) {
        val s = state
        if (s is RoutePlanningViewModel.State.Loaded && s.routePoints.isNotEmpty()) {
            val bounds = LatLngBounds.builder().apply {
                s.routePoints.forEach { include(it) }
            }.build()
            try {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
            topBar = { ClayTopBar(title = "Plan a Trip") },
            containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Destination search
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Where to?") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                        onClick = {
                            val o = origin
                            if (o == null) {
                                android.widget.Toast.makeText(
                                        context,
                                        "Waiting for your location…",
                                        android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                viewModel.planRoute(o.latitude, o.longitude, destination.trim())
                            }
                        },
                        enabled = destination.isNotBlank()
                ) { Text("Plan") }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = hasLocation)
                ) {
                    val s = state
                    if (s is RoutePlanningViewModel.State.Loaded) {
                        if (s.routePoints.isNotEmpty()) {
                            Polyline(points = s.routePoints, color = androidx.compose.ui.graphics.Color(0xFF00BCD4), width = 12f)
                        }
                        s.stations.forEach { station ->
                            Marker(
                                    state = MarkerState(position = LatLng(station.latitude, station.longitude)),
                                    title = station.name,
                                    onClick = { onStationClick(station.id); true }
                            )
                        }
                    }
                }

                if (state is RoutePlanningViewModel.State.Loading) {
                    LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    )
                }
            }

            // Results / hints below the map
            when (val s = state) {
                is RoutePlanningViewModel.State.Loaded -> {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                        Text(
                                text = buildString {
                                    append("${s.stations.size} stop(s) along your route")
                                    if (s.distance != null && s.duration != null) {
                                        append(" · ${s.distance}, ${s.duration}")
                                    }
                                },
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(s.stations) { station ->
                                RouteStationRow(station = station, onClick = { onStationClick(station.id) })
                            }
                        }
                    }
                }
                is RoutePlanningViewModel.State.Error -> {
                    Text(
                            text = s.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                    )
                }
                else -> {
                    Text(
                            text = "Enter a destination to see chargers along the way.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteStationRow(station: Station, onClick: () -> Unit) {
    ClayClickableCard(onClick = onClick, containerColor = MaterialTheme.colorScheme.surface) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = station.name, style = MaterialTheme.typography.titleSmall)
                Text(
                        text = station.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
