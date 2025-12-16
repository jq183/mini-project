package com.example.miniproject.repository

import android.net.Uri
import com.example.miniproject.UserScreen.Project
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
            "Title" to project.title,
            "Description" to project.description,
            "Category" to project.category,
            "creatorName" to project.creatorName,
            "Current_Amount" to 0.0,
            "Target_Amount" to project.goalAmount,
            "backers" to 0,
            "dueDate" to project.dueDate,
            "ImageUrl" to project.ImageUrl,
            "Status" to "active",
            "createdAt" to Timestamp.now(),
            "User_ID" to project.creatorId,
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
                        val updatedProject = project.copy(ImageUrl = downloadUrl.toString())
                        createProject(updatedProject, onSuccess, onError)
                    }
                    .addOnFailureListener { onError(it) }
            }
            .addOnFailureListener { onError(it) }
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
                            dueDate = doc.getTimestamp("dueDate"), // This will auto-calculate daysLeft
                            ImageUrl = doc.getString("ImageUrl") ?: "", // Fixed: was "imageUrl", now "ImageUrl"
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
                            dueDate = doc.getTimestamp("dueDate"),
                            ImageUrl = doc.getString("ImageUrl") ?: "", // Fixed: was "imageUrl", now "ImageUrl"
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

    fun checkAndUpdateExpiredProjects() {
        val db = FirebaseFirestore.getInstance()
        val today = Date()

        db.collection("projects")
            .whereEqualTo("Status", "active")
            .get()
            .addOnSuccessListener { documents ->
                documents.forEach { doc ->
                    val dueDate = doc.getTimestamp("dueDate")?.toDate()

                    if (dueDate != null && today.after(dueDate)) {
                        doc.reference.update(
                            mapOf(
                                "Status" to "expired"
                            )
                        )
                    }
                }
            }
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

    fun updateProjectWithImage(
        projectId: String,
        updates: MutableMap<String, Any>,
        newImageUri: Uri?,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (newImageUri != null) {
            // If there's a new image, upload it first
            val filePath = "project_images/${UUID.randomUUID()}.jpg"
            storage.child(filePath).putFile(newImageUri)
                .addOnSuccessListener {
                    storage.child(filePath).downloadUrl
                        .addOnSuccessListener { downloadUrl ->
                            updates["ImageUrl"] = downloadUrl.toString() // Standardized uppercase 'I'
                            updateProject(projectId, updates, onSuccess, onError)
                        }
                        .addOnFailureListener { onError(it) }
                }
                .addOnFailureListener { onError(it) }
        } else {
            updateProject(projectId, updates, onSuccess, onError)
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