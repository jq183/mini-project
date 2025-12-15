package com.example.miniproject.Payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TngPage(
    amount: String,
    navController: NavController
) {
    // --- STATE ---
    var qrPayload by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf(FakeTngGateway.PaymentStatus.PENDING) }
    var timeLeft by remember { mutableStateOf(300) } // 5 mins

    // 1. Load the QR Code (Simulate calling API to create order)
    LaunchedEffect(Unit) {
        qrPayload = FakeTngGateway.createTransaction()
    }

    // 2. Timer Logic
    LaunchedEffect(timeLeft, status) {
        if (timeLeft > 0 && status == FakeTngGateway.PaymentStatus.PENDING) {
            delay(1000)
            timeLeft--
        } else if (timeLeft == 0) {
            status = FakeTngGateway.PaymentStatus.EXPIRED
        }
    }

    // 3. POLLING: Check for payment status every 2 seconds
    LaunchedEffect(status) {
        while (status == FakeTngGateway.PaymentStatus.PENDING) {
            delay(2000) // Wait 2 seconds
            val newStatus = FakeTngGateway.checkPaymentStatus()
            if (newStatus == FakeTngGateway.PaymentStatus.PAID) {
                status = newStatus
                // DELAY slightly so user sees "Payment Received" text before navigating
                delay(1500)
                navController.navigate("paymentSuccess/$amount/TG") {
                    // Remove TngPage from backstack so they can't go back to QR
                    popUpTo("tngPage/$amount") { inclusive = true }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tng E-Wallet") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text = "TNG",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 16.dp),
                        color = Color(0xFF0054A6) // TNG Blueish color
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2))
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- QR CARD ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scan here to pay",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (qrPayload == null) {
                        // Loading State
                        CircularProgressIndicator(
                            modifier = Modifier.size(50.dp),
                            color = Color(0xFF0054A6)
                        )
                    } else {
                        // QR Code Box
                        FakeQrCode(
                            payload = qrPayload ?: "",
                            onSimulateScan = {
                                // This simulates the external event of a user scanning
                                FakeTngGateway.simulateUserScanningQr()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Status Text
                    Text(
                        text = when (status) {
                            FakeTngGateway.PaymentStatus.PENDING -> "QR valid for ${timeLeft}s"
                            FakeTngGateway.PaymentStatus.PAID -> "Payment Successful! Redirecting..."
                            FakeTngGateway.PaymentStatus.EXPIRED -> "QR Expired"
                            else -> "Error"
                        },
                        fontSize = 14.sp,
                        color = if (status == FakeTngGateway.PaymentStatus.PAID) Color(0xFF4CAF50) else Color.Gray,
                        fontWeight = if (status == FakeTngGateway.PaymentStatus.PAID) FontWeight.Bold else FontWeight.Normal
                    )

                    if (status == FakeTngGateway.PaymentStatus.PENDING) {
                        Text(
                            "(Tap QR to simulate scan)",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- AMOUNT CARD ---
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Payment: RM $amount",
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- CANCEL BUTTON ---
            // Only enabled if not currently processing a success
            Button(
                onClick = { navController.popBackStack() },
                enabled = status != FakeTngGateway.PaymentStatus.PAID,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray,
                    contentColor = Color.Black
                )
            ) {
                Text("Cancel Transaction")
            }
        }
    }
}

@Composable
fun FakeQrCode(
    payload: String,
    onSimulateScan: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(220.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(4.dp, Color.Black, RoundedCornerShape(8.dp))
            .clickable { onSimulateScan() }, // Clicking acts as the "Scan"
        contentAlignment = Alignment.Center
    ) {
        // Visual representation of a QR code
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Inner pattern to look like QR
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(Color.Black)
                    .padding(4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White))
                // Just a visual placeholder text
                Text(
                    text = ":: $payload ::",
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 8.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

//simulated server
object FakeTngGateway {
    // Enum to track payment status
    enum class PaymentStatus { PENDING, PAID, EXPIRED }

    // Store active transactions in memory (Simulating a database)
    private val _transactionStatus = MutableStateFlow(PaymentStatus.PENDING)
    val transactionStatus = _transactionStatus.asStateFlow()

    // 1. Call this to start a transaction
    suspend fun createTransaction(): String {
        // Simulate network delay
        delay(1000)
        // Reset status for new transaction
        _transactionStatus.value = PaymentStatus.PENDING
        // Return a fake unique QR payload
        return "TNG_PAY|${System.currentTimeMillis()}|${Random.nextInt(1000, 9999)}"
    }

    // 2. The app calls this repeatedly to check if user paid
    suspend fun checkPaymentStatus(): PaymentStatus {
        // Simulate network delay
        delay(500)
        return _transactionStatus.value
    }

    // 3. TRIGGER: Simulate the user scanning the QR code with their phone
    // In a real app, this happens on the user's phone, sending a signal to TNG servers.
    fun simulateUserScanningQr() {
        _transactionStatus.value = PaymentStatus.PAID
    }
}

@Preview(showBackground = true)
@Composable
fun TngPreview() {
    TngPage(amount = "10.00", navController = rememberNavController())
}