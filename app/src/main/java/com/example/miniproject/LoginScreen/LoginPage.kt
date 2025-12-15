package com.example.miniproject

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay

@Composable
fun LoginPage(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var navSignUp by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf("") }
    var pwError by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf("") }
    var showForgotPwDialog by remember { mutableStateOf(false) }
    var emailReset by remember { mutableStateOf("") }
    var displayedText by remember { mutableStateOf("") }
    val fullText = "SparkFund"

    var showGoogleAccountDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var pendingMergeUserId by remember { mutableStateOf("") }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference

    LaunchedEffect(navSignUp) {
        if (navSignUp) {
            Log.d("GoogleSignIn", "LaunchedEffect triggered - navigating to signUp")
            try {
                navController.navigate("signUp")
                navSignUp = false
                Log.d("GoogleSignIn", "Navigation executed successfully")
            } catch (e: Exception) {
                Log.e("GoogleSignIn", "Navigation failed in LaunchedEffect", e)
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("GoogleSignIn", "Result received: resultCode=${result.resultCode}")
        isLoading = false

        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Log.d("GoogleSignIn", "Result OK, processing...")
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("GoogleSignIn", "Account retrieved: ${account?.email}")

                if (account?.idToken != null) {
                    Log.d("GoogleSignIn", "ID Token found, authenticating with Firebase...")
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                    isLoading = true
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { authTask ->
                            isLoading = false
                            Log.d("GoogleSignIn", "Firebase auth complete: success=${authTask.isSuccessful}")

                            if (authTask.isSuccessful) {
                                val userId = authTask.result?.user?.uid ?: ""
                                val userEmail = authTask.result?.user?.email ?: ""
                                Log.d("GoogleSignIn", "User ID: $userId")

                                database.child("users")
                                    .orderByChild("email")
                                    .equalTo(userEmail)
                                    .get()
                                    .addOnSuccessListener { snapshot ->
                                        if (snapshot.exists()) {
                                            val existingUserData = snapshot.children.firstOrNull()?.value as? Map<*, *>
                                            val existingUserId = existingUserData?.get("userId") as? String
                                            val hasPassword = existingUserData?.get("hasPassword") as? Boolean ?: false
                                            val isGoogleAccount = existingUserData?.get("isGoogleAccount") as? Boolean ?: false

                                            if (hasPassword && !isGoogleAccount) {
                                                pendingMergeUserId = existingUserId ?: ""
                                                showMergeDialog = true
                                            } else {
                                                navController.navigate("mainPage")
                                            }
                                        } else {
                                            val user = auth.currentUser
                                            val userData = hashMapOf(
                                                "name" to (user?.displayName ?: "Google User"),
                                                "email" to (user?.email ?: ""),
                                                "profileImageUrl" to (user?.photoUrl?.toString() ?: ""),
                                                "userId" to userId,
                                                "isVerified" to true,
                                                "userType" to "user",
                                                "isGoogleAccount" to true,
                                                "hasPassword" to false,
                                                "createdAt" to System.currentTimeMillis()
                                            )

                                            database.child("users").child(userId).setValue(userData)
                                                .addOnSuccessListener {
                                                    Log.d("GoogleSignIn", "User profile created automatically")
                                                    navController.navigate("mainPage")
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("GoogleSignIn", "Failed to create user profile", e)
                                                    Toast.makeText(context, "Failed to create profile: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("GoogleSignIn", "Database check failed", e)
                                        Toast.makeText(context, "Database error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                Log.e("GoogleSignIn", "Firebase auth failed", authTask.exception)
                                Toast.makeText(
                                    context,
                                    "Authentication failed: ${authTask.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                } else {
                    Log.e("GoogleSignIn", "No ID token received")
                    Toast.makeText(context, "Google Sign-In failed: No ID token", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "ApiException: statusCode=${e.statusCode}", e)
                val errorMsg = when (e.statusCode) {
                    10 -> "Developer error: Check SHA-1 and Web Client ID"
                    12501 -> "Sign in cancelled by user"
                    12500 -> "Sign in failed: Try again"
                    7 -> "Network error"
                    else -> "Error code: ${e.statusCode} - ${e.message}"
                }
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        } else if (result.resultCode == android.app.Activity.RESULT_CANCELED) {
            Log.d("GoogleSignIn", "User cancelled sign in")
            Toast.makeText(context, "Sign in cancelled", Toast.LENGTH_SHORT).show()
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
        Spacer(modifier = Modifier.height(100.dp))

        Text(
            text = displayedText,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = ""
                loginError = ""
            },
            label = { Text("Email") },
            placeholder = { Text("Enter your email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (emailError.isNotEmpty()) 4.dp else 16.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading,
            isError = emailError.isNotEmpty(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = BorderGray,
                focusedLabelColor = PrimaryBlue,
                cursorColor = PrimaryBlue
            )
        )

        if(emailError.isNotEmpty()){
            Text(
                text = emailError,
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp,bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                pwError = ""
            },
            label = { Text("Password") },
            placeholder = { Text("Enter your password") },
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
                .padding(bottom = if (pwError.isNotEmpty()) 4.dp else 16.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading,
            isError = pwError.isNotEmpty(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                        emailError = ""
                        pwError = ""
                        var hasError = false

                        if (email.isEmpty()) {
                            emailError = "Email is required"
                            hasError = true
                        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            emailError = "Invalid email format"
                            hasError = true
                        }

                        if (!hasError) {
                            isLoading = true

                            database.child("users")
                                .orderByChild("email")
                                .equalTo(email)
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    if (snapshot.exists()) {
                                        val userData = snapshot.children.firstOrNull()?.value as? Map<*, *>
                                        val userId = userData?.get("userId") as? String
                                        val hasPassword = userData?.get("hasPassword") as? Boolean ?: false
                                        val isGoogleAccount = userData?.get("isGoogleAccount") as? Boolean ?: false

                                        if (userId != null) {
                                            if (password.isEmpty()) {
                                                if (isGoogleAccount && !hasPassword) {
                                                    isLoading = false
                                                    showGoogleAccountDialog = true
                                                } else {
                                                    isLoading = false
                                                    pwError = "Password is required"
                                                }
                                            } else {
                                                if (isGoogleAccount && !hasPassword) {
                                                    isLoading = false
                                                    showGoogleAccountDialog = true
                                                } else {
                                                    auth.signInWithEmailAndPassword(email, password)
                                                        .addOnCompleteListener { task ->
                                                            isLoading = false
                                                            if (task.isSuccessful) {
                                                                navController.navigate("mainPage")
                                                            } else {
                                                                val exception = task.exception
                                                                when {
                                                                    exception?.message?.contains("password", ignoreCase = true) == true -> {
                                                                        pwError = "Incorrect password"
                                                                    }
                                                                    exception?.message?.contains("user", ignoreCase = true) == true -> {
                                                                        emailError = "Account not found"
                                                                    }
                                                                    else -> {
                                                                        pwError = "Invalid email or password"
                                                                    }
                                                                }
                                                            }
                                                        }
                                                }
                                            }
                                        } else {
                                            isLoading = false
                                            emailError = "Account not found"
                                        }
                                    } else {
                                        isLoading = false
                                        emailError = "Account not found"
                                    }
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    Toast.makeText(context, "Failed to check account", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = BorderGray,
                focusedLabelColor = PrimaryBlue,
                cursorColor = PrimaryBlue
            )
        )

        if(pwError.isNotEmpty()){
            Text(
                text = pwError,
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp,bottom = 8.dp)
            )
        }else{
            Spacer(modifier = Modifier.height(12.dp))
        }

        TextButton(
            onClick = {
                emailReset = email
                showForgotPwDialog = true
            },
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 4.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = PrimaryBlue
            )
        ) {
            Text(
                text = "Forgot password?",
                textDecoration = TextDecoration.Underline,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                emailError = ""
                pwError = ""
                var hasError = false

                if (email.isEmpty()) {
                    emailError = "Email is required"
                    hasError = true
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailError = "Invalid email format"
                    hasError = true
                }

                if (!hasError) {
                    isLoading = true

                    database.child("users")
                        .orderByChild("email")
                        .equalTo(email)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                val userData = snapshot.children.firstOrNull()?.value as? Map<*, *>
                                val userId = userData?.get("userId") as? String
                                val hasPassword = userData?.get("hasPassword") as? Boolean ?: false
                                val isGoogleAccount = userData?.get("isGoogleAccount") as? Boolean ?: false

                                if (userId != null) {
                                    if (password.isEmpty()) {
                                        if (isGoogleAccount && !hasPassword) {
                                            isLoading = false
                                            showGoogleAccountDialog = true
                                        } else {
                                            isLoading = false
                                            pwError = "Password is required"
                                        }
                                    } else {
                                        if (isGoogleAccount && !hasPassword) {
                                            isLoading = false
                                            showGoogleAccountDialog = true
                                        } else {
                                            auth.signInWithEmailAndPassword(email, password)
                                                .addOnCompleteListener { task ->
                                                    isLoading = false
                                                    if (task.isSuccessful) {
                                                        navController.navigate("mainPage")
                                                    } else {
                                                        val exception = task.exception
                                                        when {
                                                            exception?.message?.contains("password", ignoreCase = true) == true -> {
                                                                pwError = "Incorrect password"
                                                            }
                                                            exception?.message?.contains("user", ignoreCase = true) == true -> {
                                                                emailError = "Account not found"
                                                            }
                                                            else -> {
                                                                pwError = "Invalid email or password"
                                                            }
                                                        }
                                                    }
                                                }
                                        }
                                    }
                                } else {
                                    isLoading = false
                                    emailError = "Account not found"
                                }
                            } else {
                                isLoading = false
                                emailError = "Account not found"
                            }
                        }
                        .addOnFailureListener {
                            isLoading = false
                            Toast.makeText(context, "Failed to check account", Toast.LENGTH_SHORT).show()
                        }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(bottom = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading
        ) {
            Text(
                text = if (isLoading) "Loading..." else "Login",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (loginError.isNotEmpty()) {
            Text(
                text = loginError,
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (showGoogleAccountDialog) {
            AlertDialog(
                onDismissRequest = { showGoogleAccountDialog = false },
                containerColor = BackgroundWhite,
                title = {
                    Text(
                        "Google Account Detected",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            "This account uses Google Sign-In.",
                            fontSize = 16.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "Please click 'Sign in with Google' button below to login.",
                            fontSize = 15.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            "Note: You can set a password in Profile → Change Password after logging in.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontStyle = FontStyle.Italic
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showGoogleAccountDialog = false }
                    ) {
                        Text(
                            "OK",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            fontSize = 16.sp
                        )
                    }
                }
            )
        }

        if (showMergeDialog) {
            AlertDialog(
                onDismissRequest = {
                    showMergeDialog = false
                    auth.signOut()
                },
                containerColor = BackgroundWhite,
                title = {
                    Text(
                        "Merge Accounts",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            "An account with this email already exists.",
                            fontSize = 16.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "Would you like to link your Google account to this existing account? You'll be able to login with both methods.",
                            fontSize = 15.sp,
                            color = TextSecondary
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val updates = hashMapOf<String, Any>(
                                "isGoogleAccount" to true,
                                "profileImageUrl" to (auth.currentUser?.photoUrl?.toString() ?: ""),
                                "hasPassword" to true
                            )

                            database.child("users").child(pendingMergeUserId)
                                .updateChildren(updates)
                                .addOnSuccessListener {
                                    showMergeDialog = false
                                    Toast.makeText(context, "Accounts merged successfully!", Toast.LENGTH_SHORT).show()
                                    navController.navigate("mainPage")
                                }
                                .addOnFailureListener {
                                    showMergeDialog = false
                                    Toast.makeText(context, "Failed to merge accounts", Toast.LENGTH_SHORT).show()
                                }
                        }
                    ) {
                        Text(
                            "Merge",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            fontSize = 16.sp
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showMergeDialog = false
                            auth.signOut()
                        }
                    ) {
                        Text(
                            "Cancel",
                            color = TextSecondary,
                            fontSize = 16.sp
                        )
                    }
                }
            )
        }

        OutlinedButton(
            onClick = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()

                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                googleSignInClient.signOut()
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(bottom = 2.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PrimaryBlue
            ),
            enabled = !isLoading
        ) {
            Text("Sign in with Google", fontSize = 16.sp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account?",
                fontSize = 14.sp,
                color = TextSecondary
            )
            TextButton(
                onClick = {
                    navController.navigate("signUpEmail")
                          },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = PrimaryBlue
                ),
                enabled = !isLoading
            ) {
                Text(
                    text = "Sign Up",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    auth.signOut()
                    navController.navigate("mainPage") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = PrimaryBlue
                )
            ) {
                Text(
                    text = "Continue as Guest",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    textDecoration = TextDecoration.Underline
                )
            }

            Text(
                text = "|",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            TextButton(
                onClick = { navController.navigate("admin login") },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = PrimaryBlue
                )
            ) {
                Text(
                    text = "Login as Admin",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    textDecoration = TextDecoration.Underline
                )
            }
        }

        if (showForgotPwDialog){
            navController.navigate("resetPw")
        }
    }
}