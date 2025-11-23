package com.example.miniproject.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Admin(
    val adminId: String = "",
    val email: String = "",
    val username: String = ""
)

class AdminRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun isCompanyEmail(email: String): Boolean {
        return email.endsWith("@js.com")
    }

    suspend fun adminLogin(email: String): Result<Admin> {
        return try {
            if (!isCompanyEmail(email)) {
                return Result.failure(Exception("Please use company email (@js.com)"))
            }

            // 使用临时密码进行登录（或者你可以使用其他方式）
            // 这里使用 email 作为密码的示例，实际应用中应该有真实的密码
            val tempPassword = "admin123456" // 你可以根据需求修改

            try {
                // 尝试登录
                auth.signInWithEmailAndPassword(email, tempPassword).await()
            } catch (e: Exception) {
                // 如果登录失败，说明账户不存在，创建新账户
                auth.createUserWithEmailAndPassword(email, tempPassword).await()
            }

            val currentUser = auth.currentUser
            if (currentUser != null) {
                val admin = Admin(
                    adminId = currentUser.uid,
                    email = email,
                    username = email.substringBefore("@")
                )
                Result.success(admin)
            } else {
                Result.failure(Exception("Authentication failed"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentAdmin(): Admin? {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            Admin(
                adminId = currentUser.uid,
                email = currentUser.email ?: "",
                username = currentUser.email?.substringBefore("@") ?: ""
            )
        } else {
            null
        }
    }

    fun signOut() {
        auth.signOut()
    }
}