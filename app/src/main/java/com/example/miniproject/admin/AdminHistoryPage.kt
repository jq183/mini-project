package com.example.miniproject.AdminScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.miniproject.repository.AdminActionRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.miniproject.UserScreen.PageControl

data class AdminAction(
    val id: String = "",
    val actionType: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val adminEmail: String = "",
    val description: String = "",
    val timestamp: Timestamp? = null,
    val additionalInfo: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHistoryPage(navController: NavController) {
    val scope = rememberCoroutineScope()
    val actionRepository = remember { AdminActionRepository() }

    var selectedFilter by remember { mutableStateOf("All") }
    var historyActions by remember { mutableStateOf<List<AdminAction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedAction by remember { mutableStateOf<AdminAction?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    val itemsPerPage = 10
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    val currentRoute = navController.currentBackStackEntry?.destination?.route
    val filters = listOf(
        "All",
        "Verifications",
        "Flags & Warnings",
        "Deletions"
    )

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            errorMessage = null

            val result = actionRepository.getAllActions()

            result.fold(
                onSuccess = { actions ->
                    historyActions = actions
                    isLoading = false
                },
                onFailure = { exception ->
                    errorMessage = exception.message ?: "Failed to load action history"
                    isLoading = false
                }
            )
        }
    }
    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousScrollOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (currentIndex, currentScrollOffset) ->
            isBottomBarVisible = when {
                currentIndex < previousIndex -> true
                currentIndex > previousIndex -> false
                currentScrollOffset < previousScrollOffset -> true
                currentScrollOffset > previousScrollOffset -> false
                else -> isBottomBarVisible
            }

            previousIndex = currentIndex
            previousScrollOffset = currentScrollOffset
        }
    }

    val filteredActions = remember(selectedFilter, historyActions) {
        actionRepository.filterActionsByType(historyActions, selectedFilter)
    }
    val totalPages = (filteredActions.size + itemsPerPage - 1) / itemsPerPage
    val paginatedActions = remember(filteredActions, currentPage) {
        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, filteredActions.size)
        if (startIndex < filteredActions.size) {
            filteredActions.subList(startIndex, endIndex)
        } else {
            emptyList()
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
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            isLoading = true
                            val result = actionRepository.getAllActions()
                            result.fold(
                                onSuccess = { actions ->
                                    historyActions = actions
                                    isLoading = false
                                },
                                onFailure = { exception ->
                                    errorMessage = exception.message
                                    isLoading = false
                                }
                            )
                        }
                    }) {
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
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = expandVertically(
                    animationSpec = tween(200),
                    expandFrom = Alignment.Bottom
                ),
                exit = shrinkVertically(
                    animationSpec = tween(200),
                    shrinkTowards = Alignment.Bottom
                )
            ) {
                AdminBottomNavigationBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
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
            LaunchedEffect(selectedFilter) {
                currentPage = 1
            }

            // Content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading action history...", color = TextSecondary)
                    }
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    val result = actionRepository.getAllActions()
                                    result.fold(
                                        onSuccess = { actions ->
                                            historyActions = actions
                                            isLoading = false
                                        },
                                        onFailure = { exception ->
                                            errorMessage = exception.message
                                            isLoading = false
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
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
                        if (selectedFilter != "All") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Try changing the filter",
                                fontSize = 14.sp,
                                color = TextSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(paginatedActions) { action ->
                        ActionHistoryCard(
                            action = action,
                            onClick = {
                                selectedAction = action
                            },
                            onViewProject = {
                                navController.navigate("adminProjectDetail/${action.projectId}")
                            }
                        )
                    }

                    item {
                        if (totalPages > 1) {
                            PageControl(
                                currentPage = currentPage,
                                totalPages = totalPages,
                                onPageChange = { newPage ->
                                    currentPage = newPage
                                    scope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionHistoryCard(
    action: AdminAction,
    onClick: () -> Unit,
    onViewProject: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val actionDate = action.timestamp?.toDate()?.let { dateFormat.format(it) } ?: "Unknown"

    val (icon, color, actionLabel) = when (action.actionType) {
        "verified" -> Triple(Icons.Default.Verified, SuccessGreen, "Verified")
        "unverified" -> Triple(Icons.Default.RemoveCircle, TextSecondary, "Unverified")
        "flagged", "suspended" -> Triple(Icons.Default.Warning, WarningOrange, "Flagged/Suspended")
        "unflagged", "resolved" -> Triple(Icons.Default.CheckCircle, InfoBlue, "Resolved")
        "deleted" -> Triple(Icons.Default.Delete, ErrorRed, "Deleted")
        else -> Triple(Icons.Default.Info, TextSecondary, "Action")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewProject),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
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

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = color.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = actionLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = action.projectTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
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
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 1.dp, color = BorderGray)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Admin",
                        modifier = Modifier.size(14.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = action.adminEmail.substringBefore("@"),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time",
                        modifier = Modifier.size(14.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = actionDate,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                TextButton(
                    onClick = onViewProject,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("View Project", fontSize = 12.sp)
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

