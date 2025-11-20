package com.example.miniproject.UserScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.BottomNavigationBar
import com.example.miniproject.ui.theme.*
import com.google.firebase.Timestamp
import com.example.miniproject.repository.ProjectRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Project(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val creatorName: String = "",
    val creatorId: String = "",
    val currentAmount: Double = 0.0,
    val goalAmount: Double = 0.0,
    val backers: Int = 0,
    val daysLeft: Int = 0,
    val imageUrl: String = "",
    val status: String = "active",
    val createdAt: Timestamp? = null,
    val isOfficial: Boolean = false,
    val isWarning: Boolean = false,
    val isComplete: Boolean = false
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedSort by remember { mutableStateOf("Newest") }
    var showFilterSheet by remember { mutableStateOf(false) }

    var tempCategory by remember { mutableStateOf(selectedCategory) }
    var tempSort by remember { mutableStateOf(selectedSort) }

    var projects by remember {mutableStateOf<List<Project>>(emptyList())}
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val repository = remember { ProjectRepository() }
    val currentRoute = navController.currentBackStackEntry?.destination?.route
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
    val filteredProjects = remember(selectedCategory, selectedSort, projects,searchQuery) {
        var filtered = projects

        if (searchQuery.isNotEmpty()){
            filtered =filtered.filter { project ->
                project.title.contains(searchQuery, ignoreCase = true) ||
                project.description.contains(searchQuery, ignoreCase = true) ||
                project.creatorName.contains(searchQuery, ignoreCase = true) ||
                project.category.contains(searchQuery, ignoreCase = true)
            }
        }

        if(selectedCategory !="All"){
            filtered = filtered.filter { it.category == selectedCategory }
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
                    IconButton(onClick = { /*  */ }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
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

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(filteredProjects) { project ->
                ProjectCard(
                    project = project,
                    searchQuery,
                    onClick = {
                        navController.navigate("projectDetail/${project.id}")
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

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

                // Sort Section
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
fun ProjectCard(
    project: Project,
    searchQuery: String,
    onClick: () -> Unit
) {
    var showCertifiedTips by remember { mutableStateOf(false) }
    LaunchedEffect(project.id) {
        println("Project: ${project.title}")
        println("isWarning: ${project.isWarning}")
        println("isOfficial: ${project.isOfficial}")
    }
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

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title
                Row (verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()){
                    HighlightSearchText(
                        text = project.title,
                        highlight = searchQuery,
                        normalStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        highlightStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BackgroundWhite,
                            background = PrimaryBlue
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (project.isOfficial){
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            modifier = Modifier.size(20.dp),
                            tint = PrimaryBlue
                        )
                    }
                    if (showCertifiedTips) {
                        Box(
                            modifier = Modifier
                                .offset(y = 28.dp, x = (-70).dp)
                                .width(160.dp)
                                .background(
                                    TextPrimary.copy(alpha = 0.9f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showCertifiedTips = false
                                }
                        ) {
                            Text(
                                text = "Certified by FundSpark",
                                fontSize = 11.sp,
                                color = BackgroundWhite,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                HighlightSearchText(
                    text = project.description,
                    highlight = searchQuery,
                    normalStyle = TextStyle(
                        fontSize = 13.sp,
                        color = TextSecondary
                    ),
                    highlightStyle = TextStyle(
                        fontSize = 13.sp,
                        color = TextPrimary,
                        background = WarningOrange.copy(alpha = 0.3f),
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                        HighlightSearchText(
                            text = project.creatorName,
                            highlight = searchQuery,
                            normalStyle = TextStyle(
                                fontSize = 13.sp,
                                color = TextSecondary
                            ),
                            highlightStyle = TextStyle(
                                fontSize = 13.sp,
                                color = TextPrimary,
                                background = WarningOrange.copy(alpha = 0.3f),
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
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

                if (project.isWarning) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                WarningOrange.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            modifier = Modifier.size(16.dp),
                            tint = WarningOrange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This project may contain suspicious content",
                            fontSize = 12.sp,
                            color = WarningOrange,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightSearchText(
    text: String,
    highlight: String,
    normalStyle: TextStyle = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    ),
    highlightStyle: TextStyle = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = PrimaryBlue,
        background = PrimaryBlue.copy(alpha = 0.2f)
    ),
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    if (highlight.isEmpty()) {
        Text(
            text = text,
            style = normalStyle,
            maxLines = maxLines,
            overflow = overflow
        )
        return
    }

    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val lowerText = text.lowercase()
        val lowerHighlight = highlight.lowercase()

        while (currentIndex < text.length) {
            val startIndex = lowerText.indexOf(lowerHighlight, currentIndex)

            if (startIndex == -1) {
                append(text.substring(currentIndex))
                break
            }

            if (startIndex > currentIndex) {
                append(text.substring(currentIndex, startIndex))
            }

            val endIndex = startIndex + highlight.length
            pushStyle(
                SpanStyle(
                    color = highlightStyle.color,
                    background = highlightStyle.background,
                    fontWeight = highlightStyle.fontWeight
                )
            )
            append(text.substring(startIndex, endIndex))
            pop()

            currentIndex = endIndex
        }
    }

    Text(
        text = annotatedString,
        style = normalStyle,
        maxLines = maxLines,
        overflow = overflow
    )
}