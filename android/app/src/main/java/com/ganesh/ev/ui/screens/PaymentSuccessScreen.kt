package com.ganesh.ev.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.ev.data.model.ChargingSession
import com.ganesh.ev.ui.theme.ClayCard
import com.ganesh.ev.ui.theme.ClayDivider
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun PaymentSuccessScreen(
    session: ChargingSession,
    onGoHome: () -> Unit
) {
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

            Spacer(modifier = Modifier.height(40.dp))

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
