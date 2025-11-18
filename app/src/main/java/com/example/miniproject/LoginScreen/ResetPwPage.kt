package com.example.miniproject.LoginScreen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun ResetPwPage(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var emailSent by remember { mutableStateOf(false) }
    var invalidEmail by remember { mutableStateOf("") }

    // 添加倒计时相关状态 ✅
    var countdown by remember { mutableStateOf(0) }
    var canResend by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // 添加倒计时逻辑 ✅
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        } else {
            canResend = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryBlue
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
                .background(BackgroundWhite)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Email",
                modifier = Modifier.size(80.dp),
                tint = PrimaryBlue
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!emailSent) {
                Text(
                    text = "Reset Password",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Enter your email address and we'll send you a link to reset your password.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        invalidEmail = when {
                            it.isEmpty() -> ""
                            !it.contains("@") -> "Please enter a valid email address"
                            !it.contains(".") -> "Please enter a valid email address"
                            else -> ""
                        }
                    },
                    label = { Text("Email Address") },
                    placeholder = { Text("Enter your email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue,
                        cursorColor = PrimaryBlue
                    ),
                    supportingText = {
                        if (invalidEmail.isNotEmpty()) {
                            Text(
                                text = invalidEmail,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        when {
                            email.isEmpty() -> {
                                invalidEmail = "Please enter a valid email address"
                                return@Button
                            }
                            !email.contains("@") || !email.contains(".") -> {
                                invalidEmail = "Please enter a valid email address"
                                return@Button
                            }
                        }

                        isLoading = true
                        auth.sendPasswordResetEmail(email)
                            .addOnSuccessListener {
                                isLoading = false
                                emailSent = true
                                countdown = 60  // 开始60秒倒计时 ✅
                                canResend = false
                            }
                            .addOnFailureListener { exception ->
                                isLoading = false
                                val errorMessage = when {
                                    exception.message?.contains("no user record", ignoreCase = true) == true ->
                                        "No account found with this email"
                                    exception.message?.contains("badly formatted", ignoreCase = true) == true ->
                                        "Invalid email format"
                                    else -> "Failed to send reset email: ${exception.message}"
                                }
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading
                ) {
                    Text(
                        text = if (isLoading) "Sending..." else "Send Link",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { navController.popBackStack() },
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Back to Login",
                        color = PrimaryBlue,
                        fontSize = 14.sp
                    )
                }
            } else {
                // 邮件已发送的界面
                Text(
                    text = "Check Your Email",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "We've sent a password reset link to:",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = email,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryBlue.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📧 Check your inbox",
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Click the link in the email to reset your password",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Back to Login",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 修改重发按钮 ✅
                TextButton(
                    onClick = {
                        if (canResend) {
                            isLoading = true
                            auth.sendPasswordResetEmail(email)
                                .addOnSuccessListener {
                                    isLoading = false
                                    countdown = 60
                                    canResend = false
                                    Toast.makeText(context, "Email resent successfully!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { exception ->
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Failed to resend: ${exception.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    },
                    enabled = canResend && !isLoading
                ) {
                    Text(
                        text = if (countdown > 0) {
                            "Resend email in ${countdown}s"
                        } else {
                            "Didn't receive email? Resend"
                        },
                        color = if (canResend) PrimaryBlue else TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}