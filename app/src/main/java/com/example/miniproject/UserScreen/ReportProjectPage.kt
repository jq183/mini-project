package com.example.miniproject.UserScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.miniproject.repository.ReportRepository
import com.example.miniproject.repository.Report
import com.example.miniproject.ui.theme.BackgroundWhite
import com.example.miniproject.ui.theme.PrimaryBlue
import com.example.miniproject.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportProjectPage(navController: NavController, projectId: String) {
    var selectedReason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val reasons = listOf("Scam", "Fake Information", "Inappropriate Content", "Others")
    val reportRepository = remember { ReportRepository() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Report Project") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .background(Color.White)
        ) {
            Text(
                text = "Select a reason",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column {
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedReason == reason),
                                onClick = { selectedReason = reason }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedReason == reason),
                            onClick = { selectedReason = reason }
                        )
                        Text(
                            text = reason,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Additional details (Optional)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Describe using your words", color = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (selectedReason.isNotEmpty()) {
                        coroutineScope.launch {
                            try {
                                val nextId = reportRepository.getNextReportId()

                                val now = com.google.firebase.Timestamp.now()

                                val report = Report(
                                    id = nextId,
                                    projectId = projectId,
                                    projectTitle = "Unknown",
                                    reportedBy = "UserTest2",
                                    reportCategory = selectedReason.lowercase().replace(" ", "_"),
                                    description = description.ifEmpty { "Auto generated report" },
                                    status = "pending", // normal user submission
                                    reportedAt = now,
                                    resolvedAt = null,
                                    adminNotes = "",
                                    isAnonymous = false
                                )

                                val result = reportRepository.addReport(report)
                                if (result.isSuccess) {
                                    println("Report successfully added to Firebase: $nextId")
                                    navController.popBackStack()
                                } else {
                                    println("Failed to add report: ${result.exceptionOrNull()}")
                                }
                            } catch (e: Exception) {
                                println("Error adding report: $e")
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Submit Report", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}