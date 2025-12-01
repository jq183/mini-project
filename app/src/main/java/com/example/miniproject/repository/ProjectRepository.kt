package com.example.miniproject.repository

import com.example.miniproject.UserScreen.Project
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProjectRepository {
    private val db = FirebaseFirestore.getInstance()
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
                        Project(
                            id = doc.id,
                            title = doc.getString("Title") ?: "",
                            description = doc.getString("Description") ?: "",
                            category = doc.getString("Category") ?: "",
                            creatorName = doc.getString("creatorName") ?: "",
                            currentAmount = doc.getDouble("Current_Amount") ?: 0.0,
                            goalAmount = doc.getDouble("Target_Amount") ?: 0.0,
                            backers = doc.getLong("backers")?.toInt() ?: 0,
                            daysLeft = doc.getLong("daysLeft")?.toInt() ?: 0,
                            imageUrl = doc.getString("imageUrl") ?: "",
                            status = doc.getString("Status") ?: "active",
                            createdAt = doc.getTimestamp("createdAt"),
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
                        val project = Project(
                            id = doc.id,
                            title = doc.getString("Title") ?: "",
                            description = doc.getString("Description") ?: "",
                            category = doc.getString("Category") ?: "",
                            creatorName = doc.getString("creatorName") ?: "",
                            currentAmount = doc.getDouble("Current_Amount") ?: 0.0,
                            goalAmount = doc.getDouble("Target_Amount") ?: 0.0,
                            backers = doc.getLong("backers")?.toInt() ?: 0,
                            daysLeft = doc.getLong("daysLeft")?.toInt() ?: 0,
                            imageUrl = doc.getString("imageUrl") ?: "",
                            status = doc.getString("Status") ?: "active",
                            createdAt = doc.getTimestamp("createdAt"),
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

            // 记录操作历史
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

            // 记录操作历史
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

            // 记录操作历史
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

            // 记录操作历史
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

            // 记录操作历史
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