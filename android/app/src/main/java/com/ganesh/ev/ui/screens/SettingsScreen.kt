package com.ganesh.ev.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ganesh.ev.BuildConfig
import com.ganesh.ev.data.repository.UserPreferencesRepository
import com.ganesh.ev.ui.theme.ClayCard
import com.ganesh.ev.ui.theme.ClayDivider
import com.ganesh.ev.ui.theme.ClayTopBar
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
        userPreferencesRepository: UserPreferencesRepository,
        onBack: () -> Unit
) {
        val scope = rememberCoroutineScope()
        val notificationsEnabled by
                userPreferencesRepository.notificationsEnabled.collectAsState(initial = true)

        Column(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
                ClayTopBar(
                        title = "Settings",
                        navigationIcon = {
                                IconButton(onClick = onBack) {
                                        Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back"
                                        )
                                }
                        }
                )

                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(24.dp)
                                        .verticalScroll(rememberScrollState())
                ) {
                        // ── Notifications ──
                        ClayCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                        "Notifications",
                                                        style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                        "Charging updates and reminders",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                        Switch(
                                                checked = notificationsEnabled,
                                                onCheckedChange = { enabled ->
                                                        scope.launch {
                                                                userPreferencesRepository
                                                                        .setNotificationsEnabled(
                                                                                enabled
                                                                        )
                                                        }
                                                }
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Appearance (placeholder) ──
                        ClayCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        "Follows your system theme",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── About ──
                        ClayCard(modifier = Modifier.fillMaxWidth()) {
                                Text("About", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        Text(
                                                "App",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text("Plugsy")
                                }
                                ClayDivider()
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        Text(
                                                "Version",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(BuildConfig.VERSION_NAME)
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Help & Support (placeholder) ──
                        ClayCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Help & Support", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        "Contact support@plugsy.in",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
        }
}
