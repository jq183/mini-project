package com.example.miniproject.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.repository.AdminRepository
import com.example.miniproject.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@Composable
fun AdminLogin(navCollection: NavController) {
    var adminEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var showGeneralError by remember { mutableStateOf(false) }
    var generalErrorMessage by remember { mutableStateOf("") }

    val repository = remember { AdminRepository() }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF64B5F6)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(480.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(40.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DirectAdmin",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (showGeneralError) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = generalErrorMessage,
                            color = Color(0xFFD32F2F),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Admin Email",
                        fontSize = 14.sp,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = adminEmail,
                        onValueChange = {
                            adminEmail = it
                            emailError = false
                            showGeneralError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        isError = emailError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5DADE2),
                            unfocusedBorderColor = Color(0xFFE8EEF7),
                            errorBorderColor = Color(0xFFD32F2F),
                            focusedContainerColor = Color(0xFFF8F9FA),
                            unfocusedContainerColor = Color(0xFFF8F9FA)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (emailError) {
                        Text(
                            text = "Please use company email (@sp.com)",
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Password",
                        fontSize = 14.sp,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            showGeneralError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        enabled = !isLoading,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible)
                                        Icons.Default.Visibility
                                    else
                                        Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible)
                                        "Hide password"
                                    else
                                        "Show password",
                                    tint = Color(0xFF95A5A6)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5DADE2),
                            unfocusedBorderColor = Color(0xFFE8EEF7),
                            focusedContainerColor = Color(0xFFF8F9FA),
                            unfocusedContainerColor = Color(0xFFF8F9FA)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (adminEmail.isBlank()) {
                            generalErrorMessage = "Email cannot be empty"
                            showGeneralError = true
                            return@Button
                        }

                        if (password.isBlank()) {
                            generalErrorMessage = "Password cannot be empty"
                            showGeneralError = true
                            return@Button
                        }

                        if (!adminEmail.endsWith("@sp.com")) {
                            emailError = true
                            return@Button
                        }

                        isLoading = true
                        coroutineScope.launch {
                            val result = repository.adminLogin(adminEmail, password)

                            isLoading = false

                            if (result.isSuccess) {
                                val admin = result.getOrNull()
                                if (admin?.isFirstLogin == true) {
                                    navCollection.navigate("changePassword") {
                                        popUpTo("adminLogin") { inclusive = true }
                                    }
                                } else {
                                    navCollection.navigate("adminMainPage") {
                                        popUpTo("adminLogin") { inclusive = true }
                                    }
                                }
                            } else {
                                generalErrorMessage = result.exceptionOrNull()?.message
                                    ?: "Login failed"
                                showGeneralError = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Login",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { navCollection.navigateUp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(1.dp, PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Back",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryBlue
                    )
                }
            }
        }
    }
}

@Composable
fun ChangePasswordScreen(navController: NavController) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val repository = remember { AdminRepository() }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF64B5F6)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(480.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(40.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Change Password",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF9E6)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "This is your first login. Please change your password to continue.",
                        color = Color(0xFFF57C00),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                if (showError) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFD32F2F),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "New Password",
                        fontSize = 14.sp,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            showError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (newPasswordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        enabled = !isLoading,
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    imageVector = if (newPasswordVisible)
                                        Icons.Default.Visibility
                                    else
                                        Icons.Default.VisibilityOff,
                                    contentDescription = if (newPasswordVisible)
                                        "Hide password"
                                    else
                                        "Show password",
                                    tint = Color(0xFF95A5A6)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5DADE2),
                            unfocusedBorderColor = Color(0xFFE8EEF7),
                            focusedContainerColor = Color(0xFFF8F9FA),
                            unfocusedContainerColor = Color(0xFFF8F9FA)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Text(
                        text = "Minimum 6 characters",
                        color = Color(0xFF95A5A6),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Confirm Password",
                        fontSize = 14.sp,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            showError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (confirmPasswordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        enabled = !isLoading,
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible)
                                        Icons.Default.Visibility
                                    else
                                        Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible)
                                        "Hide password"
                                    else
                                        "Show password",
                                    tint = Color(0xFF95A5A6)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5DADE2),
                            unfocusedBorderColor = Color(0xFFE8EEF7),
                            focusedContainerColor = Color(0xFFF8F9FA),
                            unfocusedContainerColor = Color(0xFFF8F9FA)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        when {
                            newPassword.isBlank() -> {
                                errorMessage = "New password cannot be empty"
                                showError = true
                            }
                            newPassword.length < 6 -> {
                                errorMessage = "Password must be at least 6 characters"
                                showError = true
                            }
                            confirmPassword.isBlank() -> {
                                errorMessage = "Please confirm your password"
                                showError = true
                            }
                            newPassword != confirmPassword -> {
                                errorMessage = "Passwords do not match"
                                showError = true
                            }
                            newPassword == AdminRepository.DEFAULT_PASSWORD -> {
                                errorMessage = "Please choose a different password"
                                showError = true
                            }
                            else -> {
                                isLoading = true
                                coroutineScope.launch {
                                    val result = repository.changePassword(newPassword)
                                    isLoading = false

                                    if (result.isSuccess) {
                                        navController.navigate("adminMainPage") {
                                            popUpTo("changePassword") { inclusive = true }
                                        }
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message
                                            ?: "Failed to change password"
                                        showError = true
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Change Password",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}