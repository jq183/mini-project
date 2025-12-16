

package com.example.miniproject.UserScreen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.compose.rememberAsyncImagePainter
import com.example.miniproject.repository.ProjectRepository
import com.example.miniproject.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProjectPage(
    navController: NavController,
    projectId: String
) {
    val repository = remember { ProjectRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form States
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var goalAmount by remember { mutableStateOf("") }
    var currentImageUrl by remember { mutableStateOf("") }
    var newImageUri by remember { mutableStateOf<Uri?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    val categories = listOf("Technology", "Charity", "Education", "Medical", "Art", "Games")

    // Fetch initial data
    LaunchedEffect(projectId) {
        repository.getProjectById(
            projectId = projectId,
            onSuccess = { project ->
                title = project.title
                description = project.description
                category = project.category
                goalAmount = project.goalAmount.toString()
                currentImageUrl = project.ImageUrl
                isLoading = false
            },
            onError = {
                scope.launch { snackbarHostState.showSnackbar("Failed to load project") }
                isLoading = false
            }
        )
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> newImageUri = uri }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Project") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundWhite)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Image Picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.LightGray, RoundedCornerShape(10.dp))
                        .clickable { pickImageLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val painter = if (newImageUri != null) {
                        rememberAsyncImagePainter(newImageUri)
                    } else {
                        rememberAsyncImagePainter(currentImageUrl)
                    }

                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    ) {
                        Text(
                            "Tap to change image",
                            color = Color.White,
                            modifier = Modifier.padding(4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))


                OutlinedTextField(
                    value = title,
                    onValueChange = {},
                    label = { Text("Project Topic (Cannot be changed)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = BorderGray)
                )

                Spacer(Modifier.height(12.dp))

                // Category Dropdown
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Category: $category", color = TextPrimary)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = { category = item; expanded = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )

                Spacer(Modifier.height(12.dp))

                // Goal Amount
                OutlinedTextField(
                    value = goalAmount,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) goalAmount = it },
                    label = { Text("Target Amount (RM)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        isSaving = true
                        val updates = mutableMapOf<String, Any>(
                            "Description" to description,
                            "Category" to category,
                            "Target_Amount" to (goalAmount.toDoubleOrNull() ?: 0.0)
                        )

                        repository.updateProjectWithImage(
                            projectId = projectId,
                            updates = updates,
                            newImageUri = newImageUri,
                            onSuccess = {
                                isSaving = false
                                navController.popBackStack()
                            },
                            onError = { e ->
                                isSaving = false
                                scope.launch { snackbarHostState.showSnackbar("Update failed: ${e.message}") }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Save Changes")
                }
            }
        }
    }
}