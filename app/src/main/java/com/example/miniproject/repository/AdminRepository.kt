package com.example.miniproject.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Admin(
    val adminId: String = "",
    val email: String = "",
    val username: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

class AdminRepository {
    private val db = FirebaseFirestore.getInstance()
    private val adminCollection = db.collection("admins")

    fun isCompanyEmail(email: String): Boolean {
        return email.endsWith("@js.com")
    }

    suspend fun checkAdminEmail(email: String): Result<Boolean> {
        return try {
            val querySnapshot = adminCollection
                .whereEqualTo("email", email)
                .get()
                .await()

            Result.success(!querySnapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminLogin(email: String): Result<Admin> {
        return try {
            if (!isCompanyEmail(email)) {
                return Result.failure(Exception("Please use company email (@js.com)"))
            }

            val exists = checkAdminEmail(email).getOrNull() ?: false

            if (!exists) {
                val adminId = adminCollection.document().id
                val admin = Admin(
                    adminId = adminId,
                    email = email,
                    username = email.substringBefore("@"),
                    createdAt = System.currentTimeMillis()
                )
                adminCollection.document(adminId).set(admin).await()
                return Result.success(admin)
            }

            val querySnapshot = adminCollection
                .whereEqualTo("email", email)
                .get()
                .await()

            val document = querySnapshot.documents[0]
            val admin = document.toObject(Admin::class.java)

            if (admin != null) {
                Result.success(admin)
            } else {
                Result.failure(Exception("Failed to read admin information"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminByEmail(email: String): Result<Admin?> {
        return try {
            val querySnapshot = adminCollection
                .whereEqualTo("email", email.lowercase())
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val admin = querySnapshot.documents[0].toObject(Admin::class.java)
                Result.success(admin)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

