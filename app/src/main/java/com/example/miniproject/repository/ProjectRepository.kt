package com.example.miniproject.repository

import android.net.Uri
import com.example.miniproject.UserScreen.Project
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.UUID



class ProjectRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private val projectsRef = db.collection("projects")
    private val certificationsRef = db.collection("Certifications")
    private val actionRepository = AdminActionRepository()

    private fun generateProjectId(): String {
        val yearMonth = SimpleDateFormat("yyMM", Locale.getDefault()).format(Date())
        val random = (1000..9999).random()
        return "P$yearMonth$random"
    }

    private suspend fun generateCertificationId(): String {
        val snapshot = certificationsRef.get().await()
        val existingIds = snapshot.documents.map { it.id }

        var counter = 1
        var newId = "C${counter.toString().padStart(3, '0')}"

        while (existingIds.contains(newId)) {
            counter++
            newId = "C${counter.toString().padStart(3, '0')}"
        }

        return newId
    }

    fun createProject(
        project: Project,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val projectId = generateProjectId()

        val data = hashMapOf(
            "Category" to project.category,
            "CurrentAmount" to 0.0,
            "Description" to project.description,
            "Status" to "active",
            "Target_Amount" to project.goalAmount,
            "Title" to project.title,
            "User_ID" to project.creatorId,
            "backers" to 0,
            "createdAt" to Timestamp.now(),
            "creatorName" to project.creatorName,
            "dueDate" to project.dueDate,
            "imageUrl" to project.imageUrl,
            "isOfficial" to false,
            "isWarning" to false,
            "isComplete" to false
        )

        projectsRef.document(projectId)
            .set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun uploadImageAndCreateProject(
        imageUri: Uri,
        project: Project,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val filePath = "project_images/${UUID.randomUUID()}.jpg"

        storage.child(filePath).putFile(imageUri)
            .addOnSuccessListener {
                storage.child(filePath).downloadUrl
                    .addOnSuccessListener { downloadUrl ->
                        val updatedProject = project.copy(imageUrl = downloadUrl.toString())
                        createProject(updatedProject, onSuccess, onError)
                    }
                    .addOnFailureListener { onError(it) }
            }
            .addOnFailureListener { onError(it) }
    }

    private fun calculateDaysLeft(dueDateString: String): Int {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dueDate = dateFormat.parse(dueDateString)!!

            val today = Date()

            // 计算时间差 (毫秒)
            val diff = dueDate.time - today.time

            // 转换为天数
            val days = (diff / (1000 * 60 * 60 * 24)).toInt()

            // 如果项目已经过期，返回 0
            if (days < 0) 0 else days

        } catch (e: Exception) {
            0
        }
    }

    fun getAllProjects(
        onSuccess: (List<Project>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val statuses = listOf("active", "suspended")
        projectsRef
            .whereIn("Status", statuses)
            .get()
            .addOnSuccessListener { snapshot ->
                val projects = snapshot.documents.mapNotNull { doc ->
                    try {

                        val dueDateString = doc.getString("dueDate") ?: ""

                        //cal dayleft
                        val remainingDays = if (dueDateString.isNotEmpty()) {
                            calculateDaysLeft(dueDateString)
                        } else {
                            -1
                        }

                        Project(
                            id = doc.id,
                            title = doc.getString("Title") ?: "",
                            description = doc.getString("Description") ?: "",
                            category = doc.getString("Category") ?: "",
                            creatorName = doc.getString("creatorName") ?: "",
                            currentAmount = doc.getDouble("Current_Amount") ?: 0.0,
                            goalAmount = doc.getDouble("Target_Amount") ?: 0.0,
                            backers = doc.getLong("backers")?.toInt() ?: 0,
                            daysLeft = remainingDays,
                            imageUrl = doc.getString("imageUrl") ?: "",
                            status = doc.getString("Status") ?: "active",
                            createdAt = doc.getTimestamp("createdAt"),
                            dueDate = dueDateString,
                            isOfficial = doc.getBoolean("isOfficial") ?: false,
                            isWarning = doc.getBoolean("isWarning") ?: false,
                            isComplete = doc.getBoolean("isComplete") ?: false
                        )
                    } catch (e: Exception) {
                        println("Error mapping project ${doc.id}: ${e.message}")
                        null
                    }
                }
                onSuccess(projects)
            }
            .addOnFailureListener(onError)
    }

    fun getProjectById(
        projectId: String,
        onSuccess: (Project) -> Unit,
        onError: (Exception) -> Unit
    ) {
        projectsRef
            .document(projectId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    try {

                        val dueDateString = doc.getString("dueDate") ?: ""

                        val remainingDays = if (dueDateString.isNotEmpty()) {
                            calculateDaysLeft(dueDateString)
                        } else {
                            -1
                        }

                        val project = Project(
                            id = doc.id,
                            title = doc.getString("Title") ?: "",
                            description = doc.getString("Description") ?: "",
                            category = doc.getString("Category") ?: "",
                            creatorName = doc.getString("creatorName") ?: "",
                            currentAmount = doc.getDouble("Current_Amount") ?: 0.0,
                            goalAmount = doc.getDouble("Target_Amount") ?: 0.0,
                            backers = doc.getLong("backers")?.toInt() ?: 0,
                            daysLeft = remainingDays,
                            imageUrl = doc.getString("imageUrl") ?: "",
                            status = doc.getString("Status") ?: "active",
                            createdAt = doc.getTimestamp("createdAt"),
                            dueDate = dueDateString,
                            isOfficial = doc.getBoolean("isOfficial") ?: false,
                            isWarning = doc.getBoolean("isWarning") ?: false,
                            isComplete = doc.getBoolean("isComplete") ?: false
                        )
                        onSuccess(project)
                    } catch (e: Exception) {
                        onError(Exception("Error parsing project data: ${e.message}"))
                    }
                } else {
                    onError(Exception("Project not found"))
                }
            }
            .addOnFailureListener(onError)
    }

    // Admin Functions with Action Logging
    suspend fun verifyProject(
        projectId: String,
        adminId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            projectsRef
                .document(projectId)
                .update("isOfficial", true)
                .await()

            val certificationId = generateCertificationId()

            val certificationData = hashMapOf(
                "Project_ID" to projectId,
                "Admin_ID" to adminId,
                "Created_at" to Timestamp.now()
            )

            certificationsRef
                .document(certificationId)
                .set(certificationData)
                .await()

            actionRepository.recordAction(
                actionType = "verified",
                projectId = projectId,
                adminId = adminId,
                description = "Project verified and certified",
                additionalInfo = "Certification ID: $certificationId"
            )

            onSuccess()
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun unverifyProject(
        projectId: String,
        adminId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            projectsRef
                .document(projectId)
                .update("isOfficial", false)
                .await()

            val certifications = certificationsRef
                .whereEqualTo("Project_ID", projectId)
                .get()
                .await()

            certifications.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            actionRepository.recordAction(
                actionType = "unverified",
                projectId = projectId,
                adminId = adminId,
                description = "Verification removed from project",
                additionalInfo = "Certification(s) deleted"
            )

            onSuccess()
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun dismissReport(
        projectId: String,
        adminId: String,
        reportCount: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            projectsRef
                .document(projectId)
                .update("isWarning", false)
                .await()

            actionRepository.recordAction(
                actionType = "resolved",
                projectId = projectId,
                adminId = adminId,
                description = "Reports dismissed after review",
                additionalInfo = "$reportCount report(s) dismissed"
            )

            onSuccess()
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun suspendProject(
        projectId: String,
        adminId: String,
        reportCount: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            projectsRef
                .document(projectId)
                .update(
                    mapOf(
                        "Status" to "suspended",
                        "isWarning" to true
                    )
                )
                .await()

            actionRepository.recordAction(
                actionType = "suspended",
                projectId = projectId,
                adminId = adminId,
                description = "Project suspended due to reports",
                additionalInfo = "$reportCount report(s) resulted in suspension"
            )

            onSuccess()
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun deleteProject(
        projectId: String,
        adminId: String,
        reason: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            projectsRef
                .document(projectId)
                .update("Status", "cancelled")
                .await()

            actionRepository.recordAction(
                actionType = "deleted",
                projectId = projectId,
                adminId = adminId,
                description = "Project deleted by admin",
                additionalInfo = "Reason: $reason"
            )

            onSuccess()
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun updateProject(
        projectId: String,
        updates: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        projectsRef
            .document(projectId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }
}