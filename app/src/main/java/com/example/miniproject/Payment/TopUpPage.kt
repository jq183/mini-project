package com.example.miniproject.Payment

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.repository.UserRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopUpPage(
    navController: NavController?,
    isFromPaymentFlow: Boolean = false
) {
    // Initialize Repository
    val userRepo = remember { UserRepository() }
    val context = LocalContext.current

    // State Variables
    var amount by remember { mutableStateOf("10") }
    var currentBalance by remember { mutableStateOf(0.00) }
    var isLoading by remember { mutableStateOf(false) }

    // REAL-TIME LISTENER: Updates 'currentBalance' whenever Firestore changes
    DisposableEffect(Unit) {
        val listener = userRepo.addBalanceListener { newBalance ->
            currentBalance = newBalance
        }
        // Cleanup listener when leaving the screen
        onDispose { listener?.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Top Up", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2))
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // --- Input Card ---
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text("Input Top Up Amount", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFDFF0FF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("RM", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = amount,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() || it == '.' }) {
                                        amount = newValue
                                    }
                                },
                                textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFDFF0FF),
                                    unfocusedContainerColor = Color(0xFFDFF0FF),
                                    disabledContainerColor = Color(0xFFDFF0FF),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Current wallet balance: RM ${String.format("%.2f", currentBalance)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Select Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("50", "100", "200").forEach { valAmount ->
                            OutlinedButton(
                                onClick = { amount = valAmount },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = valAmount)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Select Payment Method", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            // --- Logic to perform the Top Up ---
            val performTopUp = {
                val topUpValue = amount.toDoubleOrNull() ?: 0.00
                if (topUpValue > 0) {
                    isLoading = true
                    userRepo.topUpWallet(
                        amount = topUpValue,
                        onSuccess = {
                            isLoading = false
                            Toast.makeText(context, "Top up successful!", Toast.LENGTH_SHORT).show()
                            // Go back to previous screen (WalletPage or Profile)
                            navController?.popBackStack()
                        },
                        onError = { errorMsg ->
                            isLoading = false
                            Toast.makeText(context, "Top up failed: $errorMsg", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show()
                }
            }

            // --- Payment Buttons or Loading Spinner ---
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                PaymentMethodButton(
                    text = "TouchNGo E-Wallet",
                    backgroundColor = Color(0xFFE3F2FD),
                    onClick = { performTopUp() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                PaymentMethodButton(
                    text = "Online Banking",
                    backgroundColor = Color(0xFFE3F2FD),
                    onClick = { performTopUp() }
                )
            }
        }
    }
}

@Composable
fun PaymentMethodButton(text: String, backgroundColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}