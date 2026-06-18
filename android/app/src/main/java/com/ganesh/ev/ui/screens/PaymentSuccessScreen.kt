package com.ganesh.ev.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.ev.data.model.ChargingSession
import com.ganesh.ev.ui.theme.ClayCard
import com.ganesh.ev.ui.theme.ClayDivider
import com.ganesh.ev.util.ReceiptHelper
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * PaymentSuccessScreen displays the post-payment transaction details.
 * This screen is shown only after a successful Razorpay payment verification.
 */
@Composable
fun PaymentSuccessScreen(
    session: ChargingSession,
    onGoHome: () -> Unit,
    onWriteReview: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8FEFF)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success Icon
            Box(
                modifier = Modifier.size(80.dp).background(Color(0xFFDCFCE7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Payment Successful",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A2234)
            )
            
            Text(
                text = "Your transaction was completed successfully.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Transaction Details Card
            ClayCard(cornerRadius = 28.dp, containerColor = Color.White) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    SuccessSummaryRow("Transaction ID", session.razorpayOrderId?.takeLast(10) ?: "#${session.id}")
                    SuccessSummaryRow("Station", session.booking?.slot?.station?.name ?: "Unknown Station")
                    
                    ClayDivider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    SuccessSummaryRow("Energy Delivered", "${String.format("%.2f", session.energyKwh ?: 0.0)} kWh")
                    SuccessSummaryRow("Duration", calculateDuration(session.startTime, session.endTime))
                    
                    ClayDivider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Amount Paid", color = Color.Gray, fontSize = 12.sp)
                            Text("via Razorpay", color = Color.Gray, fontSize = 10.sp)
                        }
                        Text(
                            "₹${String.format("%.2f", session.totalCost ?: 0.0)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = Color(0xFF22C55E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
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
                                    "Could not download receipt. Please try again.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (downloading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Download, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Download / Share Receipt", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // Let the user rate the station for the session they just paid for.
            val reviewStationId = session.booking?.slot?.station?.id
            if (reviewStationId != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onWriteReview(reviewStationId) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Star, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Rate your experience", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onGoHome,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2234))
            ) {
                Icon(Icons.Default.Home, null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Go to Home", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

private fun calculateDuration(start: String?, end: String?): String {
    if (start == null || end == null) return "N/A"
    return try {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val startTime = LocalDateTime.parse(start, formatter)
        val endTime = LocalDateTime.parse(end, formatter)
        val duration = java.time.Duration.between(startTime, endTime)
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    } catch (e: Exception) {
        "N/A"
    }
}

@Composable
private fun SuccessSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1A2234))
    }
}
