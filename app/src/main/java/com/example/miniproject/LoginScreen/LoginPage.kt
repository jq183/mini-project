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
                                Log.d("GoogleSignIn", "User ID: $userId")

                                database.child("users").child(userId).get()
                                    .addOnSuccessListener { snapshot ->
                                        if (snapshot.exists()) {
                                            navController.navigate("mainPage")
                                        } else {
                                            val user = auth.currentUser
                                            val userData = hashMapOf(
                                                "name" to (user?.displayName ?: "Google User"),
                                                "email" to (user?.email ?: ""),
                                                "profileImageUrl" to (user?.photoUrl?.toString() ?: ""),
                                                "userId" to userId

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
                                        navSignUp = true
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
            onClick = { emailReset = email
                      showForgotPwDialog = true},
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

                if (password.isEmpty()) {
                    pwError = "Password is required"
                    hasError = true
                }

                if (!hasError) {
                    isLoading = true
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                navController.navigate("mainPage")
                            } else {
                                val exception = task.exception
                                Log.e("LoginError", "Exception: ${exception?.javaClass?.simpleName}")
                                Log.e("LoginError", "Message: ${exception?.message}")

                                when {
                                    exception?.message?.contains("password", ignoreCase = true) == true -> {
                                        pwError = "Incorrect password"
                                    }
                                    exception?.message?.contains("user", ignoreCase = true) == true ||
                                            exception?.message?.contains("email", ignoreCase = true) == true -> {
                                        emailError = "Account not found"
                                    }
                                    exception?.message?.contains("credential", ignoreCase = true) == true -> {
                                        pwError = "Invalid email or password"
                                    }
                                    exception?.message?.contains("disabled", ignoreCase = true) == true -> {
                                        emailError = "This account has been disabled"
                                    }
                                    else -> {
                                        pwError = "Invalid email or password"
                                    }
                                }
                            }
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

        OutlinedButton(
            onClick = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()

                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                googleSignInClient.signOut()
                googleSignInLauncher.launch(googleSignInClient.signInIntent)            },
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
                    navController.navigate("signUp")
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
                onClick = { navController.navigate("mainPage") },
            
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