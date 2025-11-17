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

    fun createProject(
        project: Project,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val projectId = generateProjectId()

        val projectData = hashMapOf(
            "title" to project.title,
            "description" to project.description,
            "category" to project.category,
            "creatorName" to project.creatorName,
            "creatorId" to project.creatorId,
            "currentAmount" to project.currentAmount,
            "goalAmount" to project.goalAmount,
            "backers" to project.backers,
            "daysLeft" to project.daysLeft,
            "imageUrl" to project.imageUrl,
            "status" to "active",
            "createdAt" to Timestamp.now()
        )

        projectsRef
            .document(projectId)
            .set(projectData)
            .addOnSuccessListener {
                onSuccess(projectId)
            }
            .addOnFailureListener(onError)
    }

    fun getAllProjects(
        onSuccess: (List<Project>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        projectsRef
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { snapshot ->
                val projects = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Project::class.java)?.copy(id = doc.id)
                }
                onSuccess(projects)
            }
            .addOnFailureListener(onError)
    }

    fun getProjectId(
        projectId: String,
        onSuccess: (Project?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        projectsRef
            .document(projectId)
            .get()
            .addOnSuccessListener { doc ->
                val project = doc.toObject(Project::class.java)?.copy(id = doc.id)
                onSuccess(project)
            }
            .addOnFailureListener(onError)
    }
}
