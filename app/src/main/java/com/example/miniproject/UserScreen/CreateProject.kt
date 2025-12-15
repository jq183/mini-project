package com.example.miniproject.UserScreen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.miniproject.repository.ProjectRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem


@Composable
fun CreateProjectPage(
    navigator: NavController
) {

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    if (currentUser == null) {
        return
    }

    val currentUserId = currentUser.uid
    val currentUserName = currentUser.displayName ?: "Unknown Creator"

    val context = LocalContext.current
    val repository = ProjectRepository()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var goalAmount by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var dueDate by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()


    val categories = listOf("Technology", "Charity", "Education", "Medical", "Art", "Games")

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> imageUri = uri }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create a Project", style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(16.dp))

            // IMAGE PICKER BOX
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.LightGray, RoundedCornerShape(10.dp))
                    .clickable { pickImageLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri == null) {
                    Text("+ Add Image")
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Category dropdown
            var expanded by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // 可选：添加背景或边框来突出显示选择区域
                    .wrapContentSize(Alignment.TopStart)
            ) {
                // 1. 菜单锚点 (使用 Button 或 Text)
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 显示当前选定的分类，如果为空则显示默认文本
                    Text(category.ifEmpty { "Select Category" })
                }

                // 2. 下拉菜单
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                category = item // 更新选择值
                                expanded = false // 关闭菜单
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Goal amount
            OutlinedTextField(
                value = goalAmount,
                onValueChange = { goalAmount = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Target Amount") },
                modifier = Modifier.fillMaxWidth()
            )


            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = dueDate,
                onValueChange = { dueDate = it },
                label = { Text("Target Due Date (e.g., 2026-06-30)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // SUBMIT BUTTON
            Button(
                onClick = {
                    if (title.isEmpty() || description.isEmpty() || category.isEmpty() ||
                        goalAmount.toDoubleOrNull() == null || imageUri == null || dueDate.isEmpty()
                    ) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please fill all fields and select an image.", withDismissAction = true)
                        }
                        return@Button
                    }

                    isLoading = true

                    val project = Project(
                        title = title,
                        description = description,
                        category = category,
                        goalAmount = goalAmount.toDouble(),
                        creatorId = currentUserId,
                        creatorName = currentUserName,
                        dueDate = dueDate
                    )

                    repository.uploadImageAndCreateProject(
                        imageUri!!,
                        project,
                        onSuccess = {
                            isLoading = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Project created successfully!", withDismissAction = true)
                            }
                            navigator.popBackStack()
                        },
                        onError = { e ->
                            isLoading = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Error creating project: ${e.localizedMessage}", withDismissAction = true)
                            }
                        }
                    )

                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Add Project")
                }
            }
        }
    }
}