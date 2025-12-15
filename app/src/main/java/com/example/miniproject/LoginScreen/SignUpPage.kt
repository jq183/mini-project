package com.example.miniproject.LoginScreen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.R
import com.example.miniproject.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay

@Composable
fun SignUpPage(onBackToLogin: () -> Unit = {}, navController: NavController) {
    var currentStep by remember { mutableStateOf(1) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleSignUp by remember { mutableStateOf(false) }
    var googleEmail by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }

    var displayedText by remember { mutableStateOf("") }
    val fullText = "SparkFund"

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

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            isLoading = true
            auth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val userId = authTask.result?.user?.uid ?: ""
                        googleEmail = authTask.result?.user?.email ?: ""

                        database.child("users").child(userId).get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot.exists()) {
                                    isLoading = false
                                    Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                                    onBackToLogin()
                                } else {
                                    isLoading = false
                                    isGoogleSignUp = true
                                    email = googleEmail
                                    currentStep = 2
                                }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                Toast.makeText(context, "Failed to check user data", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        isLoading = false
                        Toast.makeText(context, "Google sign in failed", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: ApiException) {
            isLoading = false
            Toast.makeText(context, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        fullText.forEachIndexed { index, _ ->
            delay(150)
            displayedText = fullText.substring(0, index + 1)
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
            IconButton(onClick = {
                if (currentStep == 1) {
                    onBackToLogin()
                } else {
                    if (isGoogleSignUp) {
                        onBackToLogin()
                    } else {
                        currentStep = 1
                    }
                }
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryBlue
                )
            }
            Text(
                text = if (currentStep == 1) "Create Account" else "Complete Your Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        AnimatedVisibility(
            visible = currentStep == 1,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = ""
                    },
                    label = { Text("Email") },
                    placeholder = { Text("Enter your email address") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (emailError.isNotEmpty()) 4.dp else 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading,
                    isError = emailError.isNotEmpty(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderGray,
                        focusedLabelColor = PrimaryBlue,
                        cursorColor = PrimaryBlue
                    )
                )

                if (emailError.isNotEmpty()) {
                    Text(
                        text = emailError,
                        color = ErrorRed,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        emailError = ""

                        if (email.isEmpty()) {
                            emailError = "Email is required"
                            return@Button
                        }
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            emailError = "Invalid email format"
                            return@Button
                        }

                        isLoading = true

                        database.child("users")
                            .orderByChild("email")
                            .equalTo(email)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                isLoading = false
                                if (snapshot.exists()) {
                                    emailError = "Email already registered"
                                } else {
                                    currentStep = 2
                                }
                            }
                            .addOnFailureListener { exception ->
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Failed to check email: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading && email.isNotEmpty()
                ) {
                    Text(
                        text = if (isLoading) "Checking..." else "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = BorderLight
                    )
                    Text(
                        text = "OR",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = BorderLight
                    )
                }

                OutlinedButton(
                    onClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(context.getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()

                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrimaryBlue
                    ),
                    enabled = !isLoading
                ) {
                    Text("Sign up with Google", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account?",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    TextButton(
                        onClick = onBackToLogin,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = PrimaryBlue
                        )
                    ) {
                        Text(
                            text = "Login",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = currentStep == 2,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                    // ==========================================
                    // Password Field with Validation
                    // ==========================================
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderGray,
                            focusedLabelColor = PrimaryBlue,
                            cursorColor = PrimaryBlue
                        )
                    )

                    // ==========================================
                    // Password Requirements Display
                    // ==========================================
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderGray,
                            focusedLabelColor = PrimaryBlue,
                            cursorColor = PrimaryBlue
                        )
                    )

                    // ==========================================
                    // Password Match Indicator
                    // ==========================================
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
                    onClick = {
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
                                    "email" to googleEmail,
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
                                        navController.navigate("mainPage")
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
                                                            onBackToLogin()
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
                                                    emailError = "Email already registered"
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
                    },
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
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

// ==========================================
// Password Requirement Item Composable
// ==========================================
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