package com.example.miniproject.AdminScreen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.repository.AdminRepository
import com.example.miniproject.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AdminProfile(
    val email: String = "",
    val name: String = "Admin",
)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardPage(navController: NavController) {
    var stats by remember { mutableStateOf(DashboardStats()) }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var adminProfile by remember { mutableStateOf(AdminProfile()) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var isChangingPassword by remember { mutableStateOf(false) }
    var showOldPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    val currentRoute = navController.currentBackStackEntry?.destination?.route
    val repository = remember { AdminRepository() }
    val db = remember { FirebaseFirestore.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                // Get current admin info
                val admin = repository.getCurrentAdmin()
                if (admin != null) {
                    adminProfile = AdminProfile(
                        email = admin.email,
                        name = admin.username,
                    )
                }

                // Fetch projects data
                val projectsSnapshot = db.collection("projects").get().await()
                val allProjects = projectsSnapshot.documents

                val activeProjects = allProjects.filter {
                    it.getString("Status") == "active"
                }.size

                val certifiedProjects = allProjects.filter {
                    it.getBoolean("isOfficial") == true
                }.size

                val flaggedProjects = allProjects.filter {
                    it.getBoolean("isWarning") == true
                }.size

                // Calculate total funding
                val totalFunding = allProjects.sumOf {
                    it.getDouble("Current_Amount") ?: 0.0
                }

                // Fetch reports data
                val reportsSnapshot = db.collection("Reports").get().await()
                val allReports = reportsSnapshot.documents

                val pendingReports = allReports.filter {
                    it.getString("Status") == "pending"
                }.size

                val resolvedReports = allReports.filter {
                    it.getString("Status") == "Resolved"
                }.size

                stats = DashboardStats(
                    totalProjects = allProjects.size,
                    activeProjects = activeProjects,
                    certifiedProjects = certifiedProjects,
                    flaggedProjects = flaggedProjects,
                    totalReports = allReports.size,
                    pendingReports = pendingReports,
                    resolvedReports = resolvedReports,
                    totalFunding = totalFunding
                )

                isLoading = false
            } catch (e: Exception) {
                println("Error fetching dashboard data: ${e.message}")
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Dashboard",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = ErrorRed
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
                item {
                    // Admin Profile Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = BackgroundWhite
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            PrimaryBlue.copy(alpha = 0.1f),
                                            InfoBlue.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Admin Avatar
                            Surface(
                                shape = CircleShape,
                                color = PrimaryBlue,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Admin",
                                        modifier = Modifier.size(32.dp),
                                        tint = BackgroundWhite
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = adminProfile.name,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = TextSecondary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = adminProfile.email,
                                                fontSize = 13.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PrimaryBlue.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "ADMIN",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Change Password Button
                                OutlinedButton(
                                    onClick = { showChangePasswordDialog = true },  // Changed this line
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = PrimaryBlue
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryBlue)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Change Password", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }

                            }
                        }
                    }
                }

                // Quick Stats Summary Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Quick Overview",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                TextButton(
                                    onClick = { navController.navigate("adminRanking") },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = PrimaryBlue
                                    )
                                ) {
                                    Text(
                                        "View All",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                QuickStatItem(
                                    value = stats.pendingReports.toString(),
                                    label = "Pending Reports",
                                    icon = Icons.Default.HourglassEmpty,
                                    color = WarningOrange
                                )
                                VerticalDivider(
                                    modifier = Modifier.height(50.dp),
                                    thickness = 1.dp,
                                    color = BorderGray
                                )
                                QuickStatItem(
                                    value = stats.activeProjects.toString(),
                                    label = "Active Projects",
                                    icon = Icons.Default.TrendingUp,
                                    color = SuccessGreen
                                )
                                VerticalDivider(
                                    modifier = Modifier.height(50.dp),
                                    thickness = 1.dp,
                                    color = BorderGray
                                )
                                QuickStatItem(
                                    value = stats.flaggedProjects.toString(),
                                    label = "Flagged",
                                    icon = Icons.Default.Flag,
                                    color = ErrorRed
                                )
                            }
                        }
                    }
                }

                // Projects Overview Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Projects Overview",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EnhancedStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Total",
                            value = stats.totalProjects.toString(),
                            icon = Icons.Default.Folder,
                            color = PrimaryBlue,
                            showProgress = false
                        )
                        EnhancedStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Active",
                            value = stats.activeProjects.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = SuccessGreen,
                            percentage = if (stats.totalProjects > 0)
                                (stats.activeProjects.toFloat() / stats.totalProjects * 100).toInt()
                            else 0
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EnhancedStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Certified",
                            value = stats.certifiedProjects.toString(),
                            icon = Icons.Default.Verified,
                            color = InfoBlue,
                            percentage = if (stats.totalProjects > 0)
                                (stats.certifiedProjects.toFloat() / stats.totalProjects * 100).toInt()
                            else 0
                        )
                        EnhancedStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Flagged",
                            value = stats.flaggedProjects.toString(),
                            icon = Icons.Default.Warning,
                            color = WarningOrange,
                            percentage = if (stats.totalProjects > 0)
                                (stats.flaggedProjects.toFloat() / stats.totalProjects * 100).toInt()
                            else 0
                        )
                    }
                }

                // Reports Overview Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Reports Overview",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EnhancedStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Total",
                            value = stats.totalReports.toString(),
                            icon = Icons.Default.Report,
                            color = ErrorRed,
                            showProgress = false
                        )
                        EnhancedStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Pending",
                            value = stats.pendingReports.toString(),
                            icon = Icons.Default.HourglassEmpty,
                            color = WarningOrange,
                            percentage = if (stats.totalReports > 0)
                                (stats.pendingReports.toFloat() / stats.totalReports * 100).toInt()
                            else 0
                        )
                    }
                }

                // Funding Stats
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            SuccessGreen.copy(alpha = 0.15f),
                                            RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = "Funding",
                                        modifier = Modifier.size(28.dp),
                                        tint = SuccessGreen
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        "Total Platform Funding",
                                        fontSize = 14.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "RM ${String.format("%,.0f", stats.totalFunding)}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                }
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

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Logout",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to logout from admin panel?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isChangingPassword) {
                    showChangePasswordDialog = false
                    oldPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                    passwordError = ""
                    showOldPassword = false
                    showNewPassword = false
                    showConfirmPassword = false
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Change Password",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Old Password
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = {
                            oldPassword = it
                            passwordError = ""
                        },
                        label = { Text("Current Password") },
                        visualTransformation = if (showOldPassword)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showOldPassword = !showOldPassword }) {
                                Icon(
                                    imageVector = if (showOldPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showOldPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isChangingPassword
                    )

                    // New Password
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            passwordError = ""
                        },
                        label = { Text("New Password") },
                        visualTransformation = if (showNewPassword)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                Icon(
                                    imageVector = if (showNewPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showNewPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isChangingPassword,
                        supportingText = {
                            Text("At least 6 characters", fontSize = 11.sp)
                        }
                    )

                    // Confirm Password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            passwordError = ""
                        },
                        label = { Text("Confirm New Password") },
                        visualTransformation = if (showConfirmPassword)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    imageVector = if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isChangingPassword
                    )

                    if (passwordError.isNotEmpty()) {
                        Text(
                            text = passwordError,
                            color = ErrorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                                passwordError = "Please fill in all fields"
                            }
                            newPassword.length < 6 -> {
                                passwordError = "New password must be at least 6 characters"
                            }
                            newPassword != confirmPassword -> {
                                passwordError = "New passwords do not match"
                            }
                            oldPassword == newPassword -> {
                                passwordError = "New password must be different from current password"
                            }
                            else -> {
                                isChangingPassword = true
                                coroutineScope.launch {
                                    try {
                                        // Verify old password by re-authenticating
                                        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                        if (user != null && user.email != null) {
                                            val credential = com.google.firebase.auth.EmailAuthProvider
                                                .getCredential(user.email!!, oldPassword)

                                            user.reauthenticate(credential).await()

                                            // Change password
                                            repository.changePassword(newPassword).fold(
                                                onSuccess = {
                                                    isChangingPassword = false
                                                    showChangePasswordDialog = false
                                                    oldPassword = ""
                                                    newPassword = ""
                                                    confirmPassword = ""
                                                    passwordError = ""
                                                    showOldPassword = false
                                                    showNewPassword = false
                                                    showConfirmPassword = false
                                                    // Show success message (you can add a Snackbar here)
                                                },
                                                onFailure = { e ->
                                                    isChangingPassword = false
                                                    passwordError = "Failed to change password: ${e.message}"
                                                }
                                            )
                                        }
                                    } catch (e: Exception) {
                                        isChangingPassword = false
                                        passwordError = "Current password is incorrect"
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    enabled = !isChangingPassword
                ) {
                    if (isChangingPassword) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = BackgroundWhite,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isChangingPassword) "Changing..." else "Change Password")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showChangePasswordDialog = false
                        oldPassword = ""
                        newPassword = ""
                        confirmPassword = ""
                        passwordError = ""
                        showOldPassword = false
                        showNewPassword = false
                        showConfirmPassword = false
                    },
                    enabled = !isChangingPassword
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun QuickStatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EnhancedStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    percentage: Int? = null,
    showProgress: Boolean = true
) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        tint = color
                    )
                }

                if (percentage != null && showProgress) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = color.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "$percentage%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}