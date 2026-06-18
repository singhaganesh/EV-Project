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
        val chargingNotifs by
                userPreferencesRepository.chargingNotificationsEnabled.collectAsState(initial = true)
        val reminderNotifs by
                userPreferencesRepository.reminderNotificationsEnabled.collectAsState(initial = true)
        val paymentNotifs by
                userPreferencesRepository.paymentNotificationsEnabled.collectAsState(initial = true)
        val themeMode by
                userPreferencesRepository.themeMode.collectAsState(initial = "SYSTEM")

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
                                SettingsSwitchRow(
                                        title = "Notifications",
                                        subtitle = "Master switch for all push alerts",
                                        checked = notificationsEnabled,
                                        enabled = true,
                                        onCheckedChange = { enabled ->
                                                scope.launch {
                                                        userPreferencesRepository
                                                                .setNotificationsEnabled(enabled)
                                                }
                                        }
                                )

                                ClayDivider()

                                SettingsSwitchRow(
                                        title = "Charging",
                                        subtitle = "Charging complete & stopped alerts",
                                        checked = chargingNotifs,
                                        enabled = notificationsEnabled,
                                        onCheckedChange = { enabled ->
                                                scope.launch {
                                                        userPreferencesRepository
                                                                .setChargingNotificationsEnabled(enabled)
                                                }
                                        }
                                )

                                SettingsSwitchRow(
                                        title = "Reminders",
                                        subtitle = "Reservation expiry reminders",
                                        checked = reminderNotifs,
                                        enabled = notificationsEnabled,
                                        onCheckedChange = { enabled ->
                                                scope.launch {
                                                        userPreferencesRepository
                                                                .setReminderNotificationsEnabled(enabled)
                                                }
                                        }
                                )

                                SettingsSwitchRow(
                                        title = "Payments",
                                        subtitle = "Payment confirmations",
                                        checked = paymentNotifs,
                                        enabled = notificationsEnabled,
                                        onCheckedChange = { enabled ->
                                                scope.launch {
                                                        userPreferencesRepository
                                                                .setPaymentNotificationsEnabled(enabled)
                                                }
                                        }
                                )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Appearance ──
                        ClayCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        listOf(
                                                "SYSTEM" to "System",
                                                "LIGHT" to "Light",
                                                "DARK" to "Dark"
                                        ).forEach { (value, label) ->
                                                FilterChip(
                                                        selected = themeMode == value,
                                                        onClick = {
                                                                scope.launch {
                                                                        userPreferencesRepository
                                                                                .setThemeMode(value)
                                                                }
                                                        },
                                                        label = { Text(label) }
                                                )
                                        }
                                }
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

@Composable
private fun SettingsSwitchRow(
        title: String,
        subtitle: String,
        checked: Boolean,
        enabled: Boolean,
        onCheckedChange: (Boolean) -> Unit
) {
        val contentAlpha = if (enabled) 1f else 0.4f
        Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                        )
                        Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                        )
                }
                Switch(
                        checked = checked && enabled,
                        enabled = enabled,
                        onCheckedChange = onCheckedChange
                )
        }
}
