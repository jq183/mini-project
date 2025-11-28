package com.example.miniproject.AdminScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.ui.theme.*
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

// 单个举报
data class Report(
    val id: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val reportedBy: String = "",
    val reportCategory: String = "",
    val description: String = "",
    val status: String = "pending",
    val reportedAt: Timestamp? = null,
    val resolvedAt: Timestamp? = null,
    val adminNotes: String = ""
)

// 合并后的举报组
data class GroupedReport(
    val projectId: String,
    val projectTitle: String,
    val reports: List<Report>, // 所有针对这个项目的举报
    val totalReports: Int,
    val latestReport: Report,
    val categoryBreakdown: Map<String, Int> // 每个类别的举报数量
) {
    val mostCommonCategory: String
        get() = categoryBreakdown.maxByOrNull { it.value }?.key ?: "Unknown"

    val status: String
        get() {
            val statuses = reports.map { it.status }
            return when {
                statuses.all { it == "resolved" } -> "resolved"
                statuses.all { it == "dismissed" } -> "dismissed"
                statuses.any { it == "pending" } -> "pending"
                else -> "pending"
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsPage(navController: NavController) {
    var selectedFilter by remember { mutableStateOf("All") }
    var reports by remember { mutableStateOf<List<Report>>(emptyList()) }
    var groupedReports by remember { mutableStateOf<List<GroupedReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedGroupedReport by remember { mutableStateOf<GroupedReport?>(null) }
    var showReportsDialog by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<Report?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    val currentRoute = navController.currentBackStackEntry?.destination?.route
    val filters = listOf("All", "Pending", "Resolved", "Dismissed")

    // Mock data - Replace with Firebase call
    LaunchedEffect(Unit) {
        // 模拟多个用户举报同一个项目
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
                projectId = "proj1",
                projectTitle = "AI Learning Platform",
                reportedBy = "user456",
                reportCategory = "Fake Information",
                description = "The creator's credentials don't check out",
                status = "pending",
                reportedAt = Timestamp(Timestamp.now().seconds - 3600, 0)
            ),
            Report(
                id = "3",
                projectId = "proj1",
                projectTitle = "AI Learning Platform",
                reportedBy = "Anonymous",
                reportCategory = "Scam",
                description = "Similar project was reported before",
                status = "pending",
                reportedAt = Timestamp(Timestamp.now().seconds - 7200, 0)
            ),
            Report(
                id = "4",
                projectId = "proj2",
                projectTitle = "Help Cancer Patients",
                reportedBy = "user789",
                reportCategory = "Fake Information",
                description = "Contact information doesn't match hospital records",
                status = "pending",
                reportedAt = Timestamp.now()
            ),
            Report(
                id = "5",
                projectId = "proj2",
                projectTitle = "Help Cancer Patients",
                reportedBy = "user999",
                reportCategory = "Inappropriate Content",
                description = "Using stock photos instead of real patients",
                status = "pending",
                reportedAt = Timestamp(Timestamp.now().seconds - 1800, 0)
            )
        )

        // 按项目分组
        groupedReports = reports
            .groupBy { it.projectId }
            .map { (projectId, projectReports) ->
                val categoryBreakdown = projectReports
                    .groupBy { it.reportCategory }
                    .mapValues { it.value.size }

                GroupedReport(
                    projectId = projectId,
                    projectTitle = projectReports.first().projectTitle,
                    reports = projectReports.sortedByDescending { it.reportedAt?.seconds ?: 0 },
                    totalReports = projectReports.size,
                    latestReport = projectReports.maxByOrNull { it.reportedAt?.seconds ?: 0 }!!,
                    categoryBreakdown = categoryBreakdown
                )
            }
            .sortedByDescending { it.totalReports }

        isLoading = false
    }

    val filteredGroupedReports = remember(selectedFilter, groupedReports) {
        if (selectedFilter == "All") {
            groupedReports
        } else {
            groupedReports.filter { it.status.equals(selectedFilter, ignoreCase = true) }
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

            // Grouped Reports List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (filteredGroupedReports.isEmpty()) {
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredGroupedReports) { groupedReport ->
                        GroupedReportCard(
                            groupedReport = groupedReport,
                            navController = navController
                        )
                    }
                }
            }
        }
    }

    // All Reports Dialog for a Project
    if (showReportsDialog && selectedGroupedReport != null) {
        AlertDialog(
            onDismissRequest = { showReportsDialog = false },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            title = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "All Reports",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Surface(
                            shape = CircleShape,
                            color = ErrorRed.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${selectedGroupedReport!!.totalReports}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedGroupedReport!!.projectTitle,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(selectedGroupedReport!!.reports) { report ->
                        IndividualReportItem(
                            report = report,
                            onClick = {
                                selectedReport = report
                                showDetailDialog = true
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            navController.navigate("adminProjectDetail/${selectedGroupedReport!!.projectId}")
                            showReportsDialog = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Project")
                    }

                    Button(
                        onClick = {
                            // Handle all reports at once
                            showReportsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed
                        )
                    ) {
                        Text("Handle All")
                    }

                    TextButton(onClick = { showReportsDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Individual Report Detail Dialog
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
fun GroupedReportCard(
    groupedReport: GroupedReport,
    navController: NavController
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val latestReportDate = groupedReport.latestReport.reportedAt?.toDate()?.let {
        dateFormat.format(it)
    } ?: "Unknown"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with count badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = groupedReport.projectTitle,
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
                            text = groupedReport.mostCommonCategory,
                            fontSize = 13.sp,
                            color = ErrorRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Report count badge
                Surface(
                    shape = CircleShape,
                    color = ErrorRed
                ) {
                    Text(
                        text = "${groupedReport.totalReports} Reports",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BackgroundWhite,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedReport.categoryBreakdown.entries.take(3).forEach { (category, count) ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = WarningOrange.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "$category ($count)",
                            fontSize = 11.sp,
                            color = WarningOrange,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (groupedReport.categoryBreakdown.size > 3) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TextSecondary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "+${groupedReport.categoryBreakdown.size - 3}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Latest report preview
            Text(
                text = "Latest: ${groupedReport.latestReport.description}",
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(thickness = 1.dp, color = BorderGray)

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedButton(
                    onClick = {
                        navController.navigate("adminReportDetail/${groupedReport.projectId}")
                    },
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = ErrorRed
                    ),
                    border = BorderStroke(1.dp, ErrorRed) // 红色边框
                ) {
                    Icon(
                        imageVector = Icons.Default.Report,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "View Reports",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Date",
                        modifier = Modifier.size(12.dp),
                        tint = TextLight
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Latest: $latestReportDate",
                        fontSize = 11.sp,
                        color = TextLight
                    )
                }
            }
        }
    }
}

@Composable
fun IndividualReportItem(
    report: Report,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val reportDate = report.reportedAt?.toDate()?.let { dateFormat.format(it) } ?: "Unknown"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.reportCategory,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "by ${report.reportedBy}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = reportDate,
                    fontSize = 11.sp,
                    color = TextLight
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
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