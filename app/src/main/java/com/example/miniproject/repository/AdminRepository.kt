package com.example.miniproject.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Admin(
    val adminId: String = "",
    val email: String = "",
    val username: String = "",
    val responsible: List<String> = emptyList()
)

class AdminRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun isCompanyEmail(email: String): Boolean {
        return email.endsWith("@js.com")
    }

    suspend fun adminLogin(email: String, password: String): Result<Admin> {
        return try {
            if (!isCompanyEmail(email)) {
                return Result.failure(Exception("Please use company email (@js.com)"))
            }

            // 使用用户输入的密码进行登录
            try {
                auth.signInWithEmailAndPassword(email, password).await()
            } catch (e: Exception) {
                return Result.failure(Exception("Invalid email or password"))
            }

            val currentUser = auth.currentUser
            if (currentUser != null) {
                // 从 Firestore 获取 admin 信息
                val adminDoc = db.collection("admins")
                    .document(currentUser.uid)
                    .get()
                    .await()

                if (!adminDoc.exists()) {
                    // 如果 Firestore 中没有记录,创建一个默认的
                    val defaultResponsible = listOf("Education", "Technology")

                    val adminData = hashMapOf(
                        "email" to email,
                        "responsible" to defaultResponsible,
                        "created_at" to com.google.firebase.Timestamp.now()
                    )

                    db.collection("admins")
                        .document(currentUser.uid)
                        .set(adminData)
                        .await()

                    return Result.success(Admin(
                        adminId = currentUser.uid,
                        email = email,
                        username = email.substringBefore("@"),
                        responsible = defaultResponsible
                    ))
                }

                val responsible = adminDoc.get("responsible") as? List<String> ?: emptyList()

                val admin = Admin(
                    adminId = currentUser.uid,
                    email = email,
                    username = email.substringBefore("@"),
                    responsible = responsible
                )
                Result.success(admin)
            } else {
                Result.failure(Exception("Authentication failed"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentAdmin(): Admin? {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            try {
                val adminDoc = db.collection("admins")
                    .document(currentUser.uid)
                    .get()
                    .await()

                val responsible = adminDoc.get("responsible") as? List<String> ?: emptyList()

                Admin(
                    adminId = currentUser.uid,
                    email = currentUser.email ?: "",
                    username = currentUser.email?.substringBefore("@") ?: "",
                    responsible = responsible
                )
            } catch (e: Exception) {
                Admin(
                    adminId = currentUser.uid,
                    email = currentUser.email ?: "",
                    username = currentUser.email?.substringBefore("@") ?: ""
                )
            }
        } else {
            null
        }
    }

    fun signOut() {
        auth.signOut()
    }
}