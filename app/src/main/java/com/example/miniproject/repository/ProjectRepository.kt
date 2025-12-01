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
                            title = doc.getString("Title") ?: "",  // 改成 "Title"
                            description = doc.getString("Description") ?: "",  // 改成 "Description"
                            category = doc.getString("Category") ?: "",  // 改成 "Category"
                            creatorName = doc.getString("creatorName") ?: "",
                            creatorId = doc.getString("User_ID") ?: "",  // 添加这个
                            currentAmount = doc.getDouble("Current_Amount") ?: 0.0,  // 改成 "Current_Amount"
                            goalAmount = doc.getDouble("Target_Amount") ?: 0.0,  // 改成 "Target_Amount"
                            backers = doc.getLong("backers")?.toInt() ?: 0,
                            daysLeft = doc.getLong("daysLeft")?.toInt() ?: 0,
                            imageUrl = doc.getString("imageUrl") ?: "",
                            status = doc.getString("Status") ?: "active",  // 改成 "Status"
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
                            title = doc.getString("Title") ?: "",  // 改成 "Title"
                            description = doc.getString("Description") ?: "",  // 改成 "Description"
                            category = doc.getString("Category") ?: "",  // 改成 "Category"
                            creatorName = doc.getString("creatorName") ?: "",
                            creatorId = doc.getString("User_ID") ?: "",  // 添加这个
                            currentAmount = doc.getDouble("Current_Amount") ?: 0.0,  // 改成 "Current_Amount"
                            goalAmount = doc.getDouble("Target_Amount") ?: 0.0,  // 改成 "Target_Amount"
                            backers = doc.getLong("backers")?.toInt() ?: 0,
                            daysLeft = doc.getLong("daysLeft")?.toInt() ?: 0,
                            imageUrl = doc.getString("imageUrl") ?: "",
                            status = doc.getString("Status") ?: "active",  // 改成 "Status"
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
                    "Status" to "suspended",  // 改成 "Status"
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
            .update("Status", "deleted")  // 改成 "Status"
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