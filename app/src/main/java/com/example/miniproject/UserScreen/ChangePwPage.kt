package com.example.miniproject.UserScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.ui.theme.*
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePwPage(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val hasPasswordProvider = currentUser?.providerData?.any {
        it.providerId == "password"
    } ?: false

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var currentPasswordError by remember { mutableStateOf(false) }

    val hasMinLength = newPassword.length >= 8
    val hasUpperCase = newPassword.any { it.isUpperCase() }
    val hasLowerCase = newPassword.any { it.isLowerCase() }
    val hasNumber = newPassword.any { it.isDigit() }
    val hasSpecialChar = newPassword.any { !it.isLetterOrDigit() }
    val pwMatch = newPassword.isNotEmpty() && newPassword == confirmPassword

    val isPwValid = hasMinLength && hasUpperCase && hasLowerCase && hasNumber && hasSpecialChar

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (hasPasswordProvider) "Change Password" else "Set Password",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundWhite
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            if (!hasPasswordProvider) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryBlue.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "ℹ️ Google Login Account",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You're currently using Google login. Set a password to enable email/password login as well.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    if (hasPasswordProvider) {
                        Text(
                            text = "Current Password",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = {
                                currentPassword = it
                                currentPasswordError = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (currentPasswordVisible)
                                VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                                    Icon(
                                        imageVector = if (currentPasswordVisible)
                                            Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "password visibility",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderGray,
                                focusedContainerColor = BackgroundWhite,
                                unfocusedContainerColor = BackgroundWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            isError = currentPasswordError
                        )

                        if (currentPasswordError) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Current password is incorrect",
                                    fontSize = 13.sp,
                                    color = ErrorRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Text(
                        text = "New Password",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (newPasswordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    imageVector = if (newPasswordVisible)
                                        Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderGray,
                            focusedContainerColor = BackgroundWhite,
                            unfocusedContainerColor = BackgroundWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        isError = newPassword.isNotEmpty() && !isPwValid
                    )

                    if (newPassword.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isPwValid) SuccessGreen.copy(alpha = 0.1f)
                                    else ErrorRed.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "The password must have:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPwValid) SuccessGreen else ErrorRed
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            PasswordRequirementItem(
                                text = "At least 8 characters",
                                isMet = hasMinLength
                            )
                            PasswordRequirementItem(
                                text = "One uppercase letter (A-Z)",
                                isMet = hasUpperCase
                            )
                            PasswordRequirementItem(
                                text = "One lowercase letter (a-z)",
                                isMet = hasLowerCase
                            )
                            PasswordRequirementItem(
                                text = "One number (0-9)",
                                isMet = hasNumber
                            )
                            PasswordRequirementItem(
                                text = "One special character (!@#$%^&*)",
                                isMet = hasSpecialChar
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Confirm New Password",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (confirmPasswordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible)
                                        Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderGray,
                            focusedContainerColor = BackgroundWhite,
                            unfocusedContainerColor = BackgroundWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        isError = confirmPassword.isNotEmpty() && !pwMatch
                    )
                    if (confirmPassword.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (pwMatch) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (pwMatch) SuccessGreen else ErrorRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (pwMatch) "Passwords match" else "Passwords do not match",
                                fontSize = 13.sp,
                                color = if (pwMatch) SuccessGreen else ErrorRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    scope.launch {
                        if (hasPasswordProvider) {
                            // Change existing password
                            when {
                                currentPassword.isEmpty() -> {
                                    snackbarHostState.showSnackbar("Please enter your current password")
                                }
                                !isPwValid -> {
                                    snackbarHostState.showSnackbar("Please meet all password requirements")
                                }
                                !pwMatch -> {
                                    snackbarHostState.showSnackbar("Passwords do not match")
                                }
                                currentPassword == newPassword -> {
                                    snackbarHostState.showSnackbar("New password must be different from current password")
                                }
                                else -> {
                                    isLoading = true
                                    try {
                                        val credential = EmailAuthProvider.getCredential(
                                            currentUser?.email ?: "",
                                            currentPassword
                                        )
                                        currentUser?.reauthenticate(credential)?.await()
                                        currentUser?.updatePassword(newPassword)?.await()

                                        isLoading = false
                                        showSuccessDialog = true
                                    } catch (e: Exception) {
                                        isLoading = false
                                        when {
                                            e.message?.contains("password is invalid") == true -> {
                                                currentPasswordError = true
                                                snackbarHostState.showSnackbar("Current password is incorrect")
                                            }
                                            e.message?.contains("network") == true -> {
                                                snackbarHostState.showSnackbar("Network error. Please check your connection")
                                            }
                                            else -> {
                                                snackbarHostState.showSnackbar("Failed to update password")
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Set password for Google user
                            when {
                                !isPwValid -> {
                                    snackbarHostState.showSnackbar("Please meet all password requirements")
                                }
                                !pwMatch -> {
                                    snackbarHostState.showSnackbar("Passwords do not match")
                                }
                                else -> {
                                    isLoading = true
                                    try {
                                        currentUser?.updatePassword(newPassword)?.await()
                                        isLoading = false
                                        showSuccessDialog = true
                                    } catch (e: Exception) {
                                        isLoading = false
                                        snackbarHostState.showSnackbar("Failed to set password: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                enabled = !isLoading && isPwValid && pwMatch &&
                        (if (hasPasswordProvider) currentPassword.isNotEmpty() else true),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    disabledContainerColor = PrimaryBlue.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = BackgroundWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (hasPasswordProvider) "Update" else "Set",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(SuccessGreen.copy(alpha = 0.1f), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        fontSize = 32.sp,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            title = {
                Text(
                    if (hasPasswordProvider) "Password Updated" else "Password Set",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    if (hasPasswordProvider)
                        "Your password has been successfully updated."
                    else
                        "Password has been set successfully. You can now login with email and password.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.navigateUp()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    )
                ) {
                    Text("Done")
                }
            },
            containerColor = BackgroundWhite,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun PasswordRequirementItem(
    text: String,
    isMet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isMet) SuccessGreen else ErrorRed,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (isMet) SuccessGreen else ErrorRed
        )
    }
}