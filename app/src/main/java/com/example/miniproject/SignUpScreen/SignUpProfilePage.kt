package com.example.miniproject.SignUpScreen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun SignUpProfilePage(navController: NavController, email: String, isGoogleSignUp: Boolean) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var usernameError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference

    val hasMinLength = password.length >= 8
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasLowerCase = password.any { it.isLowerCase() }
    val hasNumber = password.any { it.isDigit() }
    val hasSpecialChar = password.any { !it.isLetterOrDigit() }
    val isPwValid = hasMinLength && hasUpperCase && hasLowerCase && hasNumber && hasSpecialChar
    val pwMatch = password.isNotEmpty() && password == confirmPassword

    val handleCreateAccount: () -> Unit = {
        usernameError = ""
        passwordError = ""
        confirmPasswordError = ""

        var hasError = false

        if (username.isEmpty()) {
            usernameError = "Username is required"
            hasError = true
        }

        if (isGoogleSignUp) {
            if (!hasError) {
                isLoading = true
                val userId = auth.currentUser?.uid ?: ""

                val userData = mapOf(
                    "userId" to userId,
                    "email" to email,
                    "username" to username,
                    "userType" to "user",
                    "isVerified" to true,
                    "isGoogleAccount" to true,
                    "hasPassword" to false,
                    "profileImageUrl" to (auth.currentUser?.photoUrl?.toString() ?: ""),
                    "createdAt" to System.currentTimeMillis()
                )

                database.child("users").child(userId).setValue(userData)
                    .addOnSuccessListener {
                        isLoading = false
                        Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                        navController.navigate("mainPage") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    .addOnFailureListener {
                        isLoading = false
                        Toast.makeText(context, "Failed to save data", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            if (!isPwValid) {
                passwordError = "Please meet all password requirements"
                hasError = true
            }

            if (!pwMatch) {
                confirmPasswordError = "Passwords do not match"
                hasError = true
            }

            if (!hasError) {
                isLoading = true
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = task.result?.user?.uid ?: ""
                            val user = auth.currentUser

                            user?.sendEmailVerification()
                                ?.addOnSuccessListener {
                                    val userData = mapOf(
                                        "userId" to userId,
                                        "email" to email,
                                        "username" to username,
                                        "userType" to "user",
                                        "isVerified" to false,
                                        "isGoogleAccount" to false,
                                        "hasPassword" to true,
                                        "profileImageUrl" to "",
                                        "createdAt" to System.currentTimeMillis()
                                    )

                                    database.child("users").child(userId).setValue(userData)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            Toast.makeText(
                                                context,
                                                "Registration successful! Please check your email ($email) to verify your account.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            navController.navigate("login") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            Toast.makeText(context, "Failed to save data", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                ?.addOnFailureListener { exception ->
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Failed to send verification email: ${exception.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        } else {
                            isLoading = false
                            val exception = task.exception
                            when {
                                exception?.message?.contains("already in use", ignoreCase = true) == true -> {
                                    Toast.makeText(context, "Email already registered", Toast.LENGTH_SHORT).show()
                                }
                                exception?.message?.contains("weak password", ignoreCase = true) == true -> {
                                    passwordError = "Password is too weak"
                                }
                                else -> {
                                    Toast.makeText(
                                        context,
                                        "Registration failed: ${exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryBlue
                )
            }
            Text(
                text = "Complete Your Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                usernameError = ""
            },
            label = { Text("Username") },
            placeholder = { Text("Choose a username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (usernameError.isNotEmpty()) 4.dp else 16.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading,
            isError = usernameError.isNotEmpty(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = if (isGoogleSignUp) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (isGoogleSignUp) handleCreateAccount() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = BorderGray,
                focusedLabelColor = PrimaryBlue,
                cursorColor = PrimaryBlue
            )
        )

        if (usernameError.isNotEmpty()) {
            Text(
                text = usernameError,
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 8.dp)
            )
        }

        if (!isGoogleSignUp) {
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = ""
                },
                label = { Text("Password") },
                placeholder = { Text("Create a strong password") },
                visualTransformation = if (showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (password.isNotEmpty()) 4.dp else 16.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading,
                isError = password.isNotEmpty() && !isPwValid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderGray,
                    focusedLabelColor = PrimaryBlue,
                    cursorColor = PrimaryBlue
                )
            )

            if (password.isNotEmpty()) {
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
                        text = "Password requirements:",
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    confirmPasswordError = ""
                },
                label = { Text("Confirm Password") },
                placeholder = { Text("Re-enter your password") },
                visualTransformation = if (showConfirmPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            imageVector = if (showConfirmPassword)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                            contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (confirmPassword.isNotEmpty()) 4.dp else 16.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading,
                isError = confirmPassword.isNotEmpty() && !pwMatch,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { handleCreateAccount() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderGray,
                    focusedLabelColor = PrimaryBlue,
                    cursorColor = PrimaryBlue
                )
            )

            if (confirmPassword.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, bottom = 8.dp)
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

        Spacer(modifier = Modifier.height(if (isGoogleSignUp) 24.dp else 16.dp))

        Button(
            onClick = handleCreateAccount,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue,
                disabledContainerColor = PrimaryBlue.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading && username.isNotEmpty() &&
                    (isGoogleSignUp || (isPwValid && pwMatch))
        ) {
            Text(
                text = if (isLoading) "Creating Account..." else "Create Account",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
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