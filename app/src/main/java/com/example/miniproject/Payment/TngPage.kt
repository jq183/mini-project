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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TngPage(
    amount: String,
    navController: NavController?
) {
    var timeLeft by remember { mutableStateOf(300) } // 5 minutes
    var qrScanned by remember { mutableStateOf(false) }

    // Countdown timer
    LaunchedEffect(timeLeft, qrScanned) {
        if (timeLeft > 0 && !qrScanned) {
            delay(1000)
            timeLeft--
        }
    }

    val canReturn = qrScanned || timeLeft == 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tng E-Wallet") },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Fake TnG logo placeholder
                    Text(
                        text = "TNG",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
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

            // QR Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Scan here to pay",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FakeQrCode(
                        payload = "TNG|DUITNOW|RM=$amount",
                        onScanned = { qrScanned = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (qrScanned)
                            "Payment received"
                        else
                            "QR valid for ${timeLeft}s",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Amount
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(Color.White),
                modifier = Modifier.fillMaxWidth(),

            ) {
                Text(
                    text = "Payment: RM $amount",
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Return Button
            Button(
                onClick = {
                    navController?.popBackStack()
                },
                enabled = canReturn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canReturn) Color(0xFF1976D2) else Color.LightGray
                )
            ) {
                Text("Return to merchant")
            }
        }
    }
}

@Composable
fun FakeQrCode(
    payload: String,
    onScanned: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
            .clickable {
                // Simulate successful scan
                onScanned()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "QR\n$payload",
            textAlign = TextAlign.Center,
            fontSize = 10.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TngPagePreview() {
    TngPage(
        amount = "10.00",
        navController = null
    )
}
