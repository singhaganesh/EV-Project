package com.ganesh.ev.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.data.model.Station
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.StationUiState
import com.ganesh.ev.ui.viewmodel.StationViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
        onLogout: () -> Unit,
        onStationClick: (Long) -> Unit,
        viewModel: StationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showList by remember { mutableStateOf(false) }
    var selectedStation by remember { mutableStateOf<Station?>(null) }

    val defaultLocation = LatLng(19.0760, 72.8777)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    LaunchedEffect(Unit) { viewModel.loadStations() }

    LaunchedEffect(uiState) {
        if (uiState is StationUiState.StationsLoaded) {
            val stations = (uiState as StationUiState.StationsLoaded).stations
            if (stations.isNotEmpty() && selectedStation == null) {
                selectedStation = stations.first()
                val location = LatLng(stations.first().latitude, stations.first().longitude)
                cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 14f)
            }
        }
    }

    Scaffold(
            topBar = {
                ClayTopBar(
                        title = if (showList) "All Stations" else "EV Charging",
                        actions = {
                            ClayIconButton(
                                    onClick = { showList = !showList },
                                    icon = if (showList) Icons.Default.Place else Icons.Default.List,
                                    contentDescription = if (showList) "Show Map" else "Show List"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ClayIconButton(
                                    onClick = {
                                        cameraPositionState.position =
                                                CameraPosition.fromLatLngZoom(defaultLocation, 12f)
                                    },
                                    icon = Icons.Default.MyLocation,
                                    contentDescription = "My Location"
                            )
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
                            Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ClayButton(onClick = { viewModel.loadStations() }) { Text("Retry") }
                    }
                }
            }
            is StationUiState.StationsLoaded -> {
                val stations = state.stations

                AnimatedContent(
                        targetState = showList,
                        transitionSpec = {
                            fadeIn() + slideInHorizontally { it } togetherWith
                                    fadeOut() + slideOutHorizontally { -it }
                        },
                        label = "view_switcher"
                ) { showListView ->
                    if (showListView) {
                        // List View
                        Box(
                                modifier = Modifier
                                        .fillMaxSize()
                                        .padding(paddingValues)
                        ) {
                            StationListView(
                                    stations = stations,
                                    onStationClick = onStationClick
                            )
                        }
                    } else {
                        // Map View with Pager
                        Box(
                                modifier = Modifier
                                        .fillMaxSize()
                                        .padding(paddingValues)
                        ) {
                            // Full Screen Map
                            GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = cameraPositionState,
                                    properties = MapProperties(isMyLocationEnabled = false),
                                    uiSettings = MapUiSettings(
                                            zoomControlsEnabled = false,
                                            myLocationButtonEnabled = false
                                    )
                            ) {
                                stations.forEach { station ->
                                    Marker(
                                            state = MarkerState(
                                                    position = LatLng(station.latitude, station.longitude)
                                            ),
                                            title = station.name,
                                            snippet = station.address,
                                            onClick = {
                                                selectedStation = station
                                                true
                                            }
                                    )
                                }
                            }

                            // Station Pager at bottom
                            if (stations.isNotEmpty()) {
                                StationPagerOverlay(
                                        stations = stations,
                                        selectedStation = selectedStation,
                                        onStationSelected = { station ->
                                            selectedStation = station
                                            cameraPositionState.position =
                                                    CameraPosition.fromLatLngZoom(
                                                            LatLng(station.latitude, station.longitude),
                                                            15f
                                                    )
                                        },
                                        onStationClick = onStationClick,
                                        modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationPagerOverlay(
        stations: List<Station>,
        selectedStation: Station?,
        onStationSelected: (Station) -> Unit,
        onStationClick: (Long) -> Unit,
        modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
            initialPage = stations.indexOfFirst { it.id == selectedStation?.id }.coerceAtLeast(0),
            pageCount = { stations.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        if (stations.isNotEmpty() && pagerState.currentPage < stations.size) {
            onStationSelected(stations[pagerState.currentPage])
        }
    }

    LaunchedEffect(selectedStation) {
        val index = stations.indexOfFirst { it.id == selectedStation?.id }
        if (index >= 0 && pagerState.currentPage != index) {
            pagerState.animateScrollToPage(index)
        }
    }

    Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Page indicators
        Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
        ) {
            repeat(minOf(stations.size, 5)) { index ->
                val actualIndex = if (stations.size > 5) {
                    (pagerState.currentPage - 2 + index).coerceIn(0, stations.size - 1)
                } else index
                Box(
                        modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                        if (actualIndex == pagerState.currentPage)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                )
            }
        }

        // Pager with station cards
        HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 32.dp),
                pageSpacing = 16.dp
        ) { page ->
            val station = stations[page]
            StationInfoCard(
                    station = station,
                    onClick = { onStationClick(station.id) }
            )
        }
    }
}

@Composable
fun StationInfoCard(
        station: Station,
        onClick: () -> Unit
) {
    ClayClickableCard(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
    ) {
        Column(
                modifier = Modifier.padding(16.dp)
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = station.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                )
                ClayChip(
                        text = "Available",
                        color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                        text = station.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ClayButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Details")
            }
        }
    }
}

@Composable
fun StationListView(
        stations: List<Station>,
        onStationClick: (Long) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stations) { station ->
            ClayStationCard(
                    station = station,
                    onClick = { onStationClick(station.id) }
            )
        }
    }
}

@Composable
fun ClayStationCard(station: Station, onClick: () -> Unit) {
    ClayClickableCard(onClick = onClick, containerColor = MaterialTheme.colorScheme.surface) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
        ) {
            Box(
                    modifier = Modifier
                            .size(48.dp)
                            .claySurface(cornerRadius = 16.dp, shadowElevation = 3.dp)
                            .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            ),
                    contentAlignment = Alignment.Center
            ) { Text("⚡", style = MaterialTheme.typography.titleLarge) }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = station.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                        text = station.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
