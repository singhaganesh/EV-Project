package com.ganesh.ev.ui.screens

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
import androidx.compose.ui.unit.dp
import com.ganesh.ev.data.model.User
import com.ganesh.ev.ui.theme.*

@Composable
fun ProfileScreen(user: User?, onLogout: () -> Unit) {
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
                                                                            .surfaceVariant.copy(
                                                                            alpha = 0.3f
                                                                    )
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
                                    .claySurface(cornerRadius = 50.dp, shadowElevation = 8.dp)
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

            Text(text = user?.name ?: "User", style = MaterialTheme.typography.headlineMedium)

            Text(
                    text = user?.email ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (user?.role != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ClayChip(text = user.role, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Details Card
            ClayCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Account Details", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mobile", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(user?.mobileNumber ?: "-")
                }

                ClayDivider()

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Member Since", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("Last Updated", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
