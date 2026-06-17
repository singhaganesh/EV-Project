package com.ganesh.ev.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganesh.ev.data.model.Station
import com.ganesh.ev.ui.theme.ClayClickableCard
import com.ganesh.ev.ui.theme.ClayTopBar
import com.ganesh.ev.ui.viewmodel.FavoritesViewModel

/**
 * Saved stations (F3): the user's favorites plus their recent stations (derived
 * from recent bookings). Tapping a row opens the station detail.
 */
@Composable
fun SavedStationsScreen(
        userId: Long?,
        onStationClick: (Long) -> Unit,
        onBack: () -> Unit,
        viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsState()
    val recents by viewModel.recents.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadFavorites()
        userId?.let { viewModel.loadRecents(it) }
    }

    Scaffold(
            topBar = {
                ClayTopBar(
                        title = "Saved",
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader("Favorites") }
            if (favorites.isEmpty()) {
                item { EmptyHint("No saved stations yet. Tap the heart on a station to save it.") }
            } else {
                items(favorites) { station ->
                    SavedStationRow(station = station, onClick = { onStationClick(station.id) })
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Recent")
            }
            if (recents.isEmpty()) {
                item { EmptyHint("No recent stations yet.") }
            } else {
                items(recents) { station ->
                    SavedStationRow(station = station, onClick = { onStationClick(station.id) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SavedStationRow(station: Station, onClick: () -> Unit) {
    ClayClickableCard(onClick = onClick, containerColor = MaterialTheme.colorScheme.surface) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = station.name, style = MaterialTheme.typography.titleMedium)
                Text(
                        text = station.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
