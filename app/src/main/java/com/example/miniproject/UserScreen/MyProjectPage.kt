package com.example.miniproject.UserScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.BottomNavigationBar
import com.example.miniproject.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProjectsPage(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    val currentRoute = navController.currentBackStackEntry?.destination?.route
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val tabs = listOf("My Projects", "Backed")

    var myProjects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var backedProjects by remember { mutableStateOf<List<Project>>(emptyList()) }

    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(currentUserId, selectedTab) {
        android.util.Log.d("MyProjects", "=== Debug Start ===")
        android.util.Log.d("MyProjects", "Current User ID: '$currentUserId'")
        android.util.Log.d("MyProjects", "User ID length: ${currentUserId.length}")
        android.util.Log.d("MyProjects", "Selected Tab: $selectedTab")

        if (currentUserId.isEmpty()) {
            android.util.Log.e("MyProjects", "User ID is empty!")
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        try {
            if (selectedTab == 0) {
                val allSnapshot = db.collection("projects")
                    .get()
                    .await()

                android.util.Log.d("MyProjects", "Total projects in DB: ${allSnapshot.documents.size}")

                allSnapshot.documents.forEach { doc ->
                    val userId = doc.getString("User_ID")
                    android.util.Log.d("MyProjects", "Project: ${doc.id}, User_ID: '$userId'")
                }

                val snapshot = db.collection("projects")
                    .whereEqualTo("User_ID", currentUserId)
                    .get()
                    .await()

                android.util.Log.d("MyProjects", "Filtered projects: ${snapshot.documents.size}")

                myProjects = snapshot.documents.mapNotNull { doc ->
                    try {
                        android.util.Log.d("MyProjects", "Mapping project: ${doc.id}")
                        val project = Project(
                            id = doc.id,
                            title = doc.getString("Title") ?: "",
                            description = doc.getString("Description") ?: "",
                            category = doc.getString("Category") ?: "",
                            goalAmount = doc.getDouble("Target_Amount") ?: 0.0,
                            currentAmount = doc.getDouble("Current_Amount") ?: 0.0,
                            backers = doc.getLong("backers")?.toInt() ?: 0,
                            daysLeft = doc.getLong("daysLeft")?.toInt() ?: 0,
                            creatorId = doc.getString("User_ID") ?: "",
                            creatorName = doc.getString("creatorName") ?: "",
                            status = doc.getString("Status") ?: "active"
                        )
                        android.util.Log.d("MyProjects", "Successfully mapped: ${project.title}")
                        project
                    } catch (e: Exception) {
                        android.util.Log.e("MyProjects", "Error mapping project ${doc.id}: ${e.message}", e)
                        null
                    }
                }

                android.util.Log.d("MyProjects", "Final myProjects count: ${myProjects.size}")

            } else {
                val backingsSnapshot = db.collection("backings")
                    .whereEqualTo("userId", currentUserId)
                    .get()
                    .await()

                android.util.Log.d("MyProjects", "Backings found: ${backingsSnapshot.documents.size}")

                val projectIds = backingsSnapshot.documents.mapNotNull {
                    it.getString("projectId")
                }.distinct()

                android.util.Log.d("MyProjects", "Unique project IDs: ${projectIds.size}")

                if (projectIds.isNotEmpty()) {
                    val allProjects = mutableListOf<Project>()
                    projectIds.chunked(10).forEach { chunk ->
                        val projectsSnapshot = db.collection("projects")
                            .whereIn("__name__", chunk)
                            .get()
                            .await()

                        val projects = projectsSnapshot.documents.mapNotNull { doc ->
                            try {
                                Project(
                                    id = doc.id,
                                    title = doc.getString("Title") ?: "",
                                    description = doc.getString("Description") ?: "",
                                    category = doc.getString("Category") ?: "",
                                    goalAmount = doc.getDouble("Target_Amount") ?: 0.0,
                                    currentAmount = doc.getDouble("Current_Amount") ?: 0.0,
                                    backers = doc.getLong("backers")?.toInt() ?: 0,
                                    daysLeft = doc.getLong("daysLeft")?.toInt() ?: 0,
                                    creatorId = doc.getString("User_ID") ?: "",
                                    creatorName = doc.getString("creatorName") ?: "",
                                    status = doc.getString("Status") ?: "active"
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("MyProjects", "Error mapping backed project: ${e.message}", e)
                                null
                            }
                        }
                        allProjects.addAll(projects)
                    }
                    backedProjects = allProjects
                    android.util.Log.d("MyProjects", "Final backed projects count: ${backedProjects.size}")
                } else {
                    backedProjects = emptyList()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MyProjects", "Error loading projects: ${e.message}", e)
            e.printStackTrace()
            scope.launch {
                snackbarHostState.showSnackbar("Failed to load projects: ${e.message}")
            }
        }
        isLoading = false
        android.util.Log.d("MyProjects", "=== Debug End ===")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Projects",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("createProject") },
                containerColor = PrimaryBlue,
                contentColor = BackgroundWhite
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Project"
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BackgroundWhite,
                contentColor = PrimaryBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryBlue,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = PrimaryBlue,
                        unselectedContentColor = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                currentUserId.isEmpty() -> {
                    EmptyStateView(
                        icon = Icons.Default.Person,
                        title = "Please Log In",
                        message = "Log in to view and create your projects",
                        actionText = "Go to Login",
                        onAction = { navController.navigate("login") }
                    )
                }
                selectedTab == 0 -> {
                    if (myProjects.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Folder,
                            title = "No Projects Yet",
                            message = "Start your first project and bring your ideas to life!",
                            actionText = "Create Project",
                            onAction = { navController.navigate("createProject") }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(myProjects) { project ->
                                MyProjectCard(
                                    project = project,
                                    onClick = {
                                        navController.navigate("projectDetail/${project.id}")
                                    },
                                    onEdit = {
                                        navController.navigate("editProject/${project.id}")
                                    },
                                    onDelete = {
                                        projectToDelete = project
                                        showDeleteDialog = true
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
                selectedTab == 1 -> {
                    if (backedProjects.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Favorite,
                            title = "No Backed Projects",
                            message = "Support amazing projects and help bring them to life!",
                            actionText = "Explore Projects",
                            onAction = { navController.navigate("mainPage") }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(backedProjects) { project ->
                                BackedProjectCard(
                                    project = project,
                                    onClick = {
                                        navController.navigate("projectDetail/${project.id}")
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Delete Project?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to delete \"${projectToDelete?.title}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun MyProjectCard(
    project: Project,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

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
            // Project Image with Status Badge and Menu
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

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(
                            BackgroundWhite.copy(alpha = 0.95f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                when (project.status) {
                                    "active" -> SuccessGreen
                                    "completed" -> PrimaryBlue
                                    "suspended" -> ErrorRed
                                    else -> TextSecondary
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (project.status) {
                            "active" -> "Active"
                            "completed" -> "Completed"
                            "suspended" -> "Suspended"
                            else -> "Unknown"
                        },
                        color = when (project.status) {
                            "active" -> SuccessGreen
                            "completed" -> PrimaryBlue
                            "suspended" -> ErrorRed
                            else -> TextSecondary
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = BackgroundWhite.copy(alpha = 0.95f),
                        shadowElevation = 2.dp
                    ) {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(BackgroundWhite)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Edit",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = PrimaryBlue
                                )
                            }
                        )
                        HorizontalDivider(color = BorderGray)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete",
                                    color = ErrorRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = ErrorRed
                                )
                            }
                        )
                    }
                }
            }

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

                Text(
                    text = project.description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                val progress = if (project.goalAmount > 0) {
                    (project.currentAmount / project.goalAmount).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryBlue,
                    trackColor = BorderGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Info Row
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
                            text = "RM${String.format("%.0f", project.currentAmount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "/ RM${String.format("%.0f", project.goalAmount)}",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }

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

@Composable
fun BackedProjectCard(
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
            // Project Image
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
                Text(
                    text = project.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = project.description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                val progress = if (project.goalAmount > 0) {
                    (project.currentAmount / project.goalAmount).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryBlue,
                    trackColor = BorderGray
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolunteerActivism,
                            contentDescription = "Backed",
                            modifier = Modifier.size(14.dp),
                            tint = PrimaryBlue
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "RM 100",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
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
                            text = "RM${String.format("%.0f", project.currentAmount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "/ RM${String.format("%.0f", project.goalAmount)}",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = project.creatorName,
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

@Composable
fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionText: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TextSecondary.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = actionText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}