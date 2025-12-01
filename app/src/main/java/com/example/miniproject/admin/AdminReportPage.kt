package com.example.miniproject.AdminScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.repository.AdminRepository
import com.example.miniproject.repository.GroupedReport
import com.example.miniproject.repository.Report
import com.example.miniproject.repository.ReportRepository
import com.example.miniproject.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsPage(navController: NavController) {
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedSort by remember { mutableStateOf("Newest") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var reports by remember { mutableStateOf<List<Report>>(emptyList()) }
    var groupedReports by remember { mutableStateOf<List<GroupedReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedGroupedReport by remember { mutableStateOf<GroupedReport?>(null) }
    var showReportsDialog by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<Report?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val adminRepository = remember { AdminRepository() }
    val reportRepository = remember { ReportRepository() }
    val scope = rememberCoroutineScope()

    val currentRoute = navController.currentBackStackEntry?.destination?.route
    val statusFilters = listOf("All", "Pending", "Resolved", "Dismissed")
    val categories = listOf("All", "Technology", "Charity", "Education", "Medical", "Art", "Games")
    val sortOptions = listOf("Newest", "Oldest", "Most Reports")

    // 从 Firebase 加载所有数据
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                val result = reportRepository.getAllReports()

                result.fold(
                    onSuccess = { fetchedReports ->
                        reports = fetchedReports

                        // Debug: Print project IDs
                        fetchedReports.groupBy { it.projectId }.forEach { (projectId, reports) ->
                        }

                        groupedReports = reportRepository.groupReportsByProject(fetchedReports)

                        isLoading = false
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

    // 过滤和排序逻辑
    val filteredAndSortedReports = remember(selectedStatusFilter, selectedCategory, selectedSort, groupedReports) {
        var filtered = groupedReports

        // 按状态过滤
        if (selectedStatusFilter != "All") {
            filtered = filtered.filter { grouped ->
                when (selectedStatusFilter.lowercase()) {
                    "pending" -> grouped.pendingCount > 0
                    "resolved" -> grouped.resolvedCount > 0
                    "dismissed" -> grouped.dismissedCount > 0
                    else -> true
                }
            }
        }


        if (selectedCategory != "All") {
            filtered = filtered.filter { grouped ->
                // 这里需要从项目获取类别信息
                // 暂时通过 projectId 匹配
                true // 需要额外的项目数据才能过滤
            }
        }

        // 排序
        when (selectedSort) {
            "Newest" -> filtered.sortedByDescending { it.latestReport.reportedAt?.seconds ?: 0 }
            "Oldest" -> filtered.sortedBy { it.latestReport.reportedAt?.seconds ?: 0 }
            "Most Reports" -> filtered.sortedByDescending { it.totalReports }
            else -> filtered
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Reports Management",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundWhite
                ),
                actions = {
                    // Filter按钮
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = PrimaryBlue
                        )
                    }
                    // 刷新按钮
                    IconButton(onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val result = reportRepository.getAllReports()
                                result.fold(
                                    onSuccess = { fetchedReports ->
                                        reports = fetchedReports
                                        groupedReports = reportRepository.groupReportsByProject(fetchedReports)
                                    },
                                    onFailure = { }
                                )
                            } catch (e: Exception) {
                                errorMessage = e.message
                            }
                            isLoading = false
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = PrimaryBlue
                        )
                    }
                }
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
            // Status Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = statusFilters.indexOf(selectedStatusFilter),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundWhite),
                containerColor = BackgroundWhite,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (statusFilters.indexOf(selectedStatusFilter) < tabPositions.size) {
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[statusFilters.indexOf(selectedStatusFilter)])
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
                statusFilters.forEach { filter ->
                    Tab(
                        selected = selectedStatusFilter == filter,
                        onClick = { selectedStatusFilter = filter },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 15.sp,
                                fontWeight = if (selectedStatusFilter == filter) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedStatusFilter == filter) PrimaryBlue else TextSecondary
                            )
                        }
                    }
                }
            }

            // Active filters display
            if (selectedCategory != "All" || selectedSort != "Newest") {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundWhite)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedCategory != "All") {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = { selectedCategory = "All" },
                                label = { Text(selectedCategory, fontSize = 13.sp) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove filter",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                    if (selectedSort != "Newest") {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = { selectedSort = "Newest" },
                                label = { Text(selectedSort, fontSize = 13.sp) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove filter",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 显示错误信息
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = ErrorRed.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = ErrorRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = ErrorRed,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Grouped Reports List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading reports...", color = TextSecondary)
                    }
                }
            } else if (filteredAndSortedReports.isEmpty()) {
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
                    items(filteredAndSortedReports) { groupedReport ->
                        GroupedReportCard(
                            groupedReport = groupedReport,
                            onClick = {
                                selectedGroupedReport = groupedReport
                                showReportsDialog = true
                            },
                            navController = navController
                        )
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = BackgroundWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Filter & Sort",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Category Filter
                Text(
                    "Category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = BackgroundWhite
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sort Options
                Text(
                    "Sort By",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                sortOptions.forEach { sort ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSort = sort }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSort == sort,
                            onClick = { selectedSort = sort },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PrimaryBlue
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sort,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Apply Button
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Apply Filters")
                }

                Spacer(modifier = Modifier.height(16.dp))
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
    onClick: () -> Unit,
    navController: NavController
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val latestReportDate = groupedReport.latestReport.reportedAt?.toDate()?.let {
        dateFormat.format(it)
    } ?: "Unknown"

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

            // Status breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (groupedReport.pendingCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = WarningOrange.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Pending (${groupedReport.pendingCount})",
                            fontSize = 11.sp,
                            color = WarningOrange,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (groupedReport.resolvedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SuccessGreen.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Resolved (${groupedReport.resolvedCount})",
                            fontSize = 11.sp,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (groupedReport.dismissedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TextSecondary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Dismissed (${groupedReport.dismissedCount})",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    border = BorderStroke(1.dp, ErrorRed)
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
            Surface(
                shape = CircleShape,
                color = when (report.reportCategory) {
                    "scam" -> ErrorRed.copy(alpha = 0.1f)
                    "inappropriate_content" -> WarningOrange.copy(alpha = 0.1f)
                    else -> TextSecondary.copy(alpha = 0.1f)
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Report,
                        contentDescription = null,
                        tint = when (report.reportCategory) {
                            "scam" -> ErrorRed
                            "inappropriate_content" -> WarningOrange
                            else -> TextSecondary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.reportCategory.replace("_", " ").capitalize(),
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