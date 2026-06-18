package com.ganesh.ev.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.data.model.ChargingSession
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.ChargingUiState
import com.ganesh.ev.ui.viewmodel.ChargingViewModel
import com.ganesh.ev.util.ReceiptHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingHistoryScreen(
        userId: Long,
        onPayNow: (Long) -> Unit = {},
        viewModel: ChargingViewModel = viewModel()
) {
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
                    ) { items(state.sessions) { session -> ClayHistoryCard(session = session, onPayNow = onPayNow) } }
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
fun ClayHistoryCard(session: ChargingSession, onPayNow: (Long) -> Unit = {}) {
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
                        text = "${session.energyKwh ?: 0.0} kWh",
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
                        text = "₹${String.format("%.2f", session.totalCost ?: 0.0)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Completed but not paid → let the user finish the payment from here
        // (recovery path for sessions that auto-completed while the app was closed).
        if (!session.paymentStatus.equals("PAID", ignoreCase = true) &&
                session.status.equals("COMPLETED", ignoreCase = true)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ClayButton(onClick = { onPayNow(session.id) }) {
                    Text("Pay ₹${String.format("%.2f", session.totalCost ?: 0.0)}")
                }
            }
        }

        // Receipt download is offered only once the session is paid.
        if (session.paymentStatus.equals("PAID", ignoreCase = true)) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var downloading by remember { mutableStateOf(false) }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                        onClick = {
                            if (!downloading) {
                                downloading = true
                                scope.launch {
                                    val file = ReceiptHelper.download(context, session.id)
                                    downloading = false
                                    if (file != null) {
                                        ReceiptHelper.share(context, file)
                                    } else {
                                        android.widget.Toast.makeText(
                                                context,
                                                "Could not download receipt",
                                                android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                ) {
                    if (downloading) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Receipt")
                    }
                }
            }
        }
    }
}
