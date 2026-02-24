package com.ganesh.ev.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.ganesh.ev.data.model.StationPin
import com.ganesh.ev.data.model.StationWithScore
import com.ganesh.ev.ui.components.StationCard
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.StationUiState
import com.ganesh.ev.ui.viewmodel.StationViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
        onLogout: () -> Unit,
        onStationClick: (Long) -> Unit,
        viewModel: StationViewModel = viewModel()
) {
        val uiState by viewModel.uiState.collectAsState()
        val otherPins by viewModel.otherPins.collectAsState()
        val isLoadingStations by viewModel.isLoadingStations.collectAsState()
        val context = LocalContext.current

        var showList by remember { mutableStateOf(false) }
        var currentLocation by remember { mutableStateOf<LatLng?>(null) }
        var hasLocationPermission by remember { mutableStateOf<Boolean?>(null) }

        var backPressedTime by remember { mutableStateOf(0L) }
        BackHandler {
                val currentTime = System.currentTimeMillis()
                if (currentTime - backPressedTime < 2000) {
                        (context as? android.app.Activity)?.finish()
                } else {
                        backPressedTime = currentTime
                        android.widget.Toast.makeText(
                                        context,
                                        "Press back again to exit",
                                        android.widget.Toast.LENGTH_SHORT
                                )
                                .show()
                }
        }

        val defaultLocation = LatLng(19.0760, 72.8777)
        val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
        }

        val fusedLocationClient = remember {
                LocationServices.getFusedLocationProviderClient(context)
        }

        // Derive nearby stations from UI state
        val nearbyStations =
                remember(uiState) {
                        when (uiState) {
                                is StationUiState.NearbyStationsLoaded ->
                                        (uiState as StationUiState.NearbyStationsLoaded).stations
                                is StationUiState.StationsLoaded ->
                                        (uiState as StationUiState.StationsLoaded).stations.map {
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
                                                )
                                        }
                                else -> emptyList()
                        }
                }

        val permissionLauncher =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                        hasLocationPermission =
                                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ==
                                                true
                }

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

                val granted = fineLocationGranted || coarseLocationGranted

                if (granted) {
                        hasLocationPermission = true
                } else {
                        // Launch permission request. State remains null until user decides.
                        permissionLauncher.launch(
                                arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                        )
                }
        }

        fun fetchCurrentLocation() {
                if (hasLocationPermission == true) {
                        try {
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                        location?.let {
                                                val latLng = LatLng(it.latitude, it.longitude)
                                                currentLocation = latLng
                                                cameraPositionState.position =
                                                        CameraPosition.fromLatLngZoom(latLng, 15f)
                                                // Will be picked up by camera-move listener
                                        }
                                }
                        } catch (e: SecurityException) {
                                // Permission not granted
                        }
                }
        }

        // --- GATE CHECK: Block UI and API calls until permission is resolved ---
        if (hasLocationPermission == null) {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                ) { ClayProgressIndicator() }
                return // Exit composition to prevent Map rendering and data fetching
        }

        // Guaranteed non-null from here. Convert back to standard boolean to pass to children.
        val hasPermissionResolved = hasLocationPermission == true

        // Once location permission is granted, get location
        LaunchedEffect(hasPermissionResolved) {
                if (hasPermissionResolved) {
                        fetchCurrentLocation()
                }
        }

        // Debounced unified viewport+nearby fetch on camera move
        LaunchedEffect(cameraPositionState.isMoving) {
                if (!cameraPositionState.isMoving) {
                        delay(500) // Debounce 500ms after camera stops
                        val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
                        val userLoc = currentLocation
                        if (bounds != null && userLoc != null) {
                                viewModel.loadViewportWithNearby(
                                        neLat = bounds.northeast.latitude,
                                        neLng = bounds.northeast.longitude,
                                        swLat = bounds.southwest.latitude,
                                        swLng = bounds.southwest.longitude,
                                        lat = userLoc.latitude,
                                        lng = userLoc.longitude
                                )
                        } else if (bounds != null) {
                                // No user location yet — use map center as reference
                                val center = cameraPositionState.position.target
                                viewModel.loadViewportWithNearby(
                                        neLat = bounds.northeast.latitude,
                                        neLng = bounds.northeast.longitude,
                                        swLat = bounds.southwest.latitude,
                                        swLng = bounds.southwest.longitude,
                                        lat = center.latitude,
                                        lng = center.longitude
                                )
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
                // Always show the map/content — never block with a full-screen loader
                StationContent(
                        nearbyStations = nearbyStations,
                        otherPins = otherPins,
                        showList = showList,
                        isLoading = isLoadingStations,
                        cameraPositionState = cameraPositionState,
                        hasLocationPermission = hasPermissionResolved,
                        onStationClick = onStationClick,
                        onMarkerClick = { id, lat, lng -> viewModel.onMarkerClicked(id, lat, lng) },
                        paddingValues = paddingValues
                )
        }
}

@Composable
fun StationContent(
        nearbyStations: List<StationWithScore>,
        otherPins: List<StationPin>,
        showList: Boolean,
        isLoading: Boolean,
        cameraPositionState: CameraPositionState,
        hasLocationPermission: Boolean,
        onStationClick: (Long) -> Unit,
        onMarkerClick: (Long, Double, Double) -> Unit,
        paddingValues: PaddingValues
) {
        var selectedStation by remember { mutableStateOf<StationWithScore?>(null) }
        val context = LocalContext.current

        // Pre-generate custom marker bitmaps (memoized for performance)
        val selectedBitmap: Bitmap = remember {
                createCustomMarkerBitmap(
                        context,
                        true,
                        android.graphics.Color.parseColor("#2196F3")
                )
        }
        val unselectedBitmap: Bitmap = remember {
                createCustomMarkerBitmap(
                        context,
                        false,
                        android.graphics.Color.parseColor("#2196F3")
                )
        }
        val otherPinBitmap: Bitmap = remember {
                createCustomMarkerBitmap(
                        context,
                        false,
                        android.graphics.Color.parseColor("#4CAF50")
                )
        }

        // Auto-select first (highest ranked) when stations arrive
        LaunchedEffect(nearbyStations) {
                if (nearbyStations.isNotEmpty() && selectedStation == null) {
                        selectedStation = nearbyStations.first()
                        cameraPositionState.position =
                                CameraPosition.fromLatLngZoom(
                                        LatLng(
                                                nearbyStations.first().station.latitude,
                                                nearbyStations.first().station.longitude
                                        ),
                                        14f
                                )
                }
        }

        if (showList) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        if (nearbyStations.isEmpty() && isLoading) {
                                Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                ) { ClayProgressIndicator() }
                        } else {
                                StationListView(
                                        stations = nearbyStations,
                                        onStationClick = onStationClick
                                )
                        }
                }
        } else {
                // MAP VIEW — fill to bottom edge (no bottom padding, map goes under nav)
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(top = paddingValues.calculateTopPadding())
                ) {
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
                                // Maps is initialized here. Safe to call BitmapDescriptorFactory.
                                val selectedIcon =
                                        remember(selectedBitmap) {
                                                BitmapDescriptorFactory.fromBitmap(selectedBitmap)
                                        }
                                val unselectedIcon =
                                        remember(unselectedBitmap) {
                                                BitmapDescriptorFactory.fromBitmap(unselectedBitmap)
                                        }
                                val otherPinIcon =
                                        remember(otherPinBitmap) {
                                                BitmapDescriptorFactory.fromBitmap(otherPinBitmap)
                                        }
                                // Nearby stations — full-data markers (dynamic size)
                                nearbyStations.forEach { item ->
                                        val station = item.station
                                        val isSelected = selectedStation?.station?.id == station.id
                                        val markerIcon: BitmapDescriptor =
                                                if (isSelected) selectedIcon else unselectedIcon

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
                                                        if (item.availableSlots > 0) "Available"
                                                        else "Busy",
                                                icon = markerIcon,
                                                zIndex = if (isSelected) 1f else 0f,
                                                onClick = {
                                                        onMarkerClick(
                                                                station.id,
                                                                station.latitude,
                                                                station.longitude
                                                        )
                                                        true
                                                }
                                        )
                                }

                                // Other pins — small lightweight markers
                                otherPins.forEach { pin ->
                                        Marker(
                                                state =
                                                        MarkerState(
                                                                position =
                                                                        LatLng(
                                                                                pin.latitude,
                                                                                pin.longitude
                                                                        )
                                                        ),
                                                title = "Station",
                                                icon = otherPinIcon,
                                                onClick = {
                                                        onMarkerClick(
                                                                pin.id,
                                                                pin.latitude,
                                                                pin.longitude
                                                        )
                                                        true
                                                }
                                        )
                                }
                        }

                        // Loading indicator at bottom of map when fetching stations
                        if (isLoading) {
                                LinearProgressIndicator(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .align(Alignment.BottomCenter),
                                        color = MaterialTheme.colorScheme.primary
                                )
                        }

                        // Station Card pager at bottom (shows nearest 5)
                        if (nearbyStations.isNotEmpty()) {
                                StationPagerOverlay(
                                        stations = nearbyStations,
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

/** Draw a teardrop-shaped map pin as a Bitmap. Selected markers are drawn larger for emphasis. */
private fun createCustomMarkerBitmap(
        context: android.content.Context,
        isSelected: Boolean,
        color: Int
): Bitmap {
        val density = context.resources.displayMetrics.density

        // Dimensions: selected pins are ~2x the size of unselected
        val headRadiusDp = if (isSelected) 14f else 12f
        val headRadius = headRadiusDp * density
        val pinHeight = headRadius * 2.6f // total height of pin (head + tail)
        val strokeW = if (isSelected) 2.5f * density else 1.5f * density

        val width = (headRadius * 2 + strokeW * 2 + 4 * density).toInt()
        val height = (pinHeight + strokeW + 4 * density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = width / 2f
        val headCy = headRadius + strokeW + 1 * density
        val tipY = headCy + pinHeight - headRadius

        // Build the teardrop path: circle head + pointed bottom
        val path = android.graphics.Path()
        // The angle where the circle transitions to the tip lines
        val halfAngle = 35f // degrees from bottom-center of circle to tangent point
        val startAngle = 90f + halfAngle
        val sweepAngle = 360f - 2f * halfAngle

        val oval =
                android.graphics.RectF(
                        cx - headRadius,
                        headCy - headRadius,
                        cx + headRadius,
                        headCy + headRadius
                )

        // Start from the tip, draw line to circle tangent, arc around, line back to tip
        val tangentAngleLeft = Math.toRadians((90.0 + halfAngle))
        val tangentAngleRight = Math.toRadians((90.0 - halfAngle))
        val leftX = cx + headRadius * Math.cos(tangentAngleLeft).toFloat()
        val leftY = headCy + headRadius * Math.sin(tangentAngleLeft).toFloat()
        val rightX = cx + headRadius * Math.cos(tangentAngleRight).toFloat()
        val rightY = headCy + headRadius * Math.sin(tangentAngleRight).toFloat()

        path.moveTo(cx, tipY) // tip of pin
        path.lineTo(leftX, leftY) // line to left tangent
        path.arcTo(oval, startAngle, sweepAngle) // arc around the head
        path.lineTo(cx, tipY) // line back to tip
        path.close()

        // Shadow
        val shadowPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.color = android.graphics.Color.argb(40, 0, 0, 0)
                        style = Paint.Style.FILL
                }
        canvas.save()
        canvas.translate(0f, 1.5f * density)
        canvas.drawPath(path, shadowPaint)
        canvas.restore()

        // Fill
        val fillPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.color = color
                        style = Paint.Style.FILL
                }
        canvas.drawPath(path, fillPaint)

        // White border
        val borderPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.color = android.graphics.Color.WHITE
                        style = Paint.Style.STROKE
                        strokeWidth = strokeW
                        strokeJoin = Paint.Join.ROUND
                }
        canvas.drawPath(path, borderPaint)

        // Inner white dot on the circle head
        val dotRadius = if (isSelected) 4f * density else 2.5f * density
        val dotPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.color = android.graphics.Color.WHITE
                        style = Paint.Style.FILL
                }
        canvas.drawCircle(cx, headCy, dotRadius, dotPaint)

        return bitmap
}
