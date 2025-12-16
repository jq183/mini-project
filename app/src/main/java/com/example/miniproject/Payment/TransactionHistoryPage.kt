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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.repository.Donation
import com.example.miniproject.repository.DonationRepository
import com.example.miniproject.repository.Payments
import com.example.miniproject.repository.ProjectRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

// Data model for the UI
data class TransactionUiModel(
    val donation: Donation,
    val projectTitle: String,
    val projectCategory: String,
    val creatorName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryPage(
    navController: NavController
) {
    val donationRepo = remember { DonationRepository() }
    val projectRepo = remember { ProjectRepository() }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // State
    var transactions by remember { mutableStateOf<List<TransactionUiModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Logic: Fetch Data
    LaunchedEffect(Unit) {
        val userId = currentUser?.uid
        if (userId != null) {
            // 1. Get all donations for this user
            donationRepo.getDonationsByUserId(
                userId = userId,
                onSuccess = { donationList ->
                    if (donationList.isEmpty()) {
                        isLoading = false
                        return@getDonationsByUserId
                    }

                    // 2. For each donation, fetch the Project details
                    val loadedTransactions = mutableListOf<TransactionUiModel>()
                    var processedCount = 0

                    donationList.forEach { donation ->
                        // FIX: Use updated property 'projectId' (camelCase)
                        projectRepo.getProjectById(
                            projectId = donation.projectId,
                            onSuccess = { project ->
                                synchronized(loadedTransactions) {
                                    loadedTransactions.add(
                                        TransactionUiModel(
                                            donation = donation,
                                            // Prefer the live project title, fallback to donation snapshot if needed
                                            projectTitle = project.title,
                                            projectCategory = project.category,
                                            creatorName = project.creatorName
                                        )
                                    )
                                    processedCount++
                                    // Check if this was the last one
                                    if (processedCount == donationList.size) {
                                        // FIX: Use updated property 'donatedAt'
                                        transactions = loadedTransactions.sortedByDescending { it.donation.donatedAt }
                                        isLoading = false
                                    }
                                }
                            },
                            onError = {
                                // Even if project load fails, we show the donation with the stored Title/Unknowns
                                synchronized(loadedTransactions) {
                                    loadedTransactions.add(
                                        TransactionUiModel(
                                            donation = donation,
                                            // Fallback to the title stored in donation record if project fetch fails
                                            projectTitle = donation.projectTitle.ifEmpty { "Unknown Project" },
                                            projectCategory = "Unknown",
                                            creatorName = "Unknown"
                                        )
                                    )
                                    processedCount++
                                    if (processedCount == donationList.size) {
                                        // FIX: Use updated property 'donatedAt'
                                        transactions = loadedTransactions.sortedByDescending { it.donation.donatedAt }
                                        isLoading = false
                                    }
                                }
                            }
                        )
                    }
                },
                onError = { e ->
                    errorMessage = e.message
                    isLoading = false
                }
            )
        } else {
            errorMessage = "User not logged in"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2))
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "Error",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                transactions.isEmpty() -> {
                    Text(
                        text = "No transactions found.",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // THE LIST
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(transactions) { item ->
                            TransactionCard(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionCard(item: TransactionUiModel) {
    // Helper formats
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    // FIX: Use updated property 'donatedAt'
    val date = item.donation.donatedAt.toDate()

    val paymentIcon = when (item.donation.paymentMethod) {
        Payments.OnlineBanking -> Icons.Default.AccountBalance
        Payments.TnG -> Icons.Default.CreditCard // Placeholder for TnG
        Payments.Wallet -> Icons.Default.AccountBalanceWallet
        else -> Icons.Default.CreditCard
    }

    val paymentLabel = when (item.donation.paymentMethod) {
        Payments.OnlineBanking -> "Online Banking"
        Payments.TnG -> "TnG E-Wallet"
        Payments.Wallet -> "Wallet Balance"
        else -> "Payment"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE: Details
            Column(modifier = Modifier.weight(1f)) {
                // Project Title
                Text(
                    text = item.projectTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                // Creator • Category
                Text(
                    text = "${item.creatorName} • ${item.projectCategory}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Method Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = paymentIcon,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = paymentLabel,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Date Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.DateRange, // Or Calendar icon
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${dateFormat.format(date)}   ${timeFormat.format(date)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // RIGHT SIDE: Amount
            Column(horizontalAlignment = Alignment.End) {
                // Arrow Icon
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Amount Text
                Text(
                    text = "RM${String.format("%.2f", item.donation.amount)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}