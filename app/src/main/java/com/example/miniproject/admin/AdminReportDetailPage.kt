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
import com.example.miniproject.UserScreen.Project
import com.example.miniproject.repository.Admin
import com.example.miniproject.repository.GroupedReport
import com.example.miniproject.repository.Report
import com.example.miniproject.ui.theme.*
import com.example.miniproject.repository.ReportRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.miniproject.repository.ProjectRepository
import com.example.miniproject.repository.AdminRepository

data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportDetailPage(
    navController: NavController,
    projectId: String
) {
    val scope = rememberCoroutineScope()
    val reportRepository = remember { ReportRepository() }

    var groupedReport by remember { mutableStateOf<GroupedReport?>(null) }
    var projectData by remember { mutableStateOf<Project?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showActionDialog by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val projectRepository = remember { ProjectRepository() }
    val adminRepository = remember { AdminRepository() }
    var currentAdmin by remember { mutableStateOf<Admin?>(null) }
    var showVerificationWarningDialog by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        scope.launch {
            isLoading = true
            errorMessage = null
            currentAdmin = adminRepository.getCurrentAdmin()

            try {
                // Load reports first
                val result = reportRepository.getReportsForProject(projectId)

                result.fold(
                    onSuccess = { fetchedReports ->
                        if (fetchedReports.isNotEmpty()) {
                            // Load project data to get category
                            projectRepository.getProjectById(
                                projectId = projectId,
                                onSuccess = { project ->
                                    projectData = project

                                    // Now create grouped report with project category
                                    val categoryBreakdown = fetchedReports
                                        .groupBy { it.reportCategory }
                                        .mapValues { it.value.size }

                                    groupedReport = GroupedReport(
                                        projectId = projectId,
                                        projectTitle = fetchedReports.first().projectTitle,
                                        reports = fetchedReports.sortedByDescending { it.reportedAt?.seconds ?: 0 },
                                        totalReports = fetchedReports.size,
                                        latestReport = fetchedReports.maxByOrNull { it.reportedAt?.seconds ?: 0 }!!,
                                        categoryBreakdown = categoryBreakdown,
                                        projectCategory = project.category, // Use project category here
                                    )
                                    isLoading = false
                                },
                                onError = { exception ->
                                    errorMessage = exception.message ?: "Failed to load project data"
                                    isLoading = false
                                }
                            )
                        } else {
                            errorMessage = "No reports found for this project"
                            isLoading = false
                        }
                    },
                    onFailure = { exception ->
                        errorMessage = exception.message ?: "Failed to load reports"
                        isLoading = false
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "An error occurred"
                isLoading = false
            }
        }
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
            if (groupedReport != null && groupedReport!!.reports.any { it.status == "pending" }) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = BackgroundWhite
                ) {
                    Button(
                        onClick = { showActionDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed
                        ),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = BackgroundWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Take Action", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading report details...", color = TextSecondary)
                }
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = ErrorRed
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        errorMessage!!,
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigateUp() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        )
                    ) {
                        Text("Go Back")
                    }
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
                                                "scam" -> ErrorRed.copy(alpha = 0.1f)
                                                "inappropriate_content" -> WarningOrange.copy(alpha = 0.1f)
                                                else -> TextSecondary.copy(alpha = 0.1f)
                                            },
                                            modifier = Modifier.size(8.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = category.replace("_", " ").capitalize(),
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (category) {
                                            "scam" -> ErrorRed.copy(alpha = 0.1f)
                                            "inappropriate_content" -> WarningOrange.copy(alpha = 0.1f)
                                            else -> TextSecondary.copy(alpha = 0.1f)
                                        }
                                    ) {
                                        Text(
                                            text = "$count ${if (count == 1) "report" else "reports"}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (category) {
                                                "scam" -> ErrorRed
                                                "inappropriate_content" -> WarningOrange
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
                                    groupedReport!!.mostCommonCategory.replace("_", " ").capitalize(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorRed
                                )
                            }
                        }
                    }
                }

                // Project Details Card - Using real Firebase data
                if (projectData != null) {
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
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Project Details",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    TextButton(
                                        onClick = { navController.navigate("adminProjectDetail/$projectId") }
                                    ) {
                                        Text("View Full", fontSize = 13.sp)
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                ProjectInfoRow("Category", projectData!!.category)
                                ProjectInfoRow("Creator", projectData!!.creatorName)
                                ProjectInfoRow("Status", projectData!!.status.replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase() else it.toString()
                                })
                                ProjectInfoRow(
                                    "Funding",
                                    "RM ${String.format("%.0f", projectData!!.currentAmount)} / RM ${String.format("%.0f", projectData!!.goalAmount)}"
                                )
                                ProjectInfoRow("Backers", "${projectData!!.backers}")
                                ProjectInfoRow("Days Left", "${projectData!!.daysLeft}")

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (projectData!!.isOfficial) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SuccessGreen.copy(alpha = 0.1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = "Verified",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = SuccessGreen
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Verified",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = SuccessGreen
                                                )
                                            }
                                        }
                                    }

                                    if (projectData!!.isWarning) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ErrorRed.copy(alpha = 0.1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = "Warning",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = ErrorRed
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Flagged",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = ErrorRed
                                                )
                                            }
                                        }
                                    }
                                }
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
        ActionSelectionDialog(
            groupedReport = groupedReport,
            onDismiss = { showActionDialog = false },
            onActionSelected = { action ->
                selectedAction = action
                showActionDialog = false
                showConfirmDialog = true
            }
        )
    }

    // Confirmation Dialog
    if (showConfirmDialog && selectedAction != null) {
        // Check if trying to resolve a verified project
        if (selectedAction == "resolve" && projectData?.isOfficial == true) {
            // Show verification warning dialog instead
            AlertDialog(
                onDismissRequest = {
                    showConfirmDialog = false
                    selectedAction = null
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(56.dp)
                    )
                },
                title = {
                    Text(
                        "Warning: Verified Project",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "This project is currently VERIFIED. Resolving reports will flag and suspend this verified project.",
                            fontSize = 15.sp,
                            color = TextPrimary
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WarningOrange.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = WarningOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "The project will lose its verified status and be flagged for users",
                                    fontSize = 13.sp,
                                    color = WarningOrange,
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
                            "Reports to resolve: ${groupedReport?.totalReports}",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Are you sure you want to flag this verified project?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ErrorRed
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                showConfirmDialog = false
                                selectedAction = null
                            }
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    showConfirmDialog = false

                                    val adminId = currentAdmin?.adminId
                                    if (adminId == null) {
                                        errorMessage = "Admin information not found"
                                        isProcessing = false
                                        return@launch
                                    }

                                    val reportResult = reportRepository.updateAllReportsForProject(
                                        projectId = projectId,
                                        status = "resolved",
                                        adminNotes = "Admin has reviewed and resolved the issue."
                                    )

                                    reportResult.fold(
                                        onSuccess = {
                                            projectRepository.suspendProject(
                                                projectId = projectId,
                                                adminId = adminId,
                                                reportCount = groupedReport?.totalReports ?: 0,
                                                onSuccess = {
                                                    isProcessing = false
                                                    navController.navigateUp()
                                                },
                                                onError = { exception ->
                                                    isProcessing = false
                                                    errorMessage = exception.message
                                                        ?: "Failed to suspend project"
                                                }
                                            )
                                        },
                                        onFailure = { exception ->
                                            isProcessing = false
                                            errorMessage = exception.message
                                                ?: "Failed to update reports"
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ErrorRed
                            ),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = BackgroundWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Yes", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            )
        } else {
            // Original confirmation dialog for other actions
            ActionConfirmationDialog(
                action = selectedAction!!,
                groupedReport = groupedReport,
                onDismiss = {
                    showConfirmDialog = false
                    selectedAction = null
                },
                onConfirm = {
                    scope.launch {
                        isProcessing = true

                        val adminId = currentAdmin?.adminId
                        if (adminId == null) {
                            errorMessage = "Admin information not found"
                            isProcessing = false
                            return@launch
                        }

                        when (selectedAction) {
                            "resolve" -> {
                                val reportResult = reportRepository.updateAllReportsForProject(
                                    projectId = projectId,
                                    status = "resolved",
                                    adminNotes = "Admin has reviewed and resolved the issue."
                                )

                                reportResult.fold(
                                    onSuccess = {
                                        projectRepository.suspendProject(
                                            projectId = projectId,
                                            adminId = adminId,
                                            reportCount = groupedReport?.totalReports ?: 0,
                                            onSuccess = {
                                                isProcessing = false
                                                showConfirmDialog = false
                                                navController.navigateUp()
                                            },
                                            onError = { exception ->
                                                isProcessing = false
                                                errorMessage = exception.message ?: "Failed to suspend project"
                                            }
                                        )
                                    },
                                    onFailure = { exception ->
                                        isProcessing = false
                                        errorMessage = exception.message ?: "Failed to update reports"
                                    }
                                )
                            }
                            "dismiss" -> {
                                val reportResult = reportRepository.updateAllReportsForProject(
                                    projectId = projectId,
                                    status = "dismissed",
                                    adminNotes = "Reports dismissed after review."
                                )

                                reportResult.fold(
                                    onSuccess = {
                                        projectRepository.dismissReport(
                                            projectId = projectId,
                                            adminId = adminId,
                                            reportCount = groupedReport?.totalReports ?: 0,
                                            onSuccess = {
                                                isProcessing = false
                                                showConfirmDialog = false
                                                navController.navigateUp()
                                            },
                                            onError = { exception ->
                                                isProcessing = false
                                                errorMessage = exception.message ?: "Failed to dismiss reports"
                                            }
                                        )
                                    },
                                    onFailure = { exception ->
                                        isProcessing = false
                                        errorMessage = exception.message ?: "Failed to update reports"
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
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


@Composable
fun ActionSelectionDialog(
    groupedReport: GroupedReport?,
    onDismiss: () -> Unit,
    onActionSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                ActionOption(
                    icon = Icons.Default.CheckCircle,
                    title = "Resolve & Flag Project",
                    description = "Mark reports as resolved and add warning badge",
                    color = WarningOrange,
                    onClick = { onActionSelected("resolve")
                    }
                )

                ActionOption(
                    icon = Icons.Default.Cancel,
                    title = "Dismiss Reports",
                    description = "Mark all reports as invalid",
                    color = TextSecondary,
                    onClick = { onActionSelected("dismiss") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PrimaryBlue)
            }
        }
    )
}

@Composable
fun ActionConfirmationDialog(
    action: String,
    groupedReport: GroupedReport?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val (actionTitle, actionDescription, actionColor, actionNote) = when (action) {
        "resolve" -> Tuple4(
            "Resolve & Flag Project",
            "This will mark all reports as resolved AND add a warning badge to the project",
            WarningOrange,
            "The project will remain visible but flagged for users"
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
        onDismissRequest = onDismiss,
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
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = actionColor
                )
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun ProjectInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}