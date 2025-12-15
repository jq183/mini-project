package com.example.miniproject.UserScreen.ProfileScreen

import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeEmailPage(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val database = FirebaseDatabase.getInstance().reference
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var newEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf("") }  // ✅ 新增
    var passwordError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var verificationSent by remember { mutableStateOf(false) }

    val currentUser = auth.currentUser
    val isEmailValid = newEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()
    val isNewEmailDifferent = newEmail != currentUser?.email
    val hasPasswordProvider = currentUser?.providerData?.any { it.providerId == "password" } ?: false

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Change Email", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().background(BackgroundGray).padding(paddingValues).verticalScroll(rememberScrollState())
        ) {
            if (!hasPasswordProvider) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(120.dp).background(ErrorRed.copy(0.1f), RoundedCornerShape(60.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, null, tint = ErrorRed, modifier = Modifier.size(64.dp))
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Cannot Change Email", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "You signed in with Google and don't have a password set. To change your email, please set up a password first in Account Settings.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Go Back", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (!verificationSent) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(BackgroundWhite),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Current Email", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = currentUser?.email ?: "",
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = BorderGray,
                                disabledContainerColor = BackgroundGray,
                                disabledTextColor = TextSecondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("New Email", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = {
                                newEmail = it.trim()
                                emailError = ""  // ✅ 清除错误
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            placeholder = { Text("Enter new email") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderGray,
                                focusedContainerColor = BackgroundWhite,
                                unfocusedContainerColor = BackgroundWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            isError = (newEmail.isNotEmpty() && (!isEmailValid || !isNewEmailDifferent)) || emailError.isNotEmpty()  // ✅ 加上 emailError
                        )

// ✅ 显示验证状态或错误
                        if (newEmail.isNotEmpty() || emailError.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (emailError.isNotEmpty()) Icons.Default.Cancel
                                    else if (isEmailValid && isNewEmailDifferent) Icons.Default.CheckCircle
                                    else Icons.Default.Cancel,
                                    null,
                                    tint = if (emailError.isNotEmpty()) ErrorRed
                                    else if (isEmailValid && isNewEmailDifferent) SuccessGreen
                                    else ErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    when {
                                        emailError.isNotEmpty() -> emailError  // ✅ 显示 emailError
                                        !isEmailValid -> "Invalid email"
                                        !isNewEmailDifferent -> "Must be different"
                                        else -> "Valid"
                                    },
                                    fontSize = 13.sp,
                                    color = if (emailError.isNotEmpty()) ErrorRed
                                    else if (isEmailValid && isNewEmailDifferent) SuccessGreen
                                    else ErrorRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Current Password", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; passwordError = "" },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            placeholder = { Text("Enter password") },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = TextSecondary)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderGray,
                                focusedContainerColor = BackgroundWhite,
                                unfocusedContainerColor = BackgroundWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            isError = passwordError.isNotEmpty()
                        )
                        if (passwordError.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Cancel, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(passwordError, fontSize = 13.sp, color = ErrorRed)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        scope.launch {
                            when {
                                newEmail.isEmpty() -> snackbarHostState.showSnackbar("Enter new email")
                                !isEmailValid -> snackbarHostState.showSnackbar("Invalid email")
                                !isNewEmailDifferent -> snackbarHostState.showSnackbar("Email must be different")
                                password.isEmpty() -> passwordError = "Password required"
                                else -> {
                                    isLoading = true
                                    emailError = ""
                                    try {
                                        val methods = auth.fetchSignInMethodsForEmail(newEmail).await()
                                        if (methods.signInMethods?.isNotEmpty() == true) {
                                            isLoading = false
                                            emailError = "Email already in use"
                                            return@launch
                                        }

                                        val cred = EmailAuthProvider.getCredential(currentUser?.email ?: "", password)
                                        currentUser?.reauthenticate(cred)?.await()
                                        Log.d("ChangeEmail", "✅ Re-authenticated")

                                        try {
                                            currentUser?.unlink("google.com")?.await()
                                            Log.d("ChangeEmail", "✅ Google provider unlinked")
                                        } catch (ue: Exception) {
                                            Log.e("ChangeEmail", "Unlink: ${ue.message}")
                                        }

                                        currentUser?.verifyBeforeUpdateEmail(newEmail)?.await()
                                        Log.d("ChangeEmail", "✅ Verification sent")

                                        isLoading = false
                                        verificationSent = true

                                    } catch (e: Exception) {
                                        isLoading = false
                                        Log.e("ChangeEmail", "Error: ${e.message}")
                                        when {
                                            e.message?.contains("password is invalid") == true ->
                                                passwordError = "Incorrect password"
                                            e.message?.contains("email-already-in-use") == true ->
                                                emailError = "Email already in use"
                                            else ->
                                                snackbarHostState.showSnackbar("Error: ${e.message}")
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp),
                    enabled = !isLoading && isEmailValid && isNewEmailDifferent,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, disabledContainerColor = PrimaryBlue.copy(0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BackgroundWhite, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Send Verification Email", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(120.dp).background(PrimaryBlue.copy(0.1f), RoundedCornerShape(60.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Email, null, tint = PrimaryBlue, modifier = Modifier.size(64.dp))
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Check Your Email", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("We've sent a verification link to:", fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(newEmail, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(32.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(BackgroundWhite)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text("1️⃣", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Check your NEW email", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Open $newEmail inbox", fontSize = 13.sp, color = TextSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Text("2️⃣", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Click the verification link", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("This will update your email", fontSize = 13.sp, color = TextSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Text("3️⃣", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Come back and sign out", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Tap button below after verifying", fontSize = 13.sp, color = TextSecondary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                try {
                                    val userId = auth.currentUser?.uid
                                    if (userId != null) {
                                        database.child("users").child(userId).child("email").setValue(newEmail).await()
                                        Log.d("ChangeEmail", "✅ Database updated")
                                    }

                                    try {
                                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                            .requestIdToken("125120901156-ein014qi9a88mafhuj0lfhv20dltds5o.apps.googleusercontent.com")
                                            .requestEmail()
                                            .build()
                                        GoogleSignIn.getClient(context, gso).signOut().await()
                                        Log.d("ChangeEmail", "✅ Google signed out")
                                    } catch (ge: Exception) {
                                        Log.e("ChangeEmail", "Google signout: ${ge.message}")
                                    }

                                    auth.signOut()
                                    Log.d("ChangeEmail", "✅ Firebase signed out")

                                    delay(300)
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    isLoading = false
                                    Log.e("ChangeEmail", "Error: ${e.message}")
                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BackgroundWhite, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("I've Verified - Sign Out", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { verificationSent = false; password = ""; passwordError = "" }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}