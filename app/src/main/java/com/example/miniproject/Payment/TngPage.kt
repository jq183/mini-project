package com.example.miniproject.Payment

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.R
import com.example.miniproject.repository.Donation
import com.example.miniproject.repository.DonationRepository
import com.example.miniproject.repository.Payments
import com.example.miniproject.repository.ProjectRepository
import com.example.miniproject.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TngPage(
    amount: String,
    projectId: String,
    navController: NavController
) {
    // --- STATE ---
    var qrPayload by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf(FakeTngGateway.PaymentStatus.PENDING) }
    var timeLeft by remember { mutableStateOf(300) }

    // Repositories
    val donationRepo = remember { DonationRepository() }
    val userRepo = remember { UserRepository() } // Needed for Top Up
    val auth = FirebaseAuth.getInstance()
    val projectRepository = ProjectRepository()


    var isSaving by remember { mutableStateOf(false) }

    // 1. Load the QR Code
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

    // 3. POLLING & SAVING LOGIC
    LaunchedEffect(status) {
        while (status == FakeTngGateway.PaymentStatus.PENDING) {
            delay(2000)
            val newStatus = FakeTngGateway.checkPaymentStatus()

            if (newStatus == FakeTngGateway.PaymentStatus.PAID && !isSaving) {
                status = newStatus
                isSaving = true

                val finalAmount = amount.toDoubleOrNull() ?: 0.0

                val userId = auth.currentUser?.uid ?: "Anonymous"
                val newDonation = Donation(
                        projectId = projectId,
                        userId = userId,
                        amount = finalAmount,
                        paymentMethod = Payments.TnG,
                        isAnonymous = false,
                        status = "completed"
                )

                donationRepo.createDonation(
                    donation = newDonation,
                    onSuccess = {
                        projectRepository.updateProjectDonation(
                            projectId = projectId,
                            donationAmount = finalAmount,
                            onSuccess = {
                                navController.navigate("paymentSuccess/$amount/TnG") {
                                    popUpTo("projectDetail/$projectId") { inclusive = false }
                                }
                            },
                            onError = { isSaving =false }
                        )
                    },
                    onError = { isSaving = false }
                )
            }
        }
    }

    // ... UI REMAINS THE SAME (Scaffold, Card, FakeQrCode, etc.) ...
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
                    Text("TNG", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 16.dp), color = Color(0xFF0054A6))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F2)).padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Scan here to pay", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(20.dp))
                    if (qrPayload == null) {
                        CircularProgressIndicator(modifier = Modifier.size(50.dp), color = Color(0xFF0054A6))
                    } else {
                        FakeQrCode() { FakeTngGateway.simulateUserScanningQr() }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = when (status) {
                            FakeTngGateway.PaymentStatus.PENDING -> "QR valid for ${timeLeft}s"
                            FakeTngGateway.PaymentStatus.PAID -> "Processing..."
                            FakeTngGateway.PaymentStatus.EXPIRED -> "QR Expired"
                        },
                        fontSize = 14.sp,
                        color = if (status == FakeTngGateway.PaymentStatus.PAID) Color(0xFF4CAF50) else Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
                Text(text = "Payment: RM $amount", modifier = Modifier.padding(16.dp).fillMaxWidth(), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { navController.popBackStack() },
                enabled = status != FakeTngGateway.PaymentStatus.PAID,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)
            ) {
                Text("Cancel Transaction")
            }
        }
    }
}

// ... FakeQrCode and FakeTngGateway remain identical ...
@Composable
fun FakeQrCode(onSimulateScan: () -> Unit) {
    Box(
        modifier = Modifier
            .size(220.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(4.dp, Color.Black, RoundedCornerShape(8.dp))
            .clickable { onSimulateScan() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.qr),
                contentDescription = "QR Code",
                modifier = Modifier
                    .size(140.dp)
                    .background(Color.White)
            )
        }
    }
}


object FakeTngGateway {
    enum class PaymentStatus { PENDING, PAID, EXPIRED }
    private val _transactionStatus = MutableStateFlow(PaymentStatus.PENDING)
    suspend fun createTransaction(): String { delay(1000); _transactionStatus.value = PaymentStatus.PENDING; return "TNG|${System.currentTimeMillis()}" }
    suspend fun checkPaymentStatus(): PaymentStatus { delay(500); return _transactionStatus.value }
    fun simulateUserScanningQr() { _transactionStatus.value = PaymentStatus.PAID }
}