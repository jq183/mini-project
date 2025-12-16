package com.example.miniproject.UserScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportPage(
    navController: NavController,
    projectId: String,      // Added
    projectTitle: String,
    projectImageUrl: String? = null
) {
    // State for the donation amount input
    var amount by remember { mutableStateOf("10") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Support This Project", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- 1. Donation Input Card ---
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Blue Header Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFD3E6F5)) // Light Blue
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Select Your Amount",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Support the project with any amount. No rewards, just support.",
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                    }

                    // White Input Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Amount Input Field
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { newValue ->
                                // Only allow numeric input
                                if (newValue.all { it.isDigit() }) {
                                    amount = newValue
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Gray,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Donate Button
                        Button(
                            onClick = {
                                val finalAmount = amount.ifEmpty { "0" }
                                // Navigate to Payment Method Page, passing amount AND projectId
                                navController.navigate("paymentOption/$finalAmount/$projectId")
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA8C8E6)), // Muted Blue from image
                            modifier = Modifier.height(50.dp)
                        ) {
                            val displayAmount = if(amount.isEmpty()) "0" else amount
                            Text(
                                text = "Donate RM$displayAmount",
                                color = Color.Black,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Footer Note
                    Text(
                        text = "* This is a simple donation. No reward tiers included.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. Project Preview Card ---
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Image Container with "Report" badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        // Placeholder for Project Image (In real app, use AsyncImage/Coil)
                        // Using a Box with Icon to mimic the placeholder in previous screens,
                        // or an Image if you have the resource.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.White
                            )
                        }

                        // Report Button Badge
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            color = Color.White,
                            shape = RoundedCornerShape(4.dp),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "Report",
                                color = Color.Red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Project Title
                    Text(
                        text = projectTitle, // Dynamically passed title
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun SupportPagePreview() {
    SupportPage(
        navController = rememberNavController(),
        projectTitle = "Smart City Initiative 2025",
        projectId = "",
        projectImageUrl = null
    )
}