package com.ganesh.ev.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.data.model.ChargingSession
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.ChargingUiState
import com.ganesh.ev.ui.viewmodel.ChargingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingHistoryScreen(userId: Long, viewModel: ChargingViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) { viewModel.loadUserHistory(userId) }

    Scaffold(
            topBar = { ClayTopBar(title = "Charging History") },
            containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (val state = uiState) {
            is ChargingUiState.Loading -> {
                Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                ) { ClayProgressIndicator() }
            }
            is ChargingUiState.SessionsLoaded -> {
                if (state.sessions.isEmpty()) {
                    Box(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint =
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.5f
                                            )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                    text = "No charging history",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) { items(state.sessions) { session -> ClayHistoryCard(session = session) } }
                }
            }
            is ChargingUiState.Error -> {
                Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ClayCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ClayButton(onClick = { viewModel.loadUserHistory(userId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun ClayHistoryCard(session: ChargingSession) {
    ClayCard {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Session #${session.id}", style = MaterialTheme.typography.titleMedium)
            Text(text = "⚡", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Start", style = MaterialTheme.typography.bodyMedium)
            Text(
                    text = session.startTime?.replace("T", " ")?.take(16) ?: "-",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("End", style = MaterialTheme.typography.bodyMedium)
            Text(
                    text = session.endTime?.replace("T", " ")?.take(16) ?: "-",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ClayDivider()

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                        "Energy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text = "${session.energyConsumed ?: 0.0} kWh",
                        style = MaterialTheme.typography.titleMedium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                        "Cost",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text = "₹${String.format("%.2f", session.cost ?: 0.0)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
