package com.example.miniproject.UserScreen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.miniproject.repository.ProjectRepository
import com.example.miniproject.ui.theme.BackgroundGray
import com.example.miniproject.ui.theme.BackgroundWhite
import com.example.miniproject.ui.theme.BorderGray
import com.example.miniproject.ui.theme.ErrorRed
import com.example.miniproject.ui.theme.PrimaryBlue
import com.example.miniproject.ui.theme.TextPrimary
import com.example.miniproject.ui.theme.TextSecondary
import com.example.miniproject.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailPage(
    navController: NavController,
    projectId: String
) {
    val repository = remember { ProjectRepository() }

    var project by remember { mutableStateOf<Project?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCertifiedTips by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        if (projectId.isNotEmpty()) {
            repository.getProjectById(
                projectId = projectId,
                onSuccess = { proj ->
                    project = proj
                    isLoading = false
                },
                onError = { exception ->
                    errorMessage = exception.message
                    isLoading = false
                }
            )
        } else {
            errorMessage = "Project ID is missing."
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Project Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(
                            "projectAnalytics/" +
                                    "${project?.id}/" +
                                    "${project?.title}/" +
                                    "${project?.currentAmount}/" +
                                    "${project?.goalAmount}/" +
                                    "${project?.backers}/" +
                                    "${project?.createdAt?.seconds ?: 0L}"
                        )
                    }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Analytics")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        },
        bottomBar = {
            if (project != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundWhite)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            // ENCODE the image URL to safely pass it through navigation
                            val encodedUrl = if (project!!.ImageUrl.isNotEmpty()) {
                                Uri.encode(project!!.ImageUrl)
                            } else {
                                "placeholder"
                            }

                            // Pass ID, Title, and Image URL
                            navController.navigate("supportPage/${project!!.id}/${project!!.title}/$encodedUrl")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !project!!.isComplete
                    ) {
                        Text("Donate Now", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }
            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("Error loading project: $errorMessage", color = ErrorRed)
                }
            }
            project != null -> {
                ProjectDetailContent(
                    project = project!!,
                    paddingValues = paddingValues,
                    showCertifiedTips = showCertifiedTips,
                    onVerifiedIconClick = { showCertifiedTips = !showCertifiedTips },
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun ProjectDetailContent(
    project: Project,
    paddingValues: PaddingValues,
    showCertifiedTips: Boolean,
    onVerifiedIconClick: () -> Unit,
    navController: NavController
) {
    val progress = (project.currentAmount / project.goalAmount).toFloat().coerceIn(0f, 1f)
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundGray)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(TextSecondary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    // FIXED: Using AsyncImage to display the actual project image
                    if (project.ImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(project.ImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = project.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Placeholder",
                            modifier = Modifier.size(100.dp),
                            tint = TextSecondary.copy(alpha = 0.5f)
                        )
                    }

                    if (project.status != "cancelled") {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            color = PrimaryBlue,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                text = project.status.uppercase(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundWhite)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(text = project.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(6.dp)),
                        color = PrimaryBlue,
                        trackColor = BorderGray,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(text = "RM${String.format("%.0f", project.currentAmount)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        Text(text = "/ RM${String.format("%.0f", project.goalAmount)}", fontSize = 16.sp, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${(progress * 100).toInt()}% Funded", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { navController.navigate("reportProject/${project.id}") }) {
                            Icon(Icons.Default.Report, contentDescription = "Report", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth().background(BackgroundWhite).padding(16.dp)) {
                    Text(text = "Project Details", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = project.description, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val formattedDate = project.createdAt?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.toDate()) } ?: "N/A"
                        Text(text = "Created at: $formattedDate", fontSize = 13.sp, color = TextSecondary)
                        if (project.isWarning) Icon(Icons.Default.Warning, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().background(BackgroundWhite).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = project.creatorName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            if (project.isOfficial) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null // Standard Material 3 click handling
                                        ) { onVerifiedIconClick() },
                                    tint = PrimaryBlue
                                )
                            }
                        }
                        Text(text = project.category, fontSize = 13.sp, color = TextSecondary)
                    }
                }
            }
        }

        if (showCertifiedTips && project.isOfficial) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = paddingValues.calculateTopPadding() + 8.dp, end = 20.dp)
                    .width(160.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onVerifiedIconClick() },
                color = BackgroundWhite,
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(text = "Certified by SparkFund", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}