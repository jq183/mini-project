package com.example.miniproject.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class Report(
    val id: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val reportedBy: String = "",
    val reportCategory: String = "",
    val description: String = "",
    val status: String = "pending",
    val reportedAt: Timestamp? = null,
    val resolvedAt: Timestamp? = null,
    val adminNotes: String = "",
    val isAnonymous: Boolean = false
)

data class GroupedReport(
    val projectId: String,
    val projectTitle: String,
    val reports: List<Report>,
    val projectCategory: String,
    val totalReports: Int,
    val latestReport: Report,
    val categoryBreakdown: Map<String, Int>
) {
    val mostCommonCategory: String
        get() = categoryBreakdown.maxByOrNull { it.value }?.key ?: "Unknown"

    val status: String
        get() {
            val statuses = reports.map { it.status }
            return when {
                statuses.all { it == "resolved" } -> "resolved"
                statuses.all { it == "dismissed" } -> "dismissed"
                statuses.any { it == "pending" } -> "pending"
                else -> "pending"
            }
        }

    val pendingCount: Int
        get() = reports.count { it.status == "pending" }

    val resolvedCount: Int
        get() = reports.count { it.status == "resolved" }

    val dismissedCount: Int
        get() = reports.count { it.status == "dismissed" }
}

class ReportRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getAllReportsGrouped(): Result<List<GroupedReport>> {
        return try {
            val snapshot = db.collection("Reports").get().await()

            val projectIds = snapshot.documents.mapNotNull { it.getString("Project_ID") }.distinct()

            val projectTitles = mutableMapOf<String, String>()
            projectIds.forEach { projectId ->
                val projectDoc = db.collection("projects").document(projectId).get().await()
                projectTitles[projectId] = projectDoc.getString("Title") ?: "Unknown Project"
            }

            val reports = snapshot.documents.mapNotNull { doc ->
                try {
                    val projectId = doc.getString("Project_ID") ?: ""
                    Report(
                        id = doc.id,
                        projectId = projectId,
                        projectTitle = projectTitles[projectId] ?: "Unknown Project",
                        reportedBy = doc.getString("User_ID") ?: "Anonymous",
                        reportCategory = doc.getString("Category") ?: "",
                        description = doc.getString("Description") ?: "",
                        status = doc.getString("Status") ?: "pending",
                        reportedAt = doc.getTimestamp("Reported_at"),
                        resolvedAt = doc.getTimestamp("Resolved_at"),
                        adminNotes = doc.getString("Result") ?: "",
                        isAnonymous = doc.getBoolean("is_anonymous") ?: false
                    )
                } catch (e: Exception) {
                    null
                }
            }

            val groupedReports = groupReportsByProject(reports)
            Result.success(groupedReports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllReports(): Result<List<Report>> {
        return try {
            val snapshot = db.collection("Reports").get().await()

            val projectIds = snapshot.documents.mapNotNull { it.getString("Project_ID") }.distinct()

            val projectTitles = mutableMapOf<String, String>()
            projectIds.forEach { projectId ->
                val projectDoc = db.collection("projects").document(projectId).get().await()
                projectTitles[projectId] = projectDoc.getString("Title") ?: "Unknown Project"
            }

            val reports = snapshot.documents.mapNotNull { doc ->
                try {
                    val projectId = doc.getString("Project_ID") ?: ""
                    Report(
                        id = doc.id,
                        projectId = projectId,
                        projectTitle = projectTitles[projectId] ?: "Unknown Project",
                        reportedBy = doc.getString("User_ID") ?: "Anonymous",
                        reportCategory = doc.getString("Category") ?: "",
                        description = doc.getString("Description") ?: "",
                        status = doc.getString("Status") ?: "pending",
                        reportedAt = doc.getTimestamp("Reported_at"),
                        resolvedAt = doc.getTimestamp("Resolved_at"),
                        adminNotes = doc.getString("Result") ?: "",
                        isAnonymous = doc.getBoolean("is_anonymous") ?: false
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.reportedAt?.seconds ?: 0 }

            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun filterReportsByStatus(reports: List<Report>, status: String): List<Report> {
        if (status == "All") return reports
        return reports.filter { it.status.equals(status, ignoreCase = true) }
    }

    fun groupReportsByProject(reports: List<Report>): List<GroupedReport> {
        return reports
            .groupBy { it.projectId }
            .map { (projectId, projectReports) ->
                val categoryBreakdown = projectReports
                    .groupBy { it.reportCategory }
                    .mapValues { it.value.size }

                GroupedReport(
                    projectId = projectId,
                    projectTitle = projectReports.first().projectTitle,
                    reports = projectReports.sortedByDescending { it.reportedAt?.seconds ?: 0 },
                    projectCategory = "All",
                    totalReports = projectReports.size,
                    latestReport = projectReports.maxByOrNull { it.reportedAt?.seconds ?: 0 }!!,
                    categoryBreakdown = categoryBreakdown
                )
            }
            .sortedByDescending { it.totalReports }
    }


    suspend fun groupReportsByProjectWithCategory(reports: List<Report>): List<GroupedReport> {
        return reports
            .groupBy { it.projectId }
            .map { (projectId, projectReports) ->

                var projectCategory = "All"
                try {
                    val projectDoc = db.collection("projects").document(projectId).get().await()
                    projectCategory = projectDoc.getString("Category") ?: "All"
                } catch (e: Exception) {

                    android.util.Log.e("ReportRepository", "Failed to fetch category for project $projectId", e)
                }

                val categoryBreakdown = projectReports
                    .groupBy { it.reportCategory }
                    .mapValues { it.value.size }

                GroupedReport(
                    projectId = projectId,
                    projectTitle = projectReports.first().projectTitle,
                    projectCategory = projectCategory,
                    reports = projectReports.sortedByDescending { it.reportedAt?.seconds ?: 0 },
                    totalReports = projectReports.size,
                    latestReport = projectReports.maxByOrNull { it.reportedAt?.seconds ?: 0 }!!,
                    categoryBreakdown = categoryBreakdown
                )
            }
            .sortedByDescending { it.totalReports }
    }

    suspend fun updateReportStatus(
        reportId: String,
        status: String,
        adminNotes: String = ""
    ): Result<Boolean> {
        return try {
            val updates = mutableMapOf<String, Any>("Status" to status)
            if (adminNotes.isNotEmpty()) updates["Result"] = adminNotes
            if (status == "resolved" || status == "dismissed") {
                updates["Resolved_at"] = Timestamp.now()
            }

            db.collection("Reports").document(reportId).update(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAllReportsForProject(
        projectId: String,
        status: String,
        adminNotes: String = ""
    ): Result<Boolean> {
        return try {
            val snapshot = db.collection("Reports")
                .whereEqualTo("Project_ID", projectId)
                .get()
                .await()

            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                val updates = mutableMapOf<String, Any>("Status" to status)
                if (adminNotes.isNotEmpty()) updates["Result"] = adminNotes
                if (status == "resolved" || status == "dismissed") {
                    updates["Resolved_at"] = Timestamp.now()
                }
                batch.update(doc.reference, updates)
            }
            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePendingReportsForProject(
        projectId: String,
        status: String,
        adminNotes: String = ""
    ): Result<Boolean> {
        return try {
            val snapshot = db.collection("Reports")
                .whereEqualTo("Project_ID", projectId)
                .whereEqualTo("Status", "pending")
                .get()
                .await()

            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                val updates = mutableMapOf<String, Any>("Status" to status)
                if (adminNotes.isNotEmpty()) updates["Result"] = adminNotes
                if (status == "resolved" || status == "dismissed") {
                    updates["Resolved_at"] = Timestamp.now()
                }
                batch.update(doc.reference, updates)
            }
            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReportsForProject(projectId: String): Result<List<Report>> {
        return try {
            val projectDoc = db.collection("projects").document(projectId).get().await()
            val projectTitle = projectDoc.getString("Title") ?: "Unknown Project"

            val snapshot = db.collection("Reports")
                .whereEqualTo("Project_ID", projectId)
                .get()
                .await()

            val reports = if (snapshot.isEmpty) {
                db.collection("Reports")
                    .whereEqualTo("Project_ID", "")
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        try {
                            Report(
                                id = doc.id,
                                projectId = doc.getString("Project_ID") ?: "",
                                projectTitle = projectTitle,
                                reportedBy = doc.getString("User_ID")?.takeIf { it.isNotEmpty() } ?: "Anonymous",
                                reportCategory = doc.getString("Category") ?: "",
                                description = doc.getString("Description") ?: "",
                                status = doc.getString("Status") ?: "pending",
                                reportedAt = doc.getTimestamp("Reported_at"),
                                resolvedAt = doc.getTimestamp("Resolved_at"),
                                adminNotes = doc.getString("Result") ?: "",
                                isAnonymous = doc.getBoolean("is_anonymous") ?: false
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
            } else {
                snapshot.documents.mapNotNull { doc ->
                    try {
                        Report(
                            id = doc.id,
                            projectId = doc.getString("Project_ID") ?: "",
                            projectTitle = projectTitle,
                            reportedBy = doc.getString("User_ID")?.takeIf { it.isNotEmpty() } ?: "Anonymous",
                            reportCategory = doc.getString("Category") ?: "",
                            description = doc.getString("Description") ?: "",
                            status = doc.getString("Status") ?: "pending",
                            reportedAt = doc.getTimestamp("Reported_at"),
                            resolvedAt = doc.getTimestamp("Resolved_at"),
                            adminNotes = doc.getString("Result") ?: "",
                            isAnonymous = doc.getBoolean("is_anonymous") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            val sortedReports = reports.sortedByDescending { it.reportedAt?.seconds ?: 0 }

            Result.success(sortedReports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasNewPendingReports(projectId: String): Result<Boolean> {
        return try {
            val snapshot = db.collection("Reports")
                .whereEqualTo("Project_ID", projectId)
                .whereEqualTo("Status", "pending")
                .limit(1)
                .get()
                .await()

            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addReport(report: Report): Result<Boolean> {
        return try {
            val newReport = hashMapOf<String, Any?>(
                "Admin_ID" to report.reportedBy.takeIf { false },
                "Category" to report.reportCategory,
                "Description" to report.description,
                "Project_ID" to report.projectId,
                "Report_ID" to report.id,
                "Reported_at" to (report.reportedAt ?: Timestamp.now()),
                "Resolved_at" to report.resolvedAt,
                "Result" to report.adminNotes,
                "Status" to report.status,
                "User_ID" to report.reportedBy,
                "is_anonymous" to report.isAnonymous
            )

            db.collection("Reports")
                .document(report.id)
                .set(newReport)
                .await()

            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("ReportRepository", "Failed to add report", e)
            Result.failure(e)
        }
    }

    suspend fun getNextReportId(): String {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("Reports")
            .orderBy("Report_ID", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val lastId = snapshot.documents.firstOrNull()?.getString("Report_ID") ?: "R000"
        val lastNumber = lastId.removePrefix("R").toIntOrNull() ?: 0
        val nextNumber = lastNumber + 1
        return "R" + nextNumber.toString().padStart(3, '0')
    }
}