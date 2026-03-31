package com.ganesh.ev.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganesh.ev.ui.theme.*
import com.ganesh.ev.ui.viewmodel.ChargingUiState
import com.ganesh.ev.ui.viewmodel.ChargingViewModel
import com.razorpay.Checkout
import org.json.JSONObject
import java.util.Locale

@Composable
fun PaymentSummaryScreen(
    sessionId: Long,
    onPaymentSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: ChargingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    // Function to launch Razorpay
    fun startRazorpay(orderId: String, amount: Double, keyId: String) {
        val checkout = Checkout()
        checkout.setKeyID(keyId)
        try {
            val options = JSONObject()
            options.put("name", "EV Charging")
            options.put("description", "Payment for Session #$sessionId")
            options.put("order_id", orderId)
            options.put("theme.color", "#00BCD4")
            options.put("currency", "INR")
            options.put("amount", Math.round(amount * 100))
            
            val prefill = JSONObject()
            prefill.put("email", "customer@example.com")
            prefill.put("contact", "9144070952")
            options.put("prefill", prefill)

            // ── ADD THIS: Pass sessionId in notes so MainActivity can extract it ──
            val notes = JSONObject()
            notes.put("session_id", sessionId.toString())
            options.put("notes", notes)

            checkout.open(context as android.app.Activity, options)
        } catch (e: Exception) {
            android.util.Log.e("Razorpay", "Error starting checkout", e)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8FEFF)),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is ChargingUiState.Loading -> {
                CircularProgressIndicator(color = Color(0xFF00BCD4))
            }
            is ChargingUiState.SessionLoaded -> {
                val session = state.session
                val isPaid = session.paymentStatus == "PAID"

                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Receipt Icon
                    Box(
                        modifier = Modifier.size(80.dp).background(Color(0xFFE0F7F9), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPaid) Icons.Default.Check else Icons.Default.Receipt,
                            contentDescription = null,
                            tint = if (isPaid) Color(0xFF22C55E) else Color(0xFF00BCD4),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (isPaid) "Payment Successful" else "Payment Summary",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A2234)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Receipt Details Card
                    ClayCard(cornerRadius = 28.dp, containerColor = Color.White) {
                        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                            SummaryRow("Session ID", "#${session.id}")
                            ClayDivider(modifier = Modifier.padding(vertical = 12.dp))
                            SummaryRow("Energy Consumed", "${String.format("%.2f", session.energyKwh ?: 0.0)} kWh")
                            SummaryRow("Charging Cost", "₹${String.format("%.2f", session.totalCost ?: 0.0)}")
                            
                            ClayDivider(modifier = Modifier.padding(vertical = 12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Amount to Pay", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(
                                    "₹${String.format("%.2f", session.totalCost ?: 0.0)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    color = Color(0xFF1A2234)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    if (isPaid) {
                        // If verified, show success state
                        ClayButton(
                            onClick = onPaymentSuccess,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Go to Home")
                        }
                    } else {
                        // Payment Actions
                        Button(
                            onClick = {
                                val orderId = session.razorpayOrderId
                                val cost = session.totalCost ?: 0.0
                                if (orderId != null) {
                                    startRazorpay(orderId, cost, "rzp_test_SXhA6XgzIubKi8")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2234))
                        ) {
                            Icon(Icons.Default.Wallet, null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Proceed to Pay", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        
                        TextButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Pay Later", color = Color.Gray)
                        }
                    }
                }
            }
            is ChargingUiState.Error -> {
                Text("Error loading bill: ${state.message}", color = Color.Red)
                Button(onClick = onBack) { Text("Go Back") }
            }
            else -> {}
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
