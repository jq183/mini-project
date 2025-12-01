package com.example.miniproject.AdminScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.UserScreen.Project
import com.example.miniproject.repository.ProjectRepository
import com.example.miniproject.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainPage(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedStatus by remember { mutableStateOf("All") } // Changed from category to status
    var selectedSort by remember { mutableStateOf("Newest") }
    var showFilterSheet by remember { mutableStateOf(false) }

    var tempCategory by remember { mutableStateOf(selectedCategory) }
    var tempSort by remember { mutableStateOf(selectedSort) }

    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val repository = remember { ProjectRepository() }
    val currentRoute = navController.currentBackStackEntry?.destination?.route

    val statusTabs = listOf("All", "Verified", "Unverified", "Reported")
    val categories = listOf("All", "Technology", "Charity", "Education", "Medical", "Art", "Games")
    val sortOptions = listOf("Most funded", "Newest", "Ending soon", "Popular")

    LaunchedEffect(Unit) {
        repository.getAllProjects(
            onSuccess = { proj ->
                projects = proj
                isLoading = false
            },
            onError = { exception ->
                errorMessage = exception.message
                isLoading = false
            }
        )
    }

    val filteredProjects = remember(selectedCategory, selectedStatus, selectedSort, searchQuery, projects) {
        var filtered = projects

        // Filter by category (from bottom sheet filter)
        if (selectedCategory != "All") {
            filtered = filtered.filter { it.category == selectedCategory }
        }

        // Filter by status (from top tabs)
        filtered = when (selectedStatus) {
            "Verified" -> filtered.filter { it.isOfficial }
            "Unverified" -> filtered.filter { !it.isOfficial }
            "Reported" -> filtered.filter { it.isWarning }
            else -> filtered // "All"
        }

        // Search filter
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }
        }

        // Sort
        when (selectedSort) {
            "Most funded" -> filtered.sortedByDescending { it.currentAmount }
            "Newest" -> filtered
            "Ending soon" -> filtered.sortedBy { it.daysLeft }
            "Popular" -> filtered.sortedByDescending { it.backers }
            else -> filtered
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Manage Projects",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(paddingValues)
        ) {
            // Search Bar and Filter
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundWhite)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.weight(0.85f).height(50.dp),
                        placeholder = { Text("Search") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TextSecondary,
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderGray
                        ),
                        singleLine = true,
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = {
                            tempCategory = selectedCategory
                            tempSort = selectedSort
                            showFilterSheet = true
                        }
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Status Tabs Row (Replaced Category)
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundWhite)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(statusTabs) { status ->
                        val isSelected = selectedStatus == status
                        val isReported = status == "Reported"

                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundWhite)
                                .clickable { selectedStatus = status }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = status,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> PrimaryBlue
                                    else -> TextSecondary
                                }
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(3.dp)
                                        .background(
                                            PrimaryBlue,
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Project Cards
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
            } else if (filteredProjects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No projects found",
                            fontSize = 16.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                items(filteredProjects) { project ->
                    AdminProjectCard(
                        project = project,
                        onClick = {
                            navController.navigate("adminProjectDetail/${project.id}")
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Filter Bottom Sheet (Category & Sort only)
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showFilterSheet = false
                tempCategory = selectedCategory
                tempSort = selectedSort
            },
            sheetState = sheetState,
            containerColor = BackgroundWhite,
            dragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(BorderGray, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter & Sort",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    TextButton(
                        onClick = {
                            tempCategory = "All"
                            tempSort = "Newest"
                        }
                    ) {
                        Text(
                            "Reset",
                            color = PrimaryBlue,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                categories.chunked(2).forEach { rowCategories ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCategories.forEach { category ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { tempCategory = category }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = tempCategory == category,
                                    onClick = { tempCategory = category },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = PrimaryBlue,
                                        unselectedColor = TextSecondary
                                    )
                                )
                                Text(
                                    category,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                        if (rowCategories.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Sort by",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                sortOptions.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { tempSort = option }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = tempSort == option,
                                    onClick = { tempSort = option },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = PrimaryBlue,
                                        unselectedColor = TextSecondary
                                    )
                                )
                                Text(
                                    option,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                        if (rowOptions.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        selectedCategory = tempCategory
                        selectedSort = tempSort
                        showFilterSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Apply Filters",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AdminProjectCard(
    project: Project,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (project.isWarning) ErrorRed.copy(alpha = 0.05f) else BackgroundWhite
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row - Title + Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title + Category
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Category Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (project.category) {
                            "Technology" -> PrimaryBlue.copy(alpha = 0.1f)
                            "Charity" -> SuccessGreen.copy(alpha = 0.1f)
                            "Education" -> WarningOrange.copy(alpha = 0.1f)
                            "Medical" -> ErrorRed.copy(alpha = 0.1f)
                            "Games" -> InfoBlue.copy(alpha = 0.1f)
                            else -> TextSecondary.copy(alpha = 0.1f)
                        }
                    ) {
                        Text(
                            text = project.category,
                            fontSize = 11.sp,
                            color = when (project.category) {
                                "Technology" -> PrimaryBlue
                                "Charity" -> SuccessGreen
                                "Education" -> WarningOrange
                                "Medical" -> ErrorRed
                                "Games" -> InfoBlue
                                else -> TextSecondary
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status Badges Column
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Official Badge
                    if (project.isOfficial) {
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
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SuccessGreen
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TextSecondary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Unverified",
                                    modifier = Modifier.size(14.dp),
                                    tint = TextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Unverified",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Warning Badge
                    if (project.isWarning) {
                        Spacer(modifier = Modifier.height(4.dp))
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
                                text = "Reported",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = ErrorRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = project.description,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Funding Progress
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Funding Progress",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "RM${String.format("%.0f", project.currentAmount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = " / ${String.format("%.0f", project.goalAmount)}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val progress = (project.currentAmount / project.goalAmount * 100).toInt()
                    Text(
                        text = "$progress% funded",
                        fontSize = 11.sp,
                        color = if (progress >= 100) SuccessGreen else TextSecondary,
                        fontWeight = if (progress >= 100) FontWeight.Medium else FontWeight.Normal
                    )
                }

                // Backers
                Column(
                    modifier = Modifier.weight(0.7f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Backers",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${project.backers}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Days Left
                Column(
                    modifier = Modifier.weight(0.7f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Days Left",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (project.daysLeft <= 7) WarningOrange else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${project.daysLeft}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (project.daysLeft <= 7) WarningOrange else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Divider
            HorizontalDivider(
                thickness = 1.dp,
                color = BorderGray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View Details Button
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor =if (project.isWarning)ErrorRed
                                else PrimaryBlue
                    ),
                    border = BorderStroke(1.dp, PrimaryBlue)
                ) {
                    Icon(
                        imageVector = if (project.isWarning) Icons.Default.Report
                                        else Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "View Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                }
            }
        }
    }
}

@Composable
fun AdminBottomNavigationBar(
    navController: NavController,
    currentRoute: String?
) {
    NavigationBar(
        containerColor = BackgroundWhite,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "adminProjects",
            onClick = { navController.navigate("AdminMainPage") },
            icon = {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = "Projects"
                )
            },
            label = { Text("Projects", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryBlue.copy(alpha = 0.1f)
            )
        )

        NavigationBarItem(
            selected = currentRoute == "adminReports",
            onClick = { navController.navigate("adminReports") },
            icon = {
                Icon(
                    Icons.Default.Report,
                    contentDescription = "Reports"
                )
            },
            label = { Text("Reports", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryBlue.copy(alpha = 0.1f)
            )
        )

        NavigationBarItem(
            selected = currentRoute == "adminHistory",
            onClick = { navController.navigate("adminHistory") },
            icon = {
                Icon(
                    Icons.Default.History,
                    contentDescription = "History"
                )
            },
            label = { Text("History", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryBlue.copy(alpha = 0.1f)
            )
        )

        NavigationBarItem(
            selected = currentRoute == "adminDashboard",
            onClick = { navController.navigate("adminDashboard") },
            icon = {
                Icon(
                    Icons.Default.Dashboard,
                    contentDescription = "Dashboard"
                )
            },
            label = { Text("Dashboard", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryBlue.copy(alpha = 0.1f)
            )
        )
    }
}