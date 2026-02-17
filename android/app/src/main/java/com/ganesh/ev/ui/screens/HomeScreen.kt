package com.ganesh.ev.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.data.model.Station
import com.ganesh.ev.data.model.StationWithScore
import com.ganesh.ev.ui.components.StationCard
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.StationUiState
import com.ganesh.ev.ui.viewmodel.StationViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.Locale

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
        var currentLocation by remember { mutableStateOf<LatLng?>(null) }
        var hasLocationPermission by remember { mutableStateOf(false) }

        val defaultLocation = LatLng(19.0760, 72.8777)
        val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
        }

        // FusedLocationProviderClient
        val fusedLocationClient = remember {
                LocationServices.getFusedLocationProviderClient(context)
        }

        // Permission launcher
        val permissionLauncher =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                        hasLocationPermission =
                                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ==
                                                true
                }

        // Check and request permissions
        LaunchedEffect(Unit) {
                val fineLocationGranted =
                        ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                val coarseLocationGranted =
                        ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                hasLocationPermission = fineLocationGranted || coarseLocationGranted

                if (!hasLocationPermission) {
                        permissionLauncher.launch(
                                arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                        )
                }
        }

        // Get current location
        fun fetchCurrentLocation() {
                if (hasLocationPermission) {
                        try {
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                        location?.let {
                                                val latLng = LatLng(it.latitude, it.longitude)
                                                currentLocation = latLng
                                                cameraPositionState.position =
                                                        CameraPosition.fromLatLngZoom(latLng, 15f)
                                                // Load nearby stations with scoring
                                                viewModel.loadNearbyStations(
                                                        it.latitude,
                                                        it.longitude
                                                )
                                        }
                                }
                        } catch (e: SecurityException) {
                                // Permission not granted
                        }
                } else {
                        // Fallback if no permission
                        viewModel.loadStations()
                }
        }

        // Fetch location when permission is granted
        LaunchedEffect(hasLocationPermission) {
                if (hasLocationPermission) {
                        fetchCurrentLocation()
                }
        }

        // Removed simple loadStations call, relying on fetchCurrentLocation or fallback
        // LaunchedEffect(Unit) { viewModel.loadStations() }

        LaunchedEffect(uiState) {
                if (uiState is StationUiState.StationsLoaded) {
                        val stations = (uiState as StationUiState.StationsLoaded).stations
                        if (stations.isNotEmpty() && selectedStation == null) {
                                selectedStation = stations.first()
                                val location =
                                        LatLng(
                                                stations.first().latitude,
                                                stations.first().longitude
                                        )
                                cameraPositionState.position =
                                        CameraPosition.fromLatLngZoom(location, 14f)
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
                                                icon =
                                                        if (showList) Icons.Default.Place
                                                        else Icons.Default.List,
                                                contentDescription =
                                                        if (showList) "Show Map" else "Show List"
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        ClayIconButton(
                                                onClick = {
                                                        if (currentLocation != null) {
                                                                cameraPositionState.position =
                                                                        CameraPosition
                                                                                .fromLatLngZoom(
                                                                                        currentLocation!!,
                                                                                        15f
                                                                                )
                                                        } else {
                                                                fetchCurrentLocation()
                                                        }
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
                                                ClayCard(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .errorContainer
                                                ) {
                                                        Text(
                                                                text = state.message,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .error,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium
                                                        )
                                                }
                                                Spacer(modifier = Modifier.height(16.dp))
                                                ClayButton(onClick = { viewModel.loadStations() }) {
                                                        Text("Retry")
                                                }
                                        }
                                }
                        }
                        is StationUiState.StationsLoaded -> {
                                // Legacy fallback support
                                val stations =
                                        state.stations.map {
                                                StationWithScore(
                                                        it,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0.0,
                                                        0,
                                                        0
                                                ) // Dummy score
                                        }
                                StationContent(
                                        stations = stations,
                                        showList = showList,
                                        cameraPositionState = cameraPositionState,
                                        hasLocationPermission = hasLocationPermission,
                                        onStationClick = onStationClick,
                                        paddingValues = paddingValues
                                )
                        }
                        is StationUiState.NearbyStationsLoaded -> {
                                StationContent(
                                        stations = state.stations,
                                        showList = showList,
                                        cameraPositionState = cameraPositionState,
                                        hasLocationPermission = hasLocationPermission,
                                        onStationClick = onStationClick,
                                        paddingValues = paddingValues
                                )
                        }
                        else -> {}
                }
        }
}

