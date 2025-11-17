package com.example.miniproject

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
import com.example.miniproject.ui.theme.*

// 项目数据类
data class Project(
    val id: String,
    val title: String,
    val creator: String,
    val category: String,
    val currentAmount: Double,
    val goalAmount: Double,
    val daysLeft: Int,
    val description: String,
    val backers : Int
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedSort by remember { mutableStateOf("Newest") }
    var showFilterSheet by remember { mutableStateOf(false) }

    // 临时状态 - 在 sheet 里用
    var tempCategory by remember { mutableStateOf(selectedCategory) }
    var tempSort by remember { mutableStateOf(selectedSort) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true  // 改这里！直接完全展开
    )

    val currentRoute = navController.currentBackStackEntry?.destination?.route
    val categories = listOf("All", "Technology", "Charity", "Education", "Medical", "Art", "Games")
    val sortOptions = listOf("Most funded", "Newest", "Ending soon", "Popular")

    // 示例项目数据
    val projects = remember {
        listOf(
            Project(
                id = "1",
                title = "Smart Home Automation System",
                creator = "John Tech",
                category = "Technology",
                currentAmount = 7000.0,
                goalAmount = 10000.0,
                daysLeft = 15,
                description = "Revolutionary home automation system",
                backers = 142
            ),
            Project(
                id = "2",
                title = "Clean Water Initiative",
                creator = "Sarah Hope",
                category = "Charity",
                currentAmount = 2400.0,
                goalAmount = 3000.0,
                daysLeft = 8,
                description = "Providing clean water to rural communities",
                backers = 89
            ),
            Project(
                id = "3",
                title = "Online Learning Platform",
                creator = "Mike Edu",
                category = "Education",
                currentAmount = 5500.0,
                goalAmount = 8000.0,
                daysLeft = 22,
                description = "Free education for everyone",
                backers = 203
            ),
            Project(
                id = "4",
                title = "Medical Equipment for Rural Clinic",
                creator = "Dr. Lisa",
                category = "Medical",
                currentAmount = 12000.0,
                goalAmount = 15000.0,
                daysLeft = 10,
                description = "Essential medical equipment for remote areas",
                backers = 267
            ),
            Project(
                id = "5",
                title = "Indie Game Development",
                creator = "GameDev Studio",
                category = "Games",
                currentAmount = 3200.0,
                goalAmount = 5000.0,
                daysLeft = 30,
                description = "An exciting new adventure game",
                backers = 78
            )
        )
    }

    // 根据分类和排序筛选项目
    val filteredProjects = remember(selectedCategory, selectedSort, projects) {
        var filtered = if (selectedCategory == "All") {
            projects
        } else {
            projects.filter { it.category == selectedCategory }
        }

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
                        text = "FundSpark",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                    )
                },
                actions = {
                    IconButton(onClick = { /* 通知功能 */ }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
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
            BottomNavigationBar(
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
            // Search Bar + Filter
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
                        placeholder = { Text("Search")},
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

            // Category Tabs
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundWhite)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        Column(
                            modifier = Modifier
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = category,
                                fontSize = 15.sp,
                                fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCategory == category) PrimaryBlue else TextSecondary
                            )

                            if (selectedCategory == category) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(3.dp)
                                        .background(PrimaryBlue, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }

            // Spacer
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Project Cards
            items(filteredProjects) { project ->
                ProjectCard(
                    project = project,
                    onClick = {
                        navController.navigate("projectDetail/${project.id}")
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom Padding
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Bottom Sheet - RadioButton 版本
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
                // Header
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

                // Category Section
                Text(
                    "Category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category RadioButtons - 一排两个
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

                // Sort Section
                Text(
                    "Sort by",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

// Sort Options - 一排两个
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

                // Apply Button
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
fun ProjectCard(
    project: Project,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Column {
            // Project Image Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        when (project.category) {
                            "Technology" -> PrimaryBlue.copy(alpha = 0.2f)
                            "Charity" -> SuccessGreen.copy(alpha = 0.2f)
                            "Education" -> WarningOrange.copy(alpha = 0.2f)
                            "Medical" -> ErrorRed.copy(alpha = 0.2f)
                            "Games" -> InfoBlue.copy(alpha = 0.2f)
                            else -> TextSecondary.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (project.category) {
                        "Technology" -> Icons.Default.Computer
                        "Charity" -> Icons.Default.Favorite
                        "Education" -> Icons.Default.School
                        "Medical" -> Icons.Default.LocalHospital
                        "Games" -> Icons.Default.SportsEsports
                        else -> Icons.Default.Image
                    },
                    contentDescription = project.category,
                    modifier = Modifier.size(80.dp),
                    tint = when (project.category) {
                        "Technology" -> PrimaryBlue.copy(alpha = 0.5f)
                        "Charity" -> SuccessGreen.copy(alpha = 0.5f)
                        "Education" -> WarningOrange.copy(alpha = 0.5f)
                        "Medical" -> ErrorRed.copy(alpha = 0.5f)
                        "Games" -> InfoBlue.copy(alpha = 0.5f)
                        else -> TextSecondary.copy(alpha = 0.5f)
                    }
                )
            }

            // Project Info
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title
                Text(
                    text = project.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = project.description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                val progress = (project.currentAmount / project.goalAmount).toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryBlue,
                    trackColor = BorderGray,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Backers",
                            modifier = Modifier.size(14.dp),
                            tint = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${project.backers}",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Days Left",
                            modifier = Modifier.size(14.dp),
                            tint = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${project.daysLeft} days",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Funding Info & Creator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "RM${String.format("%.0f",project.currentAmount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "/ RM${String.format("%.0f",project.goalAmount)}",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = project.creator,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = " • ",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = project.category,
                            fontSize = 13.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
