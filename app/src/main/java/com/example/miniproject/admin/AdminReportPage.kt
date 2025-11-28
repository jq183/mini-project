package com.example.miniproject.AdminScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.ui.theme.*
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

data class Report(
    val id: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val reportedBy: String = "",
    val reportCategory: String = "",
    val description: String = "",
    val status: String = "pending", // pending, resolved, dismissed
    val reportedAt: Timestamp? = null,
    val resolvedAt: Timestamp? = null,
    val adminNotes: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsPage(navController: NavController) {
    var selectedFilter by remember { mutableStateOf("All") }
    var reports by remember { mutableStateOf<List<Report>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedReport by remember { mutableStateOf<Report?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    val currentRoute = navController.currentBackStackEntry?.destination?.route
    val filters = listOf("All", "Pending", "Resolved", "Dismissed")

    // Mock data - Replace with Firebase call
    LaunchedEffect(Unit) {
        // Simulated reports
        reports = listOf(
            Report(
                id = "1",
                projectId = "proj1",
                projectTitle = "AI Learning Platform",
                reportedBy = "user123",
                reportCategory = "Scam",
                description = "This project seems suspicious with unrealistic promises",
                status = "pending",
                reportedAt = Timestamp.now()
            ),
            Report(
                id = "2",
                projectId = "proj2",
                projectTitle = "Help Cancer Patients",
                reportedBy = "Anonymous",
                reportCategory = "Fake Information",
                description = "Contact information doesn't match official records",
                status = "pending",
                reportedAt = Timestamp.now()
            )
        )
        isLoading = false
    }

    val filteredReports = remember(selectedFilter, reports) {
        if (selectedFilter == "All") {
            reports
        } else {
            reports.filter { it.status.equals(selectedFilter, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Reports Management",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundWhite
                )
            )
        },
        bottomBar = {
            AdminBottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(paddingValues)
        ) {
            // Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = filters.indexOf(selectedFilter),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundWhite),
                containerColor = BackgroundWhite,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (filters.indexOf(selectedFilter) < tabPositions.size) {
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[filters.indexOf(selectedFilter)])
                                .height(3.dp)
                                .padding(horizontal = 24.dp)
                                .background(
                                    PrimaryBlue,
                                    RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                )
                        )
                    }
                },
                divider = {}
            ) {
                filters.forEach { filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 15.sp,
                                fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedFilter == filter) PrimaryBlue else TextSecondary
                            )
                        }
                    }
                }
            }

            Divider(color = BorderGray, thickness = 1.dp)

            // Reports List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (filteredReports.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "No reports",
                            modifier = Modifier.size(64.dp),
                            tint = TextSecondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No reports found",
                            fontSize = 16.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredReports) { report ->
                        ReportCard(
                            report = report,
                            onClick = {
                                selectedReport = report
                                showDetailDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Report Detail Dialog
    if (showDetailDialog && selectedReport != null) {
        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Report,
                        contentDescription = "Report",
                        tint = ErrorRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Report Details", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    ReportDetailRow("Project", selectedReport!!.projectTitle)
                    ReportDetailRow("Category", selectedReport!!.reportCategory)
                    ReportDetailRow("Reported By", selectedReport!!.reportedBy)
                    ReportDetailRow("Status", selectedReport!!.status.uppercase())

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Description:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        selectedReport!!.description,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    if (selectedReport!!.adminNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Admin Notes:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            selectedReport!!.adminNotes,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectedReport!!.status == "pending") {
                        Button(
                            onClick = {
                                // Mark as scam
                                showDetailDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ErrorRed
                            )
                        ) {
                            Text("Flag as Scam")
                        }
                        Button(
                            onClick = {
                                showDetailDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue
                            )
                        ) {
                            Text("Dismiss")
                        }
                    }
                    TextButton(onClick = { showDetailDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

@Composable
fun ReportCard(
    report: Report,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val reportDate = report.reportedAt?.toDate()?.let { dateFormat.format(it) } ?: "Unknown"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.projectTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Category",
                            modifier = Modifier.size(14.dp),
                            tint = ErrorRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = report.reportCategory,
                            fontSize = 13.sp,
                            color = ErrorRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                StatusBadge(status = report.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = report.description,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Reporter",
                        modifier = Modifier.size(14.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = report.reportedBy,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Date",
                        modifier = Modifier.size(14.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = reportDate,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, icon) = when (status.lowercase()) {
        "pending" -> WarningOrange to Icons.Default.HourglassEmpty
        "reviewed" -> InfoBlue to Icons.Default.Visibility
        "resolved" -> SuccessGreen to Icons.Default.CheckCircle
        "dismissed" -> TextSecondary to Icons.Default.Cancel
        else -> TextSecondary to Icons.Default.Help
    }

    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = status,
            modifier = Modifier.size(14.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = status.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun ReportDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}