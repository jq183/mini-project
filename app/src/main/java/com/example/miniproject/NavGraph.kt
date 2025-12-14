package com.example.miniproject

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.miniproject.AdminScreen.AdminDashboardPage
import com.example.miniproject.AdminScreen.AdminHistoryPage
import com.example.miniproject.AdminScreen.AdminMainPage
import com.example.miniproject.AdminScreen.AdminProjectDetail
import com.example.miniproject.AdminScreen.AdminReportDetailPage
import com.example.miniproject.AdminScreen.AdminReportsPage
import com.example.miniproject.LoginScreen.ResetPwPage
import com.example.miniproject.LoginScreen.SignUpPage
import com.example.miniproject.UserScreen.CreateProjectPage
import com.example.miniproject.UserScreen.ProfileScreen.ChangeEmailPage
import com.example.miniproject.UserScreen.ProfileScreen.ChangePwPage
import com.example.miniproject.UserScreen.MainPage
import com.example.miniproject.UserScreen.MyProjectsPage
import com.example.miniproject.UserScreen.ProfileScreen.ProfilePage
import com.example.miniproject.UserScreen.ProjectDetailPage
import com.example.miniproject.admin.AdminLogin
import com.example.miniproject.admin.ChangePasswordScreen
import com.example.miniproject.ui.theme.BackgroundWhite
import com.example.miniproject.ui.theme.PrimaryBlue
import com.example.miniproject.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val isAdmin = currentUser?.email?.endsWith("@js.com") == true ||
            currentUser?.email?.endsWith("@sp.com") == true

    val startDestination = if (currentUser != null && !isAdmin) "mainPage" else "login"

    LaunchedEffect(currentUser) { }
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginPage(
                navController
            )
        }
        composable("signUp") {
            SignUpPage(
                onBackToLogin = { navController.popBackStack() },
                navController
            )
        }

        composable("mainPage") {
            MainPage(navController)
        }

        composable("profile") {
            ProfilePage(navController)
        }

        composable("resetPw") {
            ResetPwPage(navController)
        }

        composable  ("myProject"){
            MyProjectsPage(navController)
        }

        composable("projectDetail/{projectId}") { backStackEntry ->
            ProjectDetailPage(
                navController = navController,
                projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            )
        }
        composable ("createProject"){
            CreateProjectPage(navController)
        }
        composable ("editProfile"){
            EditProfilePage(navController)
        }

        composable ("changePw"){
            ChangePwPage(navController)
        }

        composable ("changeEmail"){
            ChangeEmailPage(navController)
        }
        //----admin------
        composable("admin login") {
            AdminLogin(navController)
        }

        composable("changePassword") {
            ChangePasswordScreen(navController)
        }

        composable ("adminMainPage"){
            AdminMainPage(navController)

        }

        composable("adminReports"){
            AdminReportsPage(navController)
        }
        composable("adminHistory"){
            AdminHistoryPage(navController)
        }
        composable("adminDashboard"){
            AdminDashboardPage(navController)
        }

        composable("adminProjectDetail/{projectId}") { backStackEntry ->
            AdminProjectDetail(
                navController = navController,
                projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            )
        }

        composable(
            route = "adminReportDetail/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            AdminReportDetailPage(
                navController = navController,
                projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            )
        }




    }
}
@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String?
) {
    Surface(
        color = BackgroundWhite,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularNavItem(
                icon = Icons.Default.Home,
                label = "Home",
                selected = currentRoute == "mainPage",
                onClick = {
                    if (currentRoute != "mainPage") {
                        navController.navigate("mainPage") {
                            popUpTo("mainPage") { inclusive = true }
                        }
                    }
                }
            )

            CircularNavItem(
                icon = Icons.Default.Folder,
                label = "My Projects",
                selected = currentRoute == "myProject",
                onClick = {
                    if (currentRoute != "myProject") {
                        navController.navigate("myProject")
                    }
                }
            )

            CircularNavItem(
                icon = Icons.Default.Person,
                label = "Profile",
                selected = currentRoute == "profile",
                onClick = {
                    if (currentRoute != "profile") {
                        navController.navigate("profile")
                    }
                }
            )
        }
    }
}

@Composable
fun CircularNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .clickable(
                    onClick = onClick,
                    indication = ripple(bounded = true, color = PrimaryBlue),
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) PrimaryBlue else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            color = if (selected) PrimaryBlue else TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.offset(y = (-8).dp)

        )
    }
}