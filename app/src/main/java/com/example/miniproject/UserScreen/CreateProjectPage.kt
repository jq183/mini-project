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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem

// 新增导入用于日期选择器和时间戳
import android.app.DatePickerDialog
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.Timestamp

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
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // --- DUE DATE STATES AND PICKER ---
    val todayCalendar = remember { Calendar.getInstance() }
    val initialDate = remember { todayCalendar.timeInMillis + (1000 * 60 * 60 * 24 * 30L) }
    var selectedDateMillis by remember { mutableStateOf(initialDate) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var selectedDateText by remember { mutableStateOf(dateFormatter.format(Date(initialDate))) }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0) // 设置日期，时间清零
                set(Calendar.MILLISECOND, 0)
            }
            selectedDateMillis = calendar.timeInMillis
            selectedDateText = dateFormatter.format(Date(selectedDateMillis))
        },
        todayCalendar.get(Calendar.YEAR),
        todayCalendar.get(Calendar.MONTH),
        todayCalendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = todayCalendar.timeInMillis
    }
    // --- END DUE DATE ---


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
                    .wrapContentSize(Alignment.TopStart)
            ) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(category.ifEmpty { "Select Category" })
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                category = item
                                expanded = false
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

            // --- DUE DATE PICKER UI ---
            OutlinedButton(
                onClick = { datePickerDialog.show() },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Due Date: $selectedDateText",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Date"
                    )
                }
            }
            // --- END DUE DATE PICKER UI ---

            Spacer(Modifier.height(16.dp))

            // SUBMIT BUTTON
            Button(
                onClick = {
                    if (title.isEmpty() || description.isEmpty() || category.isEmpty() ||
                        goalAmount.toDoubleOrNull() == null || imageUri == null
                    ) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please fill all fields and select an image.", withDismissAction = true)
                        }
                        return@Button
                    }

                    // 确保截止日期在今天之后
                    if (selectedDateMillis <= Calendar.getInstance().timeInMillis) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Due Date must be in the future.", withDismissAction = true)
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
                        dueDate = Timestamp(Date(selectedDateMillis))
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