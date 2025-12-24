package com.example.miniproject.Payment

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.repository.Donation
import com.example.miniproject.repository.DonationRepository
import com.example.miniproject.repository.Payments
import com.example.miniproject.repository.ProjectRepository
import com.example.miniproject.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletPage(
    navController: NavController,
    paymentAmount: Double,
    projectId: String
) {
    val userRepo = remember { UserRepository() }
    val donationRepo = remember { DonationRepository() }
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val projectRepository = ProjectRepository()

    var currentBalance by remember { mutableStateOf(0.00) }
    var isLoading by remember { mutableStateOf(false) }

    // --- REAL-TIME LISTENER ---
    // this listener fires immediately and updates 'currentBalance' on screen.
    DisposableEffect(Unit) {
        val listener = userRepo.addBalanceListener { newBalance ->
            currentBalance = newBalance
        }
        onDispose {
            listener?.remove()
        }
    }

    val isBalanceSufficient = currentBalance >= paymentAmount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account wallet", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2))
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Payment Amount Pill
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Payment : RM ${String.format("%.2f", paymentAmount)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 2. Main Status Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Current Balance:", fontSize = 16.sp, color = Color.DarkGray)

                    Text(
                        text = "RM ${String.format("%.2f", currentBalance)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!isBalanceSufficient) {
                        Text(
                            text = "Not Enough Balance",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Red
                        )
                    } else {
                        Text(text = "Payment total:", fontSize = 14.sp, color = Color.Black)
                        Text(
                            text = "RM ${String.format("%.2f", currentBalance)} - RM ${String.format("%.2f", paymentAmount)} =",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                        Text(
                            text = "RM ${String.format("%.2f", currentBalance - paymentAmount)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 3. Action Buttons
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD3E6F5)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Cancel Payment", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isBalanceSufficient) {
                Button(
                    onClick = {
                        // Pass 'true' so TopUpPage knows we are in the middle of a payment
                        navController.navigate("topUpPage/true")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD3E6F5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Top Up Now", color = Color.Black)
                }
            } else {
                Button(
                    onClick = {
                        if (isLoading) return@Button
                        isLoading = true

                        // Step 1: Deduct Money
                        if (projectId == "TOPUP") {
                            // CASE A: Wallet Top Up
                            userRepo.topUpWallet(
                                amount = paymentAmount,
                                onSuccess = {
                                    isLoading = false
                                    // Pop back to origin (Wallet or Profile)
                                    navController.navigate("profile")
                                },
                                onError = { isLoading = false }
                            )
                        } else {
                            userRepo.deductWallet(
                                amount = paymentAmount,
                                onSuccess = {
                                    // Step 2: Record Donation
                                    val userId = auth.currentUser?.uid ?: "Anonymous"
                                    val newDonation = Donation(
                                        projectId = projectId,
                                        userId = userId,
                                        amount = paymentAmount,
                                        paymentMethod = Payments.Wallet,
                                        isAnonymous = false,
                                        status = "completed"
                                    )

                                    donationRepo.createDonation(
                                        donation = newDonation,
                                        onSuccess = {
                                            isLoading = false
                                            navController.navigate("paymentSuccess/$paymentAmount/Wallet") {
                                                popUpTo("projectDetail/$projectId") { inclusive = false }
                                            }
                                        },
                                        onError = {
                                            isLoading = false
                                            Toast.makeText(context, "Payment processed but record failed.", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                onError = { error ->
                                    isLoading = false
                                    Toast.makeText(context, "Payment Failed: $error", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD3E6F5)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(if (isLoading) "Processing..." else "Pay Now", color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}