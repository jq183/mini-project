package com.example.miniproject.repository

import android.content.Context
import com.example.miniproject.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    // 注册用户
    fun createNewUser(
        email: String,
        password: String,
        phone: String,
        userType: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (email.isEmpty() || password.isEmpty()) {
            onFailure("Please fill all fields")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = task.result?.user?.uid ?: return@addOnCompleteListener
                    saveUserData(userId, email, phone, userType, onSuccess, onFailure)
                } else {
                    onFailure(task.exception?.message ?: "Registration failed")
                }
            }
    }

    // 登录用户
    fun loginUser(
        email: String,
        password: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (email.isEmpty() || password.isEmpty()) {
            onFailure("Please fill all fields")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception?.message ?: "Login failed")
                }
            }
    }

    // 保存用户数据
    private fun saveUserData(
        userId: String,
        email: String,
        phone: String,
        userType: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = User(
            userId = userId,
            email = email,
            phone = phone,
            userType = userType
        )

        database.child("users").child(userId).setValue(user)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to save user data")
            }
    }

    // 登出
    fun signOut() {
        auth.signOut()
    }

    // 获取当前用户
    fun getCurrentUser() = auth.currentUser

    // 重置密码
    fun resetPassword(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to send reset email")
            }
    }
}