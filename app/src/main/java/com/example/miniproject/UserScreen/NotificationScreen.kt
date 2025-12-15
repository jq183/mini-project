package com.example.miniproject.UserScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val reportId: String = "",
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null,
    val actionType: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPage(navController: NavController) {
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (currentUserId.isEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }

        try {
            val snapshot = db.collection("Notifications")
                .whereEqualTo("userId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            notifications = snapshot.documents.mapNotNull { doc ->
                try {
                    Notification(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        type = doc.getString("type") ?: "",
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        projectId = doc.getString("projectId") ?: "",
                        projectTitle = doc.getString("projectTitle") ?: "",
                        reportId = doc.getString("reportId") ?: "",
                        isRead = doc.getBoolean("isRead") ?: false,
                        createdAt = doc.getTimestamp("createdAt"),
                        actionType = doc.getString("actionType") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
                        fontSize = 24.sp,
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
                actions = {
                    if (notifications.any { !it.isRead }) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    notifications.filter { !it.isRead }.forEach { notification ->
                                        db.collection("Notifications")
                                            .document(notification.id)
                                            .update("isRead", true)
                                            .await()
                                    }
                                    // Refresh
                                    val snapshot = db.collection("Notifications")
                                        .whereEqualTo("userId", currentUserId)
                                        .orderBy("createdAt", Query.Direction.DESCENDING)
                                        .get()
                                        .await()

                                    notifications = snapshot.documents.mapNotNull { doc ->
                                        try {
                                            Notification(
                                                id = doc.id,
                                                userId = doc.getString("userId") ?: "",
                                                type = doc.getString("type") ?: "",
                                                title = doc.getString("title") ?: "",
                                                message = doc.getString("message") ?: "",
                                                projectId = doc.getString("projectId") ?: "",
                                                projectTitle = doc.getString("projectTitle") ?: "",
                                                reportId = doc.getString("reportId") ?: "",
                                                isRead = doc.getBoolean("isRead") ?: false,
                                                createdAt = doc.getTimestamp("createdAt"),
                                                actionType = doc.getString("actionType") ?: ""
                                            )
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Mark All Read", color = PrimaryBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundWhite
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: $errorMessage",
                        color = ErrorRed
                    )
                }
            }
            notifications.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextSecondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No notifications yet",
                            fontSize = 16.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundGray)
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = {
                                scope.launch {
                                    if (!notification.isRead) {
                                        db.collection("Notifications")
                                            .document(notification.id)
                                            .update("isRead", true)
                                            .await()
                                    }

                                    if (notification.projectId.isNotEmpty()) {
                                        navController.navigate("projectDetail/${notification.projectId}")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val notificationDate = notification.createdAt?.toDate()?.let {
        dateFormat.format(it)
    } ?: "Unknown"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                BackgroundWhite
            else
                PrimaryBlue.copy(alpha = 0.05f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = when (notification.actionType) {
                    "resolve" -> WarningOrange.copy(alpha = 0.1f)
                    "dismiss" -> TextSecondary.copy(alpha = 0.1f)
                    else -> PrimaryBlue.copy(alpha = 0.1f)
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (notification.actionType) {
                            "resolve" -> Icons.Default.CheckCircle
                            "dismiss" -> Icons.Default.Cancel
                            else -> Icons.Default.Notifications
                        },
                        contentDescription = null,
                        tint = when (notification.actionType) {
                            "resolve" -> WarningOrange
                            "dismiss" -> TextSecondary
                            else -> PrimaryBlue
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(PrimaryBlue, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notificationDate,
                        fontSize = 12.sp,
                        color = TextLight
                    )

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}