@Composable
fun StationContent(
        stations: List<StationWithScore>,
        showList: Boolean,
        cameraPositionState: CameraPositionState,
        hasLocationPermission: Boolean,
        onStationClick: (Long) -> Unit,
        paddingValues: PaddingValues
) {
        var selectedStation by remember { mutableStateOf<StationWithScore?>(null) }

        // Auto-select first (highest ranked)
        LaunchedEffect(stations) {
                if (stations.isNotEmpty() && selectedStation == null) {
                        selectedStation = stations.first()
                        cameraPositionState.position =
                                CameraPosition.fromLatLngZoom(
                                        LatLng(
                                                stations.first().station.latitude,
                                                stations.first().station.longitude
                                        ),
                                        14f
                                )
                }
        }

        if (showList) {
                // List View
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        StationListView(stations = stations, onStationClick = onStationClick)
                }
        } else {
                // Map View with Pager
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                properties =
                                        MapProperties(isMyLocationEnabled = hasLocationPermission),
                                uiSettings =
                                        MapUiSettings(
                                                zoomControlsEnabled = false,
                                                myLocationButtonEnabled = false
                                        )
                        ) {
                                stations.forEachIndexed { index, item ->
                                        val station = item.station
                                        Marker(
                                                state =
                                                        MarkerState(
                                                                position =
                                                                        LatLng(
                                                                                station.latitude,
                                                                                station.longitude
                                                                        )
                                                        ),
                                                title = station.name,
                                                snippet =
                                                        if (index == 0)
                                                                "Recommended (Score: ${String.format("%.2f", item.score)})"
                                                        else
                                                                "Score: ${String.format("%.2f", item.score)}",
                                                icon =
                                                        if (index == 0)
                                                                BitmapDescriptorFactory
                                                                        .defaultMarker(
                                                                                BitmapDescriptorFactory
                                                                                        .HUE_AZURE
                                                                        )
                                                        else
                                                                BitmapDescriptorFactory
                                                                        .defaultMarker(),
                                                onClick = {
                                                        selectedStation = item
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
                                        onStationSelected = { item ->
                                                selectedStation = item
                                                cameraPositionState.position =
                                                        CameraPosition.fromLatLngZoom(
                                                                LatLng(
                                                                        item.station.latitude,
                                                                        item.station.longitude
                                                                ),
                                                                15f
                                                        )
                                        },
                                        onStationClick = onStationClick,
                                        modifier =
                                                Modifier.align(Alignment.BottomCenter)
                                                        .padding(bottom = 16.dp)
                                )
                        }
                }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationPagerOverlay(
        stations: List<StationWithScore>,
        selectedStation: StationWithScore?,
        onStationSelected: (StationWithScore) -> Unit,
        onStationClick: (Long) -> Unit,
        modifier: Modifier = Modifier
) {
        val pagerState =
                rememberPagerState(
                        initialPage =
                                stations
                                        .indexOfFirst {
                                                it.station.id == selectedStation?.station?.id
                                        }
                                        .coerceAtLeast(0),
                        pageCount = { stations.size }
                )

        LaunchedEffect(pagerState.currentPage) {
                if (stations.isNotEmpty() && pagerState.currentPage < stations.size) {
                        onStationSelected(stations[pagerState.currentPage])
                }
        }

        LaunchedEffect(selectedStation) {
                val index = stations.indexOfFirst { it.station.id == selectedStation?.station?.id }
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
                                val actualIndex =
                                        if (stations.size > 5) {
                                                (pagerState.currentPage - 2 + index).coerceIn(
                                                        0,
                                                        stations.size - 1
                                                )
                                        } else index
                                Box(
                                        modifier =
                                                Modifier.size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                                if (actualIndex ==
                                                                                pagerState
                                                                                        .currentPage
                                                                )
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                                else
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface.copy(
                                                                                alpha = 0.3f
                                                                        )
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
                        StationCard(
                                stationWithScore = stations[page],
                                onClick = { onStationClick(stations[page].station.id) }
                        )
                }
        }
}

@Composable
fun StationListView(stations: List<StationWithScore>, onStationClick: (Long) -> Unit) {
        androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                items(stations) { item ->
                        ClayStationCard(item = item, onClick = { onStationClick(item.station.id) })
                }
        }
}

@Composable
fun ClayStationCard(item: StationWithScore, onClick: () -> Unit) {
        val station = item.station
        ClayClickableCard(onClick = onClick, containerColor = MaterialTheme.colorScheme.surface) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                ) {
                        Box(
                                modifier =
                                        Modifier.size(48.dp)
                                                .claySurface(
                                                        cornerRadius = 16.dp,
                                                        shadowElevation = 3.dp
                                                )
                                                .background(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        shape =
                                                                androidx.compose.foundation.shape
                                                                        .RoundedCornerShape(16.dp)
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Text(
                                        String.format(Locale.US, "%.1f", item.score),
                                        style = MaterialTheme.typography.labelLarge
                                )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = station.name,
                                        style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                        text =
                                                "${String.format(Locale.US, "%.1f", item.distance)} km • ${item.availableSlots} slots free",
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
