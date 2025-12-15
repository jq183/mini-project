package com.example.miniproject.admin

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.AdminScreen.AdminAction
import com.example.miniproject.repository.AdminActionRepository
import com.example.miniproject.ui.theme.*
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


data class AdminStats(
    val adminEmail: String,
    val totalActions: Int,
    val verifications: Int,
    val unverifications: Int,
    val deletions: Int,
    val resolved: Int,
    val suspended: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminActionsPage(navController: NavController) {
    var allActions by remember { mutableStateOf<List<AdminAction>>(emptyList()) }
    var filteredActions by remember { mutableStateOf<List<AdminAction>>(emptyList()) }
    var adminStats by remember { mutableStateOf<List<AdminStats>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }
    val repository = remember { AdminActionRepository() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            repository.getAllActions().fold(
                onSuccess = { actions ->
                    allActions = actions
                    filteredActions = actions
                    adminStats = calculateAdminStats(actions)
                    isLoading = false
                },
                onFailure = {
                    isLoading = false
                }
            )
        }
    }

    LaunchedEffect(selectedFilter) {
        filteredActions = repository.filterActionsByType(allActions, selectedFilter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Admin Actions",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
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
                // Admin Rankings
                item {
                    Text(
                        "Top Performing Admins",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                item {
                    AdminRankingCard(adminStats.take(3))
                }

                // Filter Tabs
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    FilterTabs(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { selectedFilter = it }
                    )
                }

                // Actions List
                item {
                    Text(
                        "Recent Actions (${filteredActions.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                items(filteredActions) { action ->
                    ActionItemCard(action = action)
                }
            }
        }
    }
}

@Composable
fun AdminRankingCard(topAdmins: List<AdminStats>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            topAdmins.forEachIndexed { index, admin ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Rank Badge
                        Surface(
                            shape = CircleShape,
                            color = when (index) {
                                0 -> WarningOrange.copy(alpha = 0.2f)
                                1 -> TextSecondary.copy(alpha = 0.2f)
                                2 -> Color(0xFFCD7F32).copy(alpha = 0.2f)
                                else -> BackgroundGray
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "#${index + 1}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (index) {
                                        0 -> WarningOrange
                                        1 -> TextSecondary
                                        2 -> Color(0xFFCD7F32)
                                        else -> TextPrimary
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = admin.adminEmail.substringBefore("@"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = TextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = admin.adminEmail,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${admin.totalActions} total actions",
                                fontSize = 13.sp,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Action breakdown - show ALL action types
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (admin.verifications > 0) {
                            ActionStatRow(
                                count = admin.verifications,
                                label = "Verified",
                                color = SuccessGreen
                            )
                        }
                        if (admin.unverifications > 0) {
                            ActionStatRow(
                                count = admin.unverifications,
                                label = "Unverified",
                                color = WarningOrange
                            )
                        }
                        if (admin.suspended > 0) {
                            ActionStatRow(
                                count = admin.suspended,
                                label = "Suspended",
                                color = ErrorRed
                            )
                        }
                        if (admin.resolved > 0) {
                            ActionStatRow(
                                count = admin.resolved,
                                label = "Resolved",
                                color = InfoBlue
                            )
                        }
                        if (admin.deletions > 0) {
                            ActionStatRow(
                                count = admin.deletions,
                                label = "Deleted",
                                color = Color(0xFF7F8C8D)
                            )
                        }
                    }
                }

                if (index < topAdmins.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = BorderGray
                    )
                }
            }
        }
    }
}
@Composable
fun ActionStatRow(count: Int, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// Update the FilterTabs composable
@Composable
fun FilterTabs(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("All", "Verifications", "Unverifications", "Flags", "Deletions")

    ScrollableTabRow(
        selectedTabIndex = filters.indexOf(selectedFilter),
        containerColor = BackgroundWhite,
        contentColor = PrimaryBlue,
        edgePadding = 0.dp,
        modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
    ) {
        filters.forEach { filter ->
            Tab(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                text = {
                    Text(
                        text = filter,
                        fontSize = 14.sp,
                        fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}



@Composable
fun ActionItemCard(action: AdminAction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Admin Header with Action Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Admin Avatar
                    Surface(
                        shape = CircleShape,
                        color = PrimaryBlue.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = PrimaryBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = action.adminEmail.substringBefore("@"),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = action.adminEmail,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Action Type Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getActionColor(action.actionType).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = getActionIcon(action.actionType),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = getActionColor(action.actionType)
                        )
                        Text(
                            text = action.actionType.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = getActionColor(action.actionType)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Project Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = PrimaryBlue
                )
                Text(
                    text = action.projectTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = action.description,
                fontSize = 13.sp,
                color = TextSecondary
            )

            if (action.additionalInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = action.additionalInfo,
                    fontSize = 11.sp,
                    color = TextLight,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timestamp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatTimestamp(action.timestamp),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

fun getActionIcon(actionType: String) = when (actionType.lowercase()) {
    "verified" -> Icons.Default.Verified
    "unverified" -> Icons.Default.Cancel
    "flagged", "suspended" -> Icons.Default.Flag
    "resolved", "unflagged" -> Icons.Default.CheckCircle
    "deleted" -> Icons.Default.Delete
    else -> Icons.Default.Info
}

fun getActionColor(actionType: String) = when (actionType.lowercase()) {
    "verified" -> SuccessGreen
    "unverified" -> WarningOrange
    "flagged", "suspended" -> ErrorRed
    "resolved", "unflagged" -> InfoBlue
    "deleted" -> Color(0xFF7F8C8D)
    else -> TextSecondary
}

fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return "Unknown"
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

fun calculateAdminStats(actions: List<AdminAction>): List<AdminStats> {
    return actions
        .groupBy { it.adminEmail }
        .map { (email, adminActions) ->
            AdminStats(
                adminEmail = email,
                totalActions = adminActions.size,
                verifications = adminActions.count { it.actionType == "verified" },
                unverifications = adminActions.count { it.actionType == "unverified" },
                deletions = adminActions.count { it.actionType == "deleted" },
                resolved = adminActions.count { it.actionType == "resolved" },
                suspended = adminActions.count { it.actionType == "suspended" }
            )
        }
        .sortedByDescending { it.totalActions }
}