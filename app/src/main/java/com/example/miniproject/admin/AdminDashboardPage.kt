package com.example.miniproject.AdminScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.ui.theme.*

data class DashboardStats(
    val totalProjects: Int = 0,
    val activeProjects: Int = 0,
    val certifiedProjects: Int = 0,
    val flaggedProjects: Int = 0,
    val totalReports: Int = 0,
    val pendingReports: Int = 0,
    val resolvedReports: Int = 0,
    val totalFunding: Double = 0.0
)

data class RecentActivity(
    val title: String,
    val description: String,
    val time: String,
    val type: String // "report", "certification", "warning"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardPage(navController: NavController) {
    var stats by remember { mutableStateOf(DashboardStats()) }
    var recentActivities by remember { mutableStateOf<List<RecentActivity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentRoute = navController.currentBackStackEntry?.destination?.route

    // Mock data - Replace with Firebase call
    LaunchedEffect(Unit) {
        stats = DashboardStats(
            totalProjects = 156,
            activeProjects = 142,
            certifiedProjects = 23,
            flaggedProjects = 8,
            totalReports = 45,
            pendingReports = 12,
            resolvedReports = 33,
            totalFunding = 1250000.0
        )

        recentActivities = listOf(
            RecentActivity(
                "New scam report",
                "Project 'AI Learning Platform' reported for fake information",
                "5 min ago",
                "report"
            ),
            RecentActivity(
                "Certification added",
                "Verified 'Help Cancer Patients' as official fundraising",
                "1 hour ago",
                "certification"
            ),
            RecentActivity(
                "Project flagged",
                "Flagged 'Quick Money Scheme' as suspicious",
                "2 hours ago",
                "warning"
            ),
            RecentActivity(
                "Report resolved",
                "Dismissed report for 'Tech Startup Fund' - verified legitimate",
                "3 hours ago",
                "report"
            )
        )

        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dashboard",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                    )
                },
                actions = {
                    IconButton(onClick = { /* Refresh data */ }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
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
            AdminBottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute
            )
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundGray)
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PrimaryBlue
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Welcome Back, Admin!",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BackgroundWhite
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Here's what's happening today",
                                    fontSize = 14.sp,
                                    color = BackgroundWhite.copy(alpha = 0.9f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin",
                                modifier = Modifier.size(48.dp),
                                tint = BackgroundWhite.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Stats Grid - Projects
                item {
                    Text(
                        "Projects Overview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Total Projects",
                            value = stats.totalProjects.toString(),
                            icon = Icons.Default.Folder,
                            color = PrimaryBlue
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Active",
                            value = stats.activeProjects.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = SuccessGreen
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Certified",
                            value = stats.certifiedProjects.toString(),
                            icon = Icons.Default.Verified,
                            color = InfoBlue
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Flagged",
                            value = stats.flaggedProjects.toString(),
                            icon = Icons.Default.Warning,
                            color = WarningOrange
                        )
                    }
                }

                // Stats Grid - Reports
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Reports Overview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Total Reports",
                            value = stats.totalReports.toString(),
                            icon = Icons.Default.Report,
                            color = ErrorRed
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Pending",
                            value = stats.pendingReports.toString(),
                            icon = Icons.Default.HourglassEmpty,
                            color = WarningOrange
                        )
                    }
                }

                // Funding Stats
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            SuccessGreen.copy(alpha = 0.1f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = "Funding",
                                        modifier = Modifier.size(24.dp),
                                        tint = SuccessGreen
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Total Platform Funding",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        "RM ${String.format("%,.0f", stats.totalFunding)}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                }
                            }
                        }
                    }
                }

                // Recent Activity
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Recent Activity",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                items(recentActivities) { activity ->
                    RecentActivityCard(activity = activity)
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = value,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(20.dp),
                        tint = color
                    )
                }
            }
        }
    }
}

@Composable
fun RecentActivityCard(activity: RecentActivity) {
    val (icon, color) = when (activity.type) {
        "report" -> Icons.Default.Report to ErrorRed
        "certification" -> Icons.Default.Verified to SuccessGreen
        "warning" -> Icons.Default.Warning to WarningOrange
        else -> Icons.Default.Info to InfoBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = activity.type,
                    modifier = Modifier.size(20.dp),
                    tint = color
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = activity.description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = activity.time,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}