package com.example.miniproject.AdminScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.ui.theme.*
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*


data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportDetailPage(
    navController: NavController,
    projectId: String
) {
    var groupedReport by remember { mutableStateOf<GroupedReport?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showActionDialog by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Mock data - Replace with Firebase call
    LaunchedEffect(projectId) {
        // Fetch all reports for this project
        val mockReports = listOf(
            Report(
                id = "1",
                projectId = projectId,
                projectTitle = "AI Learning Platform",
                reportedBy = "user123",
                reportCategory = "Scam",
                description = "This project seems suspicious with unrealistic promises. The creator claims to deliver results in 2 weeks which is impossible for this type of project.",
                status = "pending",
                reportedAt = Timestamp.now()
            ),
            Report(
                id = "2",
                projectId = projectId,
                projectTitle = "AI Learning Platform",
                reportedBy = "user456",
                reportCategory = "Fake Information",
                description = "The creator's credentials don't check out. I couldn't find any record of their claimed educational background.",
                status = "pending",
                reportedAt = Timestamp(Timestamp.now().seconds - 3600, 0)
            ),
            Report(
                id = "3",
                projectId = projectId,
                projectTitle = "AI Learning Platform",
                reportedBy = "Anonymous",
                reportCategory = "Scam",
                description = "Similar project was reported before on another platform. Same person, different project name.",
                status = "pending",
                reportedAt = Timestamp(Timestamp.now().seconds - 7200, 0)
            ),
            Report(
                id = "4",
                projectId = projectId,
                projectTitle = "AI Learning Platform",
                reportedBy = "user789",
                reportCategory = "Fake Information",
                description = "Contact information doesn't match company records. Phone number is disconnected.",
                status = "pending",
                reportedAt = Timestamp(Timestamp.now().seconds - 10800, 0)
            ),
            Report(
                id = "5",
                projectId = projectId,
                projectTitle = "AI Learning Platform",
                reportedBy = "user999",
                reportCategory = "Inappropriate Content",
                description = "Using stock photos and claiming they are real team members.",
                status = "pending",
                reportedAt = Timestamp(Timestamp.now().seconds - 14400, 0)
            )
        )

        val categoryBreakdown = mockReports
            .groupBy { it.reportCategory }
            .mapValues { it.value.size }

        groupedReport = GroupedReport(
            projectId = projectId,
            projectTitle = mockReports.first().projectTitle,
            reports = mockReports.sortedByDescending { it.reportedAt?.seconds ?: 0 },
            totalReports = mockReports.size,
            latestReport = mockReports.maxByOrNull { it.reportedAt?.seconds ?: 0 }!!,
            categoryBreakdown = categoryBreakdown
        )

        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Report Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundWhite
                )
            )
        },
        bottomBar = {
            // Action buttons at bottom
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = BackgroundWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            navController.navigate("adminProjectDetail/$projectId")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Project", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = { showActionDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Action", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (groupedReport == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Reports not found", fontSize = 16.sp, color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundGray)
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Project Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Project Reported",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = groupedReport!!.projectTitle,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Project ID: $projectId",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                // Total reports badge
                                Surface(
                                    shape = CircleShape,
                                    color = ErrorRed
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${groupedReport!!.totalReports}",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BackgroundWhite
                                        )
                                        Text(
                                            text = "Reports",
                                            fontSize = 10.sp,
                                            color = BackgroundWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary Statistics
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Report Summary",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Category breakdown
                            Text(
                                "Categories Reported:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            groupedReport!!.categoryBreakdown.entries.forEach { (category, count) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = when (category) {
                                                "Scam" -> ErrorRed.copy(alpha = 0.1f)
                                                "Fake Information" -> WarningOrange.copy(alpha = 0.1f)
                                                "Inappropriate Content" -> InfoBlue.copy(alpha = 0.1f)
                                                else -> TextSecondary.copy(alpha = 0.1f)
                                            },
                                            modifier = Modifier.size(8.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = category,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (category) {
                                            "Scam" -> ErrorRed.copy(alpha = 0.1f)
                                            "Fake Information" -> WarningOrange.copy(alpha = 0.1f)
                                            "Inappropriate Content" -> InfoBlue.copy(alpha = 0.1f)
                                            else -> TextSecondary.copy(alpha = 0.1f)
                                        }
                                    ) {
                                        Text(
                                            text = "$count ${if (count == 1) "report" else "reports"}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (category) {
                                                "Scam" -> ErrorRed
                                                "Fake Information" -> WarningOrange
                                                "Inappropriate Content" -> InfoBlue
                                                else -> TextSecondary
                                            },
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 6.dp
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            HorizontalDivider(thickness = 1.dp, color = BorderGray)

                            Spacer(modifier = Modifier.height(16.dp))

                            // Most common category
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Most Common Issue:",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    groupedReport!!.mostCommonCategory,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorRed
                                )
                            }
                        }
                    }
                }
                

                // Individual Reports Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "All Reports (${groupedReport!!.totalReports})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Individual Reports List
                items(groupedReport!!.reports) { report ->
                    DetailedReportCard(report = report)
                }
            }
        }
    }

    // Action Selection Dialog
    if (showActionDialog) {
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Take Action on Reports",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                "${groupedReport?.totalReports} reports to handle",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Resolve & Flag (Recommended)
                    ActionOption(
                        icon = Icons.Default.CheckCircle,
                        title = "Resolve & Flag Project",
                        description = "Mark reports as resolved and add warning badge",
                        color = WarningOrange,
                        onClick = {
                            selectedAction = "resolve"
                            showActionDialog = false
                            showConfirmDialog = true
                        }
                    )

                    // Delete Project
                    ActionOption(
                        icon = Icons.Default.Delete,
                        title = "Delete Project",
                        description = "Permanently remove this project and all data",
                        color = ErrorRed,
                        onClick = {
                            selectedAction = "delete"
                            showActionDialog = false
                            showConfirmDialog = true
                        }
                    )

                    // Dismiss Reports
                    ActionOption(
                        icon = Icons.Default.Cancel,
                        title = "Dismiss Reports",
                        description = "Mark all reports as invalid",
                        color = TextSecondary,
                        onClick = {
                            selectedAction = "dismiss"
                            showActionDialog = false
                            showConfirmDialog = true
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showActionDialog = false }) {
                    Text("Cancel", color = PrimaryBlue)
                }
            }
        )
    }


    // Confirmation Dialog
    if (showConfirmDialog && selectedAction != null) {
        val (actionTitle, actionDescription, actionColor, actionNote) = when (selectedAction) {
            "resolve" -> Tuple4(
                "Resolve & Flag Project",
                "This will mark all reports as resolved AND add a warning badge to the project",
                WarningOrange,
                "The project will remain visible but flagged for users"
            )

            "delete" -> Tuple4(
                "Delete Project",
                "This will permanently delete the project and all its data",
                ErrorRed,
                "This action cannot be undone"
            )

            "dismiss" -> Tuple4(
                "Dismiss Reports",
                "This will dismiss all reports as invalid",
                TextSecondary,
                "The project will not be flagged"
            )

            else -> Tuple4("", "", Color.Gray, "")
        }

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = actionColor,
                    modifier = Modifier.size(56.dp)
                )
            },
            title = {
                Text(
                    "Confirm: $actionTitle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        actionDescription,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = actionColor.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = actionColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                actionNote,
                                fontSize = 13.sp,
                                color = actionColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Project: ${groupedReport?.projectTitle}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        "Reports affected: ${groupedReport?.totalReports}",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // TODO: Implement action
                        // If resolve: Also flag the project
                        showConfirmDialog = false
                        navController.navigateUp()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = actionColor
                    )
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
    fun DetailedReportCard(report: Report) {
        val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        val reportDate = report.reportedAt?.toDate()?.let { dateFormat.format(it) } ?: "Unknown"

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = BackgroundWhite
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when (report.reportCategory) {
                                "Scam" -> ErrorRed.copy(alpha = 0.1f)
                                "Fake Information" -> WarningOrange.copy(alpha = 0.1f)
                                "Inappropriate Content" -> InfoBlue.copy(alpha = 0.1f)
                                else -> TextSecondary.copy(alpha = 0.1f)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Report,
                                    contentDescription = null,
                                    tint = when (report.reportCategory) {
                                        "Scam" -> ErrorRed
                                        "Fake Information" -> WarningOrange
                                        "Inappropriate Content" -> InfoBlue
                                        else -> TextSecondary
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = report.reportCategory,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "by ${report.reportedBy}",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (report.status) {
                            "resolved" -> SuccessGreen.copy(alpha = 0.1f)
                            "dismissed" -> TextSecondary.copy(alpha = 0.1f)
                            else -> WarningOrange.copy(alpha = 0.1f)
                        }
                    ) {
                        Text(
                            text = report.status.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (report.status) {
                                "resolved" -> SuccessGreen
                                "dismissed" -> TextSecondary
                                else -> WarningOrange
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceGray
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Report Description:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = report.description,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Time",
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

                    Text(
                        text = "ID: ${report.id}",
                        fontSize = 11.sp,
                        color = TextLight
                    )
                }
            }
        }
    }



@Composable
fun ActionOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

