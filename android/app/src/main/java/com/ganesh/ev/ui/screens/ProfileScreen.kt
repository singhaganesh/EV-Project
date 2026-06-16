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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.data.model.User
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
        user: User?,
        onLogout: () -> Unit,
        onAccountDeleted: () -> Unit = onLogout,
        viewModel: ProfileViewModel = viewModel()
) {
        val deleteState by viewModel.deleteState.collectAsState()
        var showDeleteDialog by remember { mutableStateOf(false) }
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

                        Text(
                                text = user?.name ?: "User",
                                style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                                text = user?.email ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

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
