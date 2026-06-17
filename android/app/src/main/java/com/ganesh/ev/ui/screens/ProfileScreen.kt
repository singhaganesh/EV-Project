package com.ganesh.ev.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganesh.ev.data.model.User
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
        user: User?,
        onLogout: () -> Unit,
        onAccountDeleted: () -> Unit = onLogout,
        onOpenSettings: () -> Unit = {},
        onOpenSaved: () -> Unit = {},
        onOpenVehicles: () -> Unit = {},
        onOpenRecurring: () -> Unit = {},
        onProfileUpdated: (User) -> Unit = {},
        viewModel: ProfileViewModel = hiltViewModel()
) {
        val deleteState by viewModel.deleteState.collectAsState()
        val updateState by viewModel.updateState.collectAsState()
        var showDeleteDialog by remember { mutableStateOf(false) }
        var editing by remember { mutableStateOf(false) }
        var nameField by remember(user) { mutableStateOf(user?.name ?: "") }
        var emailField by remember(user) { mutableStateOf(user?.email ?: "") }
        val context = LocalContext.current

        LaunchedEffect(deleteState) {
                when (val state = deleteState) {
                        is ProfileViewModel.DeleteState.Deleted -> {
                                showDeleteDialog = false
                                onAccountDeleted()
                        }
                        is ProfileViewModel.DeleteState.Error -> {
                                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                                viewModel.resetDeleteState()
                        }
                        else -> {}
                }
        }

        LaunchedEffect(updateState) {
                when (val state = updateState) {
                        is ProfileViewModel.UpdateState.Saved -> {
                                editing = false
                                onProfileUpdated(state.user)
                                Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                viewModel.resetUpdateState()
                        }
                        is ProfileViewModel.UpdateState.Error -> {
                                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                                viewModel.resetUpdateState()
                        }
                        else -> {}
                }
        }

        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        MaterialTheme.colorScheme
                                                                                .background,
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceVariant
                                                                                .copy(alpha = 0.3f)
                                                                )
                                                )
                                )
        ) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Spacer(modifier = Modifier.height(40.dp))

                        // Avatar
                        Box(
                                modifier =
                                        Modifier.size(100.dp)
                                                .claySurface(
                                                        cornerRadius = 50.dp,
                                                        shadowElevation = 8.dp
                                                )
                                                .background(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        shape = CircleShape
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (editing) {
                                ClayTextField(
                                        value = nameField,
                                        onValueChange = { nameField = it },
                                        label = { Text("Name") },
                                        modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ClayTextField(
                                        value = emailField,
                                        onValueChange = { emailField = it },
                                        label = { Text("Email") },
                                        modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                val saving = updateState is ProfileViewModel.UpdateState.Saving
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        ClayButton(
                                                onClick = {
                                                        user?.id?.let {
                                                                viewModel.updateProfile(
                                                                        it,
                                                                        nameField.trim(),
                                                                        emailField.trim().ifEmpty { null }
                                                                )
                                                        }
                                                },
                                                enabled = !saving && nameField.isNotBlank(),
                                                modifier = Modifier.weight(1f)
                                        ) { Text(if (saving) "Saving…" else "Save") }
                                        ClayButton(
                                                onClick = {
                                                        nameField = user?.name ?: ""
                                                        emailField = user?.email ?: ""
                                                        editing = false
                                                },
                                                enabled = !saving,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                        ) { Text("Cancel") }
                                }
                        } else {
                                Text(
                                        text = user?.name ?: "User",
                                        style = MaterialTheme.typography.headlineMedium
                                )

                                Text(
                                        text = user?.email ?: "",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                TextButton(onClick = { editing = true }) { Text("Edit profile") }
                        }

                        if (user?.role != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                StatusChip(
                                        text = user.role,
                                        color = MaterialTheme.colorScheme.primary
                                )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Details Card
                        ClayCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                        text = "Account Details",
                                        style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        Text(
                                                "Mobile",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(user?.mobileNumber ?: "-")
                                }

                                ClayDivider()

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        Text(
                                                "Member Since",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                                text = user?.createdAt?.take(10) ?: "-",
                                                style = MaterialTheme.typography.bodyMedium
                                        )
                                }

                                ClayDivider()

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        Text(
                                                "Last Updated",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                                text = user?.updatedAt?.take(10) ?: "-",
                                                style = MaterialTheme.typography.bodyMedium
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        ClayOutlinedButton(
                                onClick = onOpenSaved,
                                modifier = Modifier.fillMaxWidth()
                        ) { Text("Saved Stations") }

                        Spacer(modifier = Modifier.height(12.dp))

                        ClayOutlinedButton(
                                onClick = onOpenVehicles,
                                modifier = Modifier.fillMaxWidth()
                        ) { Text("My Vehicles") }

                        Spacer(modifier = Modifier.height(12.dp))

                        ClayOutlinedButton(
                                onClick = onOpenRecurring,
                                modifier = Modifier.fillMaxWidth()
                        ) { Text("Recurring Bookings") }

                        Spacer(modifier = Modifier.height(12.dp))

                        ClayOutlinedButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.fillMaxWidth()
                        ) { Text("Settings") }

                        Spacer(modifier = Modifier.weight(1f))

                        ClayButton(
                                onClick = onLogout,
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.fillMaxWidth()
                        ) { Text("Logout") }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth()
                        ) { Text("Delete Account", color = MaterialTheme.colorScheme.error) }

                        Spacer(modifier = Modifier.height(24.dp))
                }

                if (showDeleteDialog) {
                        val deleting = deleteState is ProfileViewModel.DeleteState.Deleting
                        AlertDialog(
                                onDismissRequest = { if (!deleting) showDeleteDialog = false },
                                title = { Text("Delete account?") },
                                text = {
                                        Text(
                                                "This permanently deletes your Plugsy account and " +
                                                        "personal data. This action cannot be undone."
                                        )
                                },
                                confirmButton = {
                                        TextButton(
                                                onClick = { viewModel.deleteAccount() },
                                                enabled = !deleting
                                        ) {
                                                Text(
                                                        if (deleting) "Deleting…" else "Delete",
                                                        color = MaterialTheme.colorScheme.error
                                                )
                                        }
                                },
                                dismissButton = {
                                        TextButton(
                                                onClick = { showDeleteDialog = false },
                                                enabled = !deleting
                                        ) { Text("Cancel") }
                                }
                        )
                }
        }
}
