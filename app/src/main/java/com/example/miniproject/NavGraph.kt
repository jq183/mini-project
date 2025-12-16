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
import com.example.miniproject.Payment.OnlinePage
import com.example.miniproject.Payment.PaymentOption
import com.example.miniproject.Payment.PaymentSuccess
import com.example.miniproject.Payment.TngPage
import com.example.miniproject.Payment.TopUpPage
import com.example.miniproject.Payment.WalletPage
import com.example.miniproject.SignUpScreen.SignUpEmailPage
import com.example.miniproject.SignUpScreen.SignUpProfilePage
import com.example.miniproject.UserScreen.CreateProjectPage
import com.example.miniproject.UserScreen.EditProjectPage
import com.example.miniproject.UserScreen.MainPage
import com.example.miniproject.UserScreen.MyProjectsPage
import com.example.miniproject.UserScreen.ProfileScreen.ChangeEmailPage
import com.example.miniproject.UserScreen.ProfileScreen.ChangePwPage
import com.example.miniproject.UserScreen.ProfileScreen.FAQPage
import com.example.miniproject.UserScreen.ProfileScreen.ProfilePage
import com.example.miniproject.UserScreen.ProjectAnalyticsPage
import com.example.miniproject.UserScreen.ProjectDetailPage
import com.example.miniproject.UserScreen.ReportProjectPage
import com.example.miniproject.UserScreen.SupportPage
import com.example.miniproject.admin.AdminActionsPage
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
        composable("signUpEmail") { SignUpEmailPage(navController) }
        composable("signUpProfile/{email}/{isGoogleSignUp}") { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val isGoogleSignUp = backStackEntry.arguments?.getString("isGoogleSignUp")?.toBoolean() ?: false
            SignUpProfilePage(navController, email, isGoogleSignUp)
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
        composable(
            route = "projectAnalytics/{projectId}/{title}/{currentAmount}/{goalAmount}/{backers}/{createdAt}",
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("currentAmount") { type = NavType.FloatType },
                navArgument("goalAmount") { type = NavType.FloatType },
                navArgument("backers") { type = NavType.IntType },
                navArgument("createdAt") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            ProjectAnalyticsPage(
                navController = navController,
                projectId = backStackEntry.arguments?.getString("projectId") ?: "",
                title = backStackEntry.arguments?.getString("title") ?: "",
                currentAmount = backStackEntry.arguments?.getFloat("currentAmount") ?: 0f,
                goalAmount = backStackEntry.arguments?.getFloat("goalAmount") ?: 0f,
                backers = backStackEntry.arguments?.getInt("backers") ?: 0,
                createdAt = backStackEntry.arguments?.getLong("createdAt") ?: 0L
            )
        }
        composable("reportProject/{projectId}") { backStackEntry ->
            ReportProjectPage(
                navController = navController,
                projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            )
        }
        composable ("createProject"){
            CreateProjectPage(navController)
        }
        composable ("editProject/{projectId}"){
            EditProjectPage(
                navController,
                projectId = it.arguments?.getString("projectId") ?: ""
            )
        }

        composable ("changePw"){
            ChangePwPage(navController)
        }

        composable ("changeEmail"){
            ChangeEmailPage(navController)
        }

        composable ("faq"){
            FAQPage(navController)
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

        composable("adminRanking") {
            AdminActionsPage(navController = navController)
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

        composable(
            route = "topUpPage/{fromPayment}",
            arguments = listOf(navArgument("fromPayment") { type = NavType.BoolType })
        ) { backStackEntry ->
            val fromPayment = backStackEntry.arguments?.getBoolean("fromPayment") ?: false
            TopUpPage(navController, isFromPaymentFlow = fromPayment)
        }

        composable(
            route = "onlinePage/{amount}/{projectId}",
            arguments = listOf(
                navArgument("amount") { type = NavType.StringType },
                navArgument("projectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val amountString = backStackEntry.arguments?.getString("amount") ?: "0.00"
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val amountDouble = amountString.toDoubleOrNull() ?: 10.00

            OnlinePage(
                navController = navController,
                paymentAmount = amountDouble,
                projectId = projectId
            )
        }

        composable(
            route = "paymentOption/{amount}/{projectId}",
            arguments = listOf(
                navArgument("amount") { type = NavType.StringType }, // passed as string in url
                navArgument("projectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val amt = backStackEntry.arguments?.getString("amount")?.toDoubleOrNull() ?: 0.0
            val pId = backStackEntry.arguments?.getString("projectId") ?: ""
            PaymentOption(navController, amt, pId)
        }

        composable(
            route = "paymentSuccess/{amount}/{method}", // Define the route with 2 arguments
            arguments = listOf(
                navArgument("amount") { type = NavType.StringType },
                navArgument("method") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // 1. Extract the arguments passed from the previous screen
            val amountString = backStackEntry.arguments?.getString("amount") ?: "0.00"
            val method = backStackEntry.arguments?.getString("method") ?: "Unknown"

            // 2. Convert amount to Double
            val amount = amountString.toDoubleOrNull() ?: 0.00

            // 3. Launch the Page
            PaymentSuccess(
                navController = navController,
                paymentAmount = amount,
                paymentMethod = method
            )
        }

        composable(
            route = "supportPage/{projectId}/{title}/{imageUrl}",
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("imageUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pId = backStackEntry.arguments?.getString("projectId") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val url = backStackEntry.arguments?.getString("imageUrl") ?: ""
            SupportPage(navController, pId, title, url)
        }

        composable(
            route = "tngPage/{amount}/{projectId}",
            arguments = listOf(
                navArgument("amount") { type = NavType.StringType },
                navArgument("projectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val amountString = backStackEntry.arguments?.getString("amount") ?: "10.00"
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""

            TngPage(
                navController = navController,
                amount = amountString,
                projectId = projectId
            )
        }


        composable(
            route = "walletPage/{amount}/{projectId}",
            arguments = listOf(
                navArgument("amount") { type = NavType.StringType },
                navArgument("projectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val amountString = backStackEntry.arguments?.getString("amount") ?: "0.00"
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val amountDouble = amountString.toDoubleOrNull() ?: 10.00

            WalletPage(
                navController = navController,
                paymentAmount = amountDouble,
                projectId = projectId
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