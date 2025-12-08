package com.example.miniproject.AdminScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.miniproject.UserScreen.Project
import com.example.miniproject.repository.ProjectRepository
import com.example.miniproject.repository.AdminRepository
import com.example.miniproject.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProjectDetail(
    navController: NavController,
    projectId: String
) {
    var project by remember { mutableStateOf<Project?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showManageDialog by remember { mutableStateOf(false) }
    var currentAdminId by remember { mutableStateOf<String?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val repository = remember { ProjectRepository() }
    val adminRepository = remember { AdminRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        scope.launch {
            val admin = adminRepository.getCurrentAdmin()
            currentAdminId = admin?.adminId
        }
    }

    LaunchedEffect(projectId) {
        repository.getProjectById(
            projectId = projectId,
            onSuccess = { proj ->
                project = proj
                isLoading = false
            },
            onError = {
                isLoading = false
            }
        )
    }

    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar(
                message = snackbarMessage,
                duration = SnackbarDuration.Short
            )
            showSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Project Details",
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
        } else if (project == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Project not found",
                    fontSize = 16.sp,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundGray)
                    .padding(paddingValues)
            ) {
                item {
                    AsyncImage(
                        model = project!!.imageUrl,
                        contentDescription = "Project Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundWhite)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (project!!.isOfficial) SuccessGreen.copy(alpha = 0.1f)
                            else TextSecondary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (project!!.isOfficial) Icons.Default.Verified
                                    else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (project!!.isOfficial) SuccessGreen else TextSecondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (project!!.isOfficial) "Verified" else "Unverified",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (project!!.isOfficial) SuccessGreen else TextSecondary
                                )
                            }
                        }

                        if (project!!.isWarning) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ErrorRed.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = ErrorRed
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Flagged",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ErrorRed
                                    )
                                }
                            }
                        }

                        if (project!!.status == "suspended") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = WarningOrange.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = WarningOrange
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Suspended",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = WarningOrange
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (project!!.category) {
                                "Technology" -> PrimaryBlue.copy(alpha = 0.1f)
                                "Charity" -> SuccessGreen.copy(alpha = 0.1f)
                                "Education" -> WarningOrange.copy(alpha = 0.1f)
                                "Medical" -> ErrorRed.copy(alpha = 0.1f)
                                "Games" -> InfoBlue.copy(alpha = 0.1f)
                                else -> TextSecondary.copy(alpha = 0.1f)
                            }
                        ) {
                            Text(
                                text = project!!.category,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = when (project!!.category) {
                                    "Technology" -> PrimaryBlue
                                    "Charity" -> SuccessGreen
                                    "Education" -> WarningOrange
                                    "Medical" -> ErrorRed
                                    "Games" -> InfoBlue
                                    else -> TextSecondary
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = BackgroundWhite
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = project!!.title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "by ${project!!.creatorName}",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = BorderGray
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Description",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = project!!.description,
                                fontSize = 14.sp,
                                color = TextSecondary,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = BackgroundWhite
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Funding Progress",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "RM${String.format("%.2f", project!!.currentAmount)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                                Text(
                                    text = "of RM${String.format("%.2f", project!!.goalAmount)}",
                                    fontSize = 16.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val progress = (project!!.currentAmount / project!!.goalAmount).coerceIn(
                                0.0, 1.0).toFloat()

                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = PrimaryBlue,
                                trackColor = BorderGray
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.People,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = PrimaryBlue
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${project!!.backers}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Backers",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (project!!.daysLeft <= 7) WarningOrange else PrimaryBlue
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${project!!.daysLeft}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (project!!.daysLeft <= 7) WarningOrange else TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Days Left",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (progress >= 1f) SuccessGreen else TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Funded",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = BackgroundWhite
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Project Information",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            InfoRow("Project ID", project!!.id)
                            InfoRow("Creator", project!!.creatorName)
                            InfoRow("Category", project!!.category)
                            InfoRow("Status", project!!.status.capitalize())
                            InfoRow(
                                "Verification",
                                if (project!!.isOfficial) "Verified" else "Unverified"
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showManageDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PrimaryBlue
                            ),
                            border = BorderStroke(1.5.dp, PrimaryBlue),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = PrimaryBlue,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Manage Project",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showManageDialog) {
        AlertDialog(
            onDismissRequest = { showManageDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Manage Project",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            project!!.title,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Verify/Unverify
                    ManagementActionCard(
                        icon = if (project!!.isOfficial) Icons.Default.Cancel else Icons.Default.Verified,
                        title = if (project!!.isOfficial) "Remove Verification" else "Verify Project",
                        description = if (project!!.isOfficial)
                            "Remove verified badge from this project"
                        else
                            "Mark this project as verified and official",
                        color = if (project!!.isOfficial) TextSecondary else SuccessGreen,
                        onClick = {
                            scope.launch {
                                if (currentAdminId == null) {
                                    snackbarMessage = "Admin ID not found"
                                    showSnackbar = true
                                    showManageDialog = false
                                    return@launch
                                }

                                isProcessing = true
                                showManageDialog = false

                                if (project!!.isOfficial) {
                                    repository.unverifyProject(
                                        projectId = project!!.id,
                                        adminId = currentAdminId!!,
                                        onSuccess = {
                                            // Reload project data
                                            repository.getProjectById(
                                                projectId = projectId,
                                                onSuccess = { proj ->
                                                    project = proj
                                                    isProcessing = false
                                                    snackbarMessage = "Project unverified successfully"
                                                    showSnackbar = true
                                                },
                                                onError = {
                                                    isProcessing = false
                                                }
                                            )
                                        },
                                        onError = { e ->
                                            isProcessing = false
                                            snackbarMessage = "Failed to unverify: ${e.message}"
                                            showSnackbar = true
                                        }
                                    )
                                } else {
                                    repository.verifyProject(
                                        projectId = project!!.id,
                                        adminId = currentAdminId!!,
                                        onSuccess = {

                                            repository.getProjectById(
                                                projectId = projectId,
                                                onSuccess = { proj ->
                                                    project = proj
                                                    isProcessing = false
                                                    snackbarMessage = "Project verified successfully"
                                                    showSnackbar = true
                                                },
                                                onError = {
                                                    isProcessing = false
                                                }
                                            )
                                        },
                                        onError = { e ->
                                            isProcessing = false
                                            snackbarMessage = "Failed to verify: ${e.message}"
                                            showSnackbar = true
                                        }
                                    )
                                }
                            }
                        }
                    )

                    // Flag/Unflag & Suspend/Reactivate
                    ManagementActionCard(
                        icon = if (project!!.status == "suspended") Icons.Default.CheckCircle else Icons.Default.Warning,
                        title = if (project!!.status == "suspended") "Reactivate Project" else "Suspend Project",
                        description = if (project!!.status == "suspended")
                            "Remove suspension and reactivate project"
                        else
                            "Flag and suspend this project",
                        color = if (project!!.status == "suspended") SuccessGreen else WarningOrange,
                        onClick = {
                            scope.launch {
                                if (currentAdminId == null) {
                                    snackbarMessage = "Admin ID not found"
                                    showSnackbar = true
                                    showManageDialog = false
                                    return@launch
                                }

                                isProcessing = true
                                showManageDialog = false

                                if (project!!.status == "suspended") {
                                    // Reactivate
                                    repository.dismissReport(
                                        projectId = project!!.id,
                                        adminId = currentAdminId!!,
                                        reportCount = 0,
                                        onSuccess = {
                                            // Also update status to active
                                            repository.updateProject(
                                                projectId = project!!.id,
                                                updates = mapOf("Status" to "active"),
                                                onSuccess = {
                                                    // Reload project data
                                                    repository.getProjectById(
                                                        projectId = projectId,
                                                        onSuccess = { proj ->
                                                            project = proj
                                                            isProcessing = false
                                                            snackbarMessage = "Project reactivated successfully"
                                                            showSnackbar = true
                                                        },
                                                        onError = {
                                                            isProcessing = false
                                                        }
                                                    )
                                                },
                                                onError = { e ->
                                                    isProcessing = false
                                                    snackbarMessage = "Failed to update status: ${e.message}"
                                                    showSnackbar = true
                                                }
                                            )
                                        },
                                        onError = { e ->
                                            isProcessing = false
                                            snackbarMessage = "Failed to reactivate: ${e.message}"
                                            showSnackbar = true
                                        }
                                    )
                                } else {
                                    // Suspend
                                    repository.suspendProject(
                                        projectId = project!!.id,
                                        adminId = currentAdminId!!,
                                        reportCount = 0,
                                        onSuccess = {
                                            // Reload project data
                                            repository.getProjectById(
                                                projectId = projectId,
                                                onSuccess = { proj ->
                                                    project = proj
                                                    isProcessing = false
                                                    snackbarMessage = "Project suspended successfully"
                                                    showSnackbar = true
                                                },
                                                onError = {
                                                    isProcessing = false
                                                }
                                            )
                                        },
                                        onError = { e ->
                                            isProcessing = false
                                            snackbarMessage = "Failed to suspend: ${e.message}"
                                            showSnackbar = true
                                        }
                                    )
                                }
                            }
                        }
                    )

                    // Delete Project
                    ManagementActionCard(
                        icon = Icons.Default.Delete,
                        title = "Delete Project",
                        description = "Permanently cancel this project",
                        color = ErrorRed,
                        onClick = {
                            scope.launch {
                                if (currentAdminId == null) {
                                    snackbarMessage = "Admin ID not found"
                                    showSnackbar = true
                                    showManageDialog = false
                                    return@launch
                                }

                                isProcessing = true
                                showManageDialog = false

                                repository.deleteProject(
                                    projectId = project!!.id,
                                    adminId = currentAdminId!!,
                                    reason = "Deleted by admin",
                                    onSuccess = {
                                        isProcessing = false
                                        snackbarMessage = "Project deleted successfully"
                                        showSnackbar = true
                                        navController.navigateUp()
                                    },
                                    onError = { e ->
                                        isProcessing = false
                                        snackbarMessage = "Failed to delete: ${e.message}"
                                        showSnackbar = true
                                    }
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showManageDialog = false }
                ) {
                    Text("Close", color = PrimaryBlue, fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}

@Composable
fun ManagementActionCard(
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
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

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}