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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.miniproject.repository.Donation
import com.example.miniproject.repository.DonationRepository
import com.example.miniproject.repository.Payments
import com.google.firebase.auth.FirebaseAuth

// ==========================================
// BANK TRANSFER PAGE
// ==========================================

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
    var expanded by remember { mutableStateOf(false) } // For dropdown menu visibility

    // List of sample banks for the dropdown
    val bankList = listOf("Maybank", "CIMB Bank", "Public Bank", "RHB Bank", "Hong Leong Bank")

    // Logic to check if the form is valid (all fields are filled)
    val isFormValid = selectedBank.isNotEmpty() && bankId.isNotEmpty() && password.isNotEmpty()

    var isLoading by remember { mutableStateOf(false) } // Add loading state
    val repository = remember { DonationRepository() }
    val auth = FirebaseAuth.getInstance()

    // --- UI ---
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
                .background(Color(0xFFF2F2F2)) // Light gray background
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Main Input Form Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // A. Bank Selection Dropdown
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
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            bankList.forEach { bank ->
                                DropdownMenuItem(
                                    text = { Text(bank) },
                                    onClick = {
                                        selectedBank = bank
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // B. Bank ID Text Field
                    Text("Bank ID", fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = bankId,
                        onValueChange = { bankId = it },
                        label = { Text("Label") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    // C. Password Text Field
                    Text("Password", fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Label") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Payment Amount Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Payment : RM ${String.format("%.2f", paymentAmount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Pay Now Button
            // Enabled only when isFormValid is true.
            // Color changes based on enabled state.
            Button(
                onClick = {
                    isLoading = true
                    val userId = auth.currentUser?.uid ?: "Anonymous"

                    val newDonation = Donation(
                        project_id = projectId,
                        user_id = userId,
                        amount = paymentAmount,
                        paymentMethod = Payments.OnlineBanking,
                        isAnonymous = false
                    )

                    repository.createDonation(
                        donation = newDonation,
                        onSuccess = {
                            isLoading = false
                            navController.navigate("paymentSuccess/$paymentAmount/OnlineBanking") {
                                popUpTo("projectDetail/$projectId") { inclusive = false } // Clear backstack
                            }
                        },
                        onError = {
                            isLoading = false
                            // Handle error (e.g., show Toast)
                        }
                    )
                },
                enabled = isFormValid && !isLoading, // Disable when loading
                // ... (styling)
            ) {
                Text(if(isLoading) "Processing..." else "Pay Now", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Preview to see the UI
@Preview(showBackground = true)
@Composable
fun OnlinePagePreview() {
    val nav = rememberNavController()
    OnlinePage(navController = nav, paymentAmount = 50.25, projectId = "")
}