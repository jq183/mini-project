package com.example.miniproject.UserScreen

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.miniproject.repository.ProjectRepository
import com.example.miniproject.ui.theme.* // 假设您的主题颜色在这里
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

// ----------------------------------------------------
// ProjectDetailPage 核心实现
// ----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailPage(
    navController: NavController,
    projectId: String // 从导航参数接收项目ID
) {
    val repository = remember { ProjectRepository() }

    // 状态管理：存储项目数据、加载状态和错误
    var project by remember { mutableStateOf<Project?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCertifiedTips by remember { mutableStateOf(false) } // 用于 Verified Icon 提示

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
            // 使用 CenterAlignedTopAppBar 确保标题居中
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
                                    "${Uri.encode(project?.title)}/" +
                                    "${project?.currentAmount}/" +
                                    "${project?.goalAmount}/" +
                                    "${project?.backers}/" +
                                    "${project?.createdAt?.seconds ?: 0}"
                        )
                    }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Analytics")
                    }

                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        },
        bottomBar = {
            // 底部捐赠按钮 (根据截图样式)
            if (project != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundWhite)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { /* TODO: Navigate to Donation Screen */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !project!!.isComplete // 项目完成后禁用
                    ) {
                        Text("Donate Now", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
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

// ----------------------------------------------------
// Project 详情内容 Composable
// ----------------------------------------------------
@Composable
fun ProjectDetailContent(
    project: Project,
    paddingValues: PaddingValues,
    showCertifiedTips: Boolean,
    onVerifiedIconClick: () -> Unit,
    navController: NavController
) {
    val progress = (project.currentAmount / project.goalAmount).toFloat().coerceIn(0f, 1f)

    // 使用 Box 容纳所有内容，以便将 Verified 提示浮动在卡片之上
    Box(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundGray)
        ) {
            // 1. 顶部图片/Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(TextSecondary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    // 实际项目中应使用 Coil 或 Glide 加载 project.imageUrl
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Project Image",
                        modifier = Modifier.size(100.dp),
                        tint = TextSecondary.copy(alpha = 0.5f)
                    )
                    // 假设图片是重要的，这里是占位符
                    /*
                    Image(
                        painter = rememberAsyncImagePainter(model = project.imageUrl),
                        contentDescription = project.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    */

                    // Analytics Badge (根据截图)
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

            // 2. 项目标题和进度
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundWhite)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = project.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 进度条
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = PrimaryBlue,
                        trackColor = BorderGray,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 进度数字 / 目标金额
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "RM${String.format("%.0f", project.currentAmount)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "/ RM${String.format("%.0f", project.goalAmount)}",
                            fontSize = 16.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${(progress * 100).toInt()}% Funded",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { navController.navigate("reportProject/${project.id}") }) {
                            Icon(
                                imageVector = Icons.Default.Report,
                                contentDescription = "Report Project",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 3. 项目详细描述
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundWhite)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Project Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = project.description,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp // 提高可读性
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 其他项目信息（创建时间, 状态）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val formattedDate = project.createdAt?.let {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.toDate())
                        } ?: "N/A"
                        Text(
                            text = "Created at: $formattedDate",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        // 警告标志
                        if (project.isWarning) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = WarningOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 4. 创建者信息和认证
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundWhite)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Creator",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = project.creatorName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = project.category,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Verified Tip (浮动在顶部)
        if (showCertifiedTips && project.isOfficial) {
            Surface(
                modifier = Modifier
                    .padding(top = paddingValues.calculateTopPadding() + 8.dp, end = 20.dp)
                    .width(160.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onVerifiedIconClick()
                    },
                color = BackgroundWhite,
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Certified by SparkFund",
                        fontSize = 11.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}