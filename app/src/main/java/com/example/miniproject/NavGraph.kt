package com.example.miniproject

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.miniproject.AdminScreen.AdminDashboardPage
import com.example.miniproject.AdminScreen.AdminHistoryPage
import com.example.miniproject.AdminScreen.AdminMainPage
import com.example.miniproject.AdminScreen.AdminReportsPage
import com.example.miniproject.LoginScreen.ResetPwPage
import com.example.miniproject.LoginScreen.SignUpPage
import com.example.miniproject.UserScreen.MainPage
import com.example.miniproject.UserScreen.ProfilePage
import com.example.miniproject.admin.AdminLogin
import com.example.miniproject.ui.theme.BackgroundWhite
import com.example.miniproject.ui.theme.PrimaryBlue
import com.example.miniproject.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val startDestination = if (currentUser != null) "mainPage" else "login"

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
        //----admin------
        composable("admin login") {
            AdminLogin(navController)
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


    }
}
@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String?
) {
    NavigationBar(
        containerColor = BackgroundWhite,
        tonalElevation = 8.dp
    ) {
        // Home
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") },
            selected = currentRoute == "mainPage",
            onClick = {
                if (currentRoute != "mainPage") {
                    navController.navigate("mainPage") {
                        popUpTo("mainPage") { inclusive = true }
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = Color.Transparent
            )
        )

        // My Projects
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = "My Projects"
                )
            },
            label = { Text("My Projects") },
            selected = currentRoute == "myProjects",
            onClick = {
                if (currentRoute != "myProjects") {
                    navController.navigate("myProjects")
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = Color.Transparent
            )
        )

        // Profile
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile"
                )
            },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = {
                if (currentRoute != "profile") {
                    navController.navigate("profile")
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = Color.Transparent
            )
        )
    }
}