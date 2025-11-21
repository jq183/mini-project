package com.example.miniproject.AdminScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.ui.theme.*
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

data class AdminAction(
    val id: String = "",
    val actionType: String = "", // "certification_added", "certification_removed", "report_resolved", "project_flagged", "project_removed"
    val projectId: String = "",
    val projectTitle: String = "",
    val adminEmail: String = "",
    val description: String = "",
    val timestamp: Timestamp? = null,
    val additionalInfo: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHistoryPage(navController: NavController) {
    var selectedFilter by remember { mutableStateOf("All") }
    var historyActions by remember { mutableStateOf<List<AdminAction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentRoute = navController.currentBackStackEntry?.destination?.route
    val filters = listOf(
        "All",
        "Certifications",
        "Reports",
        "Projects Flagged",
        "Projects Removed"
    )

    // Mock data - Replace with Firebase call
    LaunchedEffect(Unit) {
        historyActions = listOf(
            AdminAction(
                id = "1",
                actionType = "certification_added",
                projectId = "proj1",
                projectTitle = "Help Children Education Fund",
                adminEmail = "admin@fundspark.com",
                description = "Added official certification mark",
                timestamp = Timestamp.now(),
                additionalInfo = "Verified with official organization documents"
            ),
            AdminAction(
                id = "2",
                actionType = "report_resolved",
                projectId = "proj2",
                projectTitle = "Tech Startup Funding",
                adminEmail = "admin@fundspark.com",
                description = "Resolved scam report - Found legitimate",
                timestamp = Timestamp.now(),
                additionalInfo = "Report dismissed after verification"
            ),
            AdminAction(
                id = "3",
                actionType = "project_flagged",
                projectId = "proj3",
                projectTitle = "Fake Charity Campaign",
                adminEmail = "admin@fundspark.com",
                description = "Flagged project as suspicious",
                timestamp = Timestamp.now(),
                additionalInfo = "Multiple scam reports received"
            ),
            AdminAction(
                id = "4",
                actionType = "certification_removed",
                projectId = "proj4",
                projectTitle = "Medical Research Fund",
                adminEmail = "admin@fundspark.com",
                description = "Removed certification mark",
                timestamp = Timestamp.now(),
                additionalInfo = "Organization status changed"
            )
        )
        isLoading = false
    }

    val filteredActions = remember(selectedFilter, historyActions) {
        when (selectedFilter) {
            "All" -> historyActions
            "Certifications" -> historyActions.filter {
                it.actionType == "certification_added" || it.actionType == "certification_removed"
            }
            "Reports" -> historyActions.filter { it.actionType == "report_resolved" }
            "Projects Flagged" -> historyActions.filter { it.actionType == "project_flagged" }
            "Projects Removed" -> historyActions.filter { it.actionType == "project_removed" }
            else -> historyActions
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Action History",
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
                                .padding(horizontal = 12.dp)
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
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 14.sp,
                                fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedFilter == filter) PrimaryBlue else TextSecondary
                            )
                        }
                    }
                }
            }

            Divider(color = BorderGray, thickness = 1.dp)

            // History List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (filteredActions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "No history",
                            modifier = Modifier.size(64.dp),
                            tint = TextSecondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No history found",
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
                    items(filteredActions) { action ->
                        ActionHistoryCard(action = action)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionHistoryCard(action: AdminAction) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val actionDate = action.timestamp?.toDate()?.let { dateFormat.format(it) } ?: "Unknown"

    val (icon, color, actionLabel) = when (action.actionType) {
        "certification_added" -> Triple(Icons.Default.Verified, SuccessGreen, "Certification Added")
        "certification_removed" -> Triple(Icons.Default.RemoveCircle, WarningOrange, "Certification Removed")
        "report_resolved" -> Triple(Icons.Default.CheckCircle, InfoBlue, "Report Resolved")
        "project_flagged" -> Triple(Icons.Default.Flag, WarningOrange, "Project Flagged")
        "project_removed" -> Triple(Icons.Default.Delete, ErrorRed, "Project Removed")
        else -> Triple(Icons.Default.Info, TextSecondary, "Action Performed")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = actionLabel,
                    modifier = Modifier.size(24.dp),
                    tint = color
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actionLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = action.projectTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = action.description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (action.additionalInfo.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundGray, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            modifier = Modifier.size(14.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = action.additionalInfo,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Admin",
                            modifier = Modifier.size(12.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = action.adminEmail,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Time",
                            modifier = Modifier.size(12.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = actionDate,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}