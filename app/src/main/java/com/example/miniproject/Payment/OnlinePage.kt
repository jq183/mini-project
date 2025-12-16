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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.repository.Donation
import com.example.miniproject.repository.DonationRepository
import com.example.miniproject.repository.Payments
import com.example.miniproject.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlinePage(
    navController: NavController,
    paymentAmount: Double,
    projectId: String
) {
    // --- STATE ---
    var selectedBank by remember { mutableStateOf("") }
    var bankId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val bankList = listOf("Maybank", "CIMB Bank", "Public Bank", "RHB Bank", "Hong Leong Bank")
    val isFormValid = selectedBank.isNotEmpty() && bankId.isNotEmpty() && password.isNotEmpty()

    var isLoading by remember { mutableStateOf(false) }

    // Repositories
    val donationRepo = remember { DonationRepository() }
    val userRepo = remember { UserRepository() }
    val auth = FirebaseAuth.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bank Transfer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Select your bank", fontWeight = FontWeight.Medium)
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedBank,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Bank") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            bankList.forEach { bank ->
                                DropdownMenuItem(
                                    text = { Text(bank) },
                                    onClick = { selectedBank = bank; expanded = false }
                                )
                            }
                        }
                    }

                    Text("Bank ID", fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = bankId,
                        onValueChange = { bankId = it },
                        label = { Text("Bank ID") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    Text("Password", fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "Payment : RM ${String.format("%.2f", paymentAmount)}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    isLoading = true

                    // --- LOGIC SPLIT: IS IT A TOP UP OR A DONATION? ---
                    if (projectId == "TOPUP") {
                        // CASE A: Wallet Top Up
                        userRepo.topUpWallet(
                            amount = paymentAmount,
                            onSuccess = {
                                isLoading = false
                                // Pop back to origin (Wallet or Profile)
                                navController.popBackStack("topUpPage", inclusive = true)
                            },
                            onError = { isLoading = false }
                        )
                    } else {
                        // CASE B: Project Donation
                        val userId = auth.currentUser?.uid ?: "Anonymous"
                        val newDonation = Donation(
                            projectId = projectId,
                            userId = userId,
                            amount = paymentAmount,
                            paymentMethod = Payments.OnlineBanking,
                            isAnonymous = false,
                            status = "completed"
                        )

                        donationRepo.createDonation(
                            donation = newDonation,
                            onSuccess = {
                                isLoading = false
                                navController.navigate("paymentSuccess/$paymentAmount/OnlineBanking") {
                                    popUpTo("projectDetail/$projectId") { inclusive = false }
                                }
                            },
                            onError = { isLoading = false }
                        )
                    }
                },
                enabled = isFormValid && !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid) Color(0xFF1976D2) else Color(0xFFD3E6F5),
                    contentColor = if (isFormValid) Color.White else Color.Gray
                )
            ) {
                Text(text = if (isLoading) "Processing..." else "Pay Now", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}