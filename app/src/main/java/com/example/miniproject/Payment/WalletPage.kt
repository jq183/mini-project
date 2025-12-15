package com.example.miniproject.Payment

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

// ==========================================
// WALLET PAYMENT PAGE
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletPaymentPage(
    navController: NavController,
    paymentAmount: Double = 10.00 // <--- CHANGED: Now accepts value here, defaults to 10.00
) {
    // Logic for Balance (remains unchanged)
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val newBalanceLiveData = savedStateHandle?.getLiveData<Double>("new_balance")
        ?: androidx.lifecycle.MutableLiveData(0.00)

    val currentBalance by newBalanceLiveData.observeAsState(initial = 0.00)

    val isBalanceSufficient = currentBalance >= paymentAmount

    // UI (Exact same as your provided code)
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
                .background(Color(0xFFF2F2F2)) // Light gray background
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

            // 2. Main Status Card (Changes based on True/False flag)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp) // Fixed height to match image look
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Balance:",
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "RM ${String.format("%.2f", currentBalance)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!isBalanceSufficient) {
                        // SCENARIO: Not Enough Balance
                        Text(
                            text = "Not Enough Balance",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Red
                        )
                    } else {
                        // SCENARIO: Sufficient Balance
                        Text(
                            text = "Payment total:",
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "RM ${String.format("%.2f", currentBalance)} - RM${String.format("%.2f", paymentAmount)} =",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                        Text(
                            text = "RM ${String.format("%.2f", currentBalance - paymentAmount)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 3. Action Buttons

            // Cancel Button (Always returns to previous page)
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
                // Button for Top Up (Navigates to TopUpPage)
                Button(
                    onClick = {
                        // Navigate to TopUp, passing 'true' to indicate we came from Payment
                        navController.navigate("topUpPage/true")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD3E6F5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Top Up Now", color = Color.Black)
                }
            } else {
                // Button for Pay Now (Functionless for now)
                Button(
                    onClick = { /* Functionless */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD3E6F5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Pay Now", color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// Preview to see the UI
@Preview(showBackground = true)
@Composable
fun WalletPaymentPreview() {
    val nav = rememberNavController()
    // You can now pass the amount here in the preview or navigation
    WalletPaymentPage(navController = nav, paymentAmount = 2.00)
}