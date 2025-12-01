package com.example.miniproject.repository

import com.example.miniproject.AdminScreen.AdminAction
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class Admin(
    val adminId: String = "",
    val email: String = "",
    val username: String = "",
    val isFirstLogin: Boolean = false
)

class AdminRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    companion object {
        const val COMPANY_DOMAIN = "@sp.com"
        const val DEFAULT_PASSWORD = "admin123"
    }

    fun isCompanyEmail(email: String): Boolean {
        return email.endsWith(COMPANY_DOMAIN)
    }

    suspend fun adminLogin(email: String, password: String): Result<Admin> {
        return try {
            if (!isCompanyEmail(email)) {
                return Result.failure(Exception("Please use company email ($COMPANY_DOMAIN)"))
            }

            // 先尝试用输入的密码登录
            try {
                auth.signInWithEmailAndPassword(email, password).await()
            } catch (loginException: Exception) {
                // 如果登录失败，且密码是默认密码，尝试自动注册
                if (password == DEFAULT_PASSWORD) {
                    try {
                        auth.createUserWithEmailAndPassword(email, password).await()
                        // 注册成功，继续下面的流程
                    } catch (createException: Exception) {
                        return Result.failure(Exception("Invalid email or password"))
                    }
                } else {
                    return Result.failure(Exception("Invalid email or password"))
                }
            }

            val currentUser = auth.currentUser
            if (currentUser != null) {
                // 从 Firestore 获取 admin 信息
                val querySnapshot = db.collection("Admins")
                    .whereEqualTo("Email", email)
                    .get()
                    .await()

                if (querySnapshot.isEmpty) {
                    // 如果没有记录，创建新的 admin 文档
                    val newAdminId = generateAdminId()


                    val adminData = hashMapOf(
                        "Email" to email,
                        "Created_at" to com.google.firebase.Timestamp.now(),
                        "IsFirstLogin" to true
                    )

                    db.collection("Admins")
                        .document(newAdminId)
                        .set(adminData)
                        .await()

                    return Result.success(Admin(
                        adminId = newAdminId,
                        email = email,
                        username = email.substringBefore("@"),
                        isFirstLogin = true
                    ))
                }

                val adminDoc = querySnapshot.documents[0]
                val adminId = adminDoc.id
                val isFirstLogin = adminDoc.getBoolean("IsFirstLogin") ?: false

                // 检查是否是第一次登录（密码是 admin123）
                val needPasswordChange = password == DEFAULT_PASSWORD && isFirstLogin

                val admin = Admin(
                    adminId = adminId,
                    email = email,
                    username = email.substringBefore("@"),
                    isFirstLogin = needPasswordChange
                )
                Result.success(admin)
            } else {
                Result.failure(Exception("Authentication failed"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(newPassword: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // 更新 Firebase Auth 密码
                currentUser.updatePassword(newPassword).await()

                // 更新 Firestore 中的 IsFirstLogin 状态
                val querySnapshot = db.collection("Admins")
                    .whereEqualTo("Email", currentUser.email)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val adminDoc = querySnapshot.documents[0]
                    db.collection("Admins")
                        .document(adminDoc.id)
                        .update("IsFirstLogin", false)
                        .await()
                }

                Result.success(Unit)
            } else {
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentAdmin(): Admin? {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            try {
                val querySnapshot = db.collection("Admins")
                    .whereEqualTo("Email", currentUser.email)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val adminDoc = querySnapshot.documents[0]
                    val isFirstLogin = adminDoc.getBoolean("IsFirstLogin") ?: false

                    Admin(
                        adminId = adminDoc.id,
                        email = currentUser.email ?: "",
                        username = currentUser.email?.substringBefore("@") ?: "",
                        isFirstLogin = isFirstLogin
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    private suspend fun generateAdminId(): String {
        val adminsSnapshot = db.collection("Admins").get().await()
        val existingIds = adminsSnapshot.documents.map { it.id }

        var counter = 1
        var newId = "A${counter.toString().padStart(3, '0')}"

        while (existingIds.contains(newId)) {
            counter++
            newId = "A${counter.toString().padStart(3, '0')}"
        }

        return newId
    }

    fun signOut() {
        auth.signOut()
    }
}

class AdminActionRepository {
    private val db = FirebaseFirestore.getInstance()
    private val actionsRef = db.collection("AdminActions")
    private val projectsRef = db.collection("projects")
    private val adminsRef = db.collection("Admins")
    private val certificationsRef = db.collection("Certifications")
    private val reportsRef = db.collection("Reports")

    // 生成 AdminAction ID
    private suspend fun generateActionId(): String {
        val snapshot = actionsRef.get().await()
        val existingIds = snapshot.documents.map { it.id }

        var counter = 1
        var newId = "AA${counter.toString().padStart(4, '0')}"

        while (existingIds.contains(newId)) {
            counter++
            newId = "AA${counter.toString().padStart(4, '0')}"
        }

        return newId
    }

    // 记录管理员操作
    suspend fun recordAction(
        actionType: String,
        projectId: String,
        adminId: String,
        description: String,
        additionalInfo: String = ""
    ): Result<String> {
        return try {
            // 获取项目标题
            val projectDoc = projectsRef.document(projectId).get().await()
            val projectTitle = projectDoc.getString("Title") ?: "Unknown Project"

            // 获取管理员邮箱
            val adminDoc = adminsRef.document(adminId).get().await()
            val adminEmail = adminDoc.getString("Email") ?: "Unknown Admin"

            // 生成 Action ID
            val actionId = generateActionId()

            // 创建操作记录
            val actionData = hashMapOf(
                "Action_Type" to actionType,
                "Project_ID" to projectId,
                "Project_Title" to projectTitle,
                "Admin_ID" to adminId,
                "Admin_Email" to adminEmail,
                "Description" to description,
                "Additional_Info" to additionalInfo,
                "Timestamp" to Timestamp.now()
            )

            actionsRef.document(actionId).set(actionData).await()

            Result.success(actionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 获取所有操作历史
    suspend fun getAllActions(): Result<List<AdminAction>> {
        return try {
            val snapshot = actionsRef
                .orderBy("Timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val actions = snapshot.documents.mapNotNull { doc ->
                try {
                    AdminAction(
                        id = doc.id,
                        actionType = doc.getString("Action_Type") ?: "",
                        projectId = doc.getString("Project_ID") ?: "",
                        projectTitle = doc.getString("Project_Title") ?: "Unknown Project",
                        adminEmail = doc.getString("Admin_Email") ?: "Unknown Admin",
                        description = doc.getString("Description") ?: "",
                        timestamp = doc.getTimestamp("Timestamp"),
                        additionalInfo = doc.getString("Additional_Info") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(actions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 根据操作类型过滤
    fun filterActionsByType(actions: List<AdminAction>, filter: String): List<AdminAction> {
        return when (filter) {
            "All" -> actions
            "Verifications" -> actions.filter {
                it.actionType == "verified" || it.actionType == "unverified"
            }
            "Flags & Warnings" -> actions.filter {
                it.actionType == "flagged" || it.actionType == "unflagged" ||
                        it.actionType == "suspended" || it.actionType == "resolved"
            }
            "Deletions" -> actions.filter { it.actionType == "deleted" }
            else -> actions
        }
    }

    // 获取特定管理员的操作历史
    suspend fun getActionsByAdmin(adminId: String): Result<List<AdminAction>> {
        return try {
            val snapshot = actionsRef
                .whereEqualTo("Admin_ID", adminId)
                .orderBy("Timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val actions = snapshot.documents.mapNotNull { doc ->
                try {
                    AdminAction(
                        id = doc.id,
                        actionType = doc.getString("Action_Type") ?: "",
                        projectId = doc.getString("Project_ID") ?: "",
                        projectTitle = doc.getString("Project_Title") ?: "Unknown Project",
                        adminEmail = doc.getString("Admin_Email") ?: "Unknown Admin",
                        description = doc.getString("Description") ?: "",
                        timestamp = doc.getTimestamp("Timestamp"),
                        additionalInfo = doc.getString("Additional_Info") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(actions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 获取特定项目的操作历史
    suspend fun getActionsByProject(projectId: String): Result<List<AdminAction>> {
        return try {
            val snapshot = actionsRef
                .whereEqualTo("Project_ID", projectId)
                .orderBy("Timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val actions = snapshot.documents.mapNotNull { doc ->
                try {
                    AdminAction(
                        id = doc.id,
                        actionType = doc.getString("Action_Type") ?: "",
                        projectId = doc.getString("Project_ID") ?: "",
                        projectTitle = doc.getString("Project_Title") ?: "Unknown Project",
                        adminEmail = doc.getString("Admin_Email") ?: "Unknown Admin",
                        description = doc.getString("Description") ?: "",
                        timestamp = doc.getTimestamp("Timestamp"),
                        additionalInfo = doc.getString("Additional_Info") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(actions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}