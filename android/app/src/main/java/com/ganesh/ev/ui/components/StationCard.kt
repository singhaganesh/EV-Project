package com.ganesh.ev.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ganesh.ev.data.model.StationWithScore
import com.ganesh.ev.ui.theme.*
import java.util.Locale

@Composable
fun StationCard(stationWithScore: StationWithScore, onClick: () -> Unit) {
        val station = stationWithScore.station
        val context = LocalContext.current
        val isAvailable = stationWithScore.availableSlots > 0

        ClayClickableCard(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().padding(end = 2.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                cornerRadius = 8.dp
        ) {
                Column(modifier = Modifier.padding(2.dp)) {
                        // Header: Name and Rating
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                        ) {
                                // Left Column: Name, Rating, Distance
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = station.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                modifier =
                                                        Modifier.padding(start = 2.dp, top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Rating
                                                Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = Color(0xFFFFC107), // Amber
                                                        modifier =
                                                                Modifier.size(14.dp)
                                                                        .padding(start = 2.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                        text =
                                                                String.format(
                                                                        Locale.US,
                                                                        "%.1f",
                                                                        stationWithScore.rating
                                                                ),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                )

                                                Spacer(modifier = Modifier.width(8.dp))

                                                // Distance
                                                Icon(
                                                        imageVector = Icons.Default.LocationOn,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                        text =
                                                                "${String.format(Locale.US, "%.1f", stationWithScore.distance)} km",
                                                        style = MaterialTheme.typography.bodyMedium
                                                )
                                        }
                                }

                                // Right Column: Availability, Last Used
                                Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier =
                                                Modifier.padding(top = 2.dp) // Align with Name text
                                ) {
                                        if (isAvailable) {
                                                Surface(
                                                        color = Color(0xFFE8F5E9), // Light Green 50
                                                        shape = RoundedCornerShape(12.dp),
                                                        border =
                                                                BorderStroke(
                                                                        1.dp,
                                                                        Color(0xFFC8E6C9)
                                                                )
                                                ) {
                                                        Row(
                                                                modifier =
                                                                        Modifier.padding(
                                                                                horizontal = 8.dp,
                                                                                vertical = 4.dp
                                                                        ),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Text(
                                                                        text = "Available",
                                                                        color =
                                                                                Color(
                                                                                        0xFF2E7D32
                                                                                ), // Green 800
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelMedium,
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(4.dp)
                                                                )
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Default
                                                                                        .CheckCircle,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFF2E7D32),
                                                                        modifier =
                                                                                Modifier.size(14.dp)
                                                                )
                                                        }
                                                }
                                        } else {
                                                Surface(
                                                        color = Color(0xFFFFEBEE), // Light Red 50
                                                        shape = RoundedCornerShape(12.dp),
                                                        border =
                                                                BorderStroke(
                                                                        1.dp,
                                                                        Color(0xFFFFCDD2)
                                                                )
                                                ) {
                                                        Row(
                                                                modifier =
                                                                        Modifier.padding(
                                                                                horizontal = 8.dp,
                                                                                vertical = 4.dp
                                                                        ),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Text(
                                                                        text = "Busy",
                                                                        color =
                                                                                Color(
                                                                                        0xFFC62828
                                                                                ), // Red 800
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelMedium,
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        }
                                                }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                                text = "Last used ${stationWithScore.lastActive}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Connector Types
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                                Icon(
                                        imageVector = Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                        text =
                                                stationWithScore.connectorTypes.joinToString(", ")
                                                        .ifEmpty { "Standard" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Action: Navigate (Text + Icon)
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clickable {
                                                        val gmmIntentUri =
                                                                Uri.parse(
                                                                        "google.navigation:q=${station.latitude},${station.longitude}"
                                                                )
                                                        val mapIntent =
                                                                Intent(
                                                                        Intent.ACTION_VIEW,
                                                                        gmmIntentUri
                                                                )
                                                        mapIntent.setPackage(
                                                                "com.google.android.apps.maps"
                                                        )
                                                        context.startActivity(mapIntent)
                                                }
                                                .padding(
                                                        vertical = 4.dp
                                                ), // Reduced touch target padding
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center // Center the action
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = "Navigate",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text = "Navigate to Station",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                )
                        }
                }
        }
}
