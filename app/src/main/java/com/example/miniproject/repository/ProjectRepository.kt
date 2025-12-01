package com.example.miniproject.repository

import com.example.miniproject.UserScreen.Project
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class ProjectRepository {
    private val db = FirebaseFirestore.getInstance()
    private val projectsRef = db.collection("projects")

    private fun generateProjectId(): String {
        val yearMonth = SimpleDateFormat("yyMM", Locale.getDefault()).format(Date())
        val random = (1000..9999).random()
        return "P$yearMonth$random"
    }

    fun getAllProjects(
        onSuccess: (List<Project>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        projectsRef
            .whereEqualTo("Status", "active")
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

    // Admin Functions
    fun verifyProject(
        projectId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        projectsRef
            .document(projectId)
            .update("isOfficial", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    fun dismissReport(
        projectId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        projectsRef
            .document(projectId)
            .update("isWarning", false)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    fun suspendProject(
        projectId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        projectsRef
            .document(projectId)
            .update(
                mapOf(
                    "status" to "suspended",
                    "isWarning" to true
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    fun deleteProject(
        projectId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        projectsRef
            .document(projectId)
            .update("status", "deleted")
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
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