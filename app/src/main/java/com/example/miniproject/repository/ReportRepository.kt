package com.example.miniproject.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
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
    val adminNotes: String = ""
)

data class GroupedReport(
    val projectId: String,
    val projectTitle: String,
    val reports: List<Report>,
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
}

class ReportRepository {
    private val db = FirebaseFirestore.getInstance()

    // 根据 admin 负责的类别获取报告
    suspend fun getReportsForAdmin(adminResponsible: List<String>): Result<List<Report>> {
        return try {
            android.util.Log.d("ReportRepository", "========== getReportsForAdmin START ==========")
            android.util.Log.d("ReportRepository", "Admin responsible project categories: $adminResponsible")

            if (adminResponsible.isEmpty()) {
                android.util.Log.w("ReportRepository", "adminResponsible is EMPTY! Returning empty list")
                return Result.success(emptyList())
            }

            // 1️⃣ 查出 admin 负责的项目
            val projectSnapshot = db.collection("projects")
                .whereIn("category", adminResponsible)
                .get()
                .await()

            val projectIds = projectSnapshot.documents.mapNotNull { it.id }
            android.util.Log.d("ReportRepository", "Project IDs found: $projectIds")

            if (projectIds.isEmpty()) {
                android.util.Log.w("ReportRepository", "No projects found for admin categories: $adminResponsible")
                return Result.success(emptyList())
            }

            // 2️⃣ 查对应 project_id 的所有 report
            val allReports = mutableListOf<Report>()
            for (pid in projectIds) {
                val snapshot = db.collection("reports")
                    .whereEqualTo("project_id", pid)
                    .get()
                    .await()

                android.util.Log.d("ReportRepository", "Reports found for project [$pid]: ${snapshot.size()}")

                snapshot.documents.mapNotNullTo(allReports) { doc ->
                    Report(
                        id = doc.id,
                        projectId = doc.getString("project_id") ?: "",
                        projectTitle = "", // 后面统一填 project title
                        reportedBy = doc.getString("user_id")?.takeIf { it.isNotEmpty() } ?: "Anonymous",
                        reportCategory = doc.getString("category") ?: "",
                        description = doc.getString("description") ?: "",
                        status = doc.getString("status") ?: "pending",
                        reportedAt = doc.getTimestamp("reported_at"),
                        resolvedAt = doc.getTimestamp("resolved_at"),
                        adminNotes = doc.getString("result") ?: ""
                    )
                }
            }

            android.util.Log.d("ReportRepository", "Total reports collected: ${allReports.size}")

            if (allReports.isEmpty()) {
                android.util.Log.w("ReportRepository", "⚠️ No reports found for admin's projects")
                return Result.success(emptyList())
            }

            // 3️⃣ 批量获取 project title
            val projectTitles = mutableMapOf<String, String>()
            projectIds.forEach { projectId ->
                val projectDoc = db.collection("projects").document(projectId).get().await()
                val title = projectDoc.getString("title")
                    ?: projectDoc.getString("projectTitle")
                    ?: "Unknown Project"
                projectTitles[projectId] = title
            }

            val reportsWithTitle = allReports.map { report ->
                report.copy(projectTitle = projectTitles[report.projectId] ?: "Unknown Project")
            }.sortedByDescending { it.reportedAt?.seconds ?: 0 }

            android.util.Log.d("ReportRepository", "Returning ${reportsWithTitle.size} reports with titles")
            android.util.Log.d("ReportRepository", "========== getReportsForAdmin END ==========")

            Result.success(reportsWithTitle)
        } catch (e: Exception) {
            android.util.Log.e("ReportRepository", "❌ ERROR in getReportsForAdmin", e)
            Result.failure(e)
        }
    }

    // 获取所有报告
    suspend fun getAllReports(): Result<List<Report>> {
        return try {
            val snapshot = db.collection("reports").get().await()

            val projectIds = snapshot.documents.mapNotNull { it.getString("project_id") }.distinct()

            val projectTitles = mutableMapOf<String, String>()
            projectIds.forEach { projectId ->
                val projectDoc = db.collection("projects").document(projectId).get().await()
                projectTitles[projectId] = projectDoc.getString("title")
                    ?: projectDoc.getString("projectTitle")
                            ?: "Unknown Project"
            }

            val reports = snapshot.documents.mapNotNull { doc ->
                try {
                    val projectId = doc.getString("project_id") ?: ""
                    Report(
                        id = doc.id,
                        projectId = projectId,
                        projectTitle = projectTitles[projectId] ?: "Unknown Project",
                        reportedBy = doc.getString("user_id") ?: "Anonymous",
                        reportCategory = doc.getString("category") ?: "",
                        description = doc.getString("description") ?: "",
                        status = doc.getString("status") ?: "pending",
                        reportedAt = doc.getTimestamp("reported_at"),
                        resolvedAt = doc.getTimestamp("resolved_at"),
                        adminNotes = doc.getString("result") ?: ""
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

    // 根据状态过滤报告
    fun filterReportsByStatus(reports: List<Report>, status: String): List<Report> {
        if (status == "All") return reports
        return reports.filter { it.status.equals(status, ignoreCase = true) }
    }

    // 将报告按项目分组
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
                    totalReports = projectReports.size,
                    latestReport = projectReports.maxByOrNull { it.reportedAt?.seconds ?: 0 }!!,
                    categoryBreakdown = categoryBreakdown
                )
            }
            .sortedByDescending { it.totalReports }
    }

    // 更新报告状态
    suspend fun updateReportStatus(
        reportId: String,
        status: String,
        adminNotes: String = ""
    ): Result<Boolean> {
        return try {
            val updates = mutableMapOf<String, Any>("status" to status)
            if (adminNotes.isNotEmpty()) updates["result"] = adminNotes
            if (status == "resolved") updates["resolved_at"] = Timestamp.now()

            db.collection("reports").document(reportId).update(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 批量更新项目的所有报告
    suspend fun updateAllReportsForProject(
        projectId: String,
        status: String,
        adminNotes: String = ""
    ): Result<Boolean> {
        return try {
            val snapshot = db.collection("reports").whereEqualTo("project_id", projectId).get().await()
            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                val updates = mutableMapOf<String, Any>("status" to status)
                if (adminNotes.isNotEmpty()) updates["result"] = adminNotes
                if (status == "resolved") updates["resolved_at"] = Timestamp.now()
                batch.update(doc.reference, updates)
            }
            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 获取特定项目的所有报告
    suspend fun getReportsForProject(projectId: String): Result<List<Report>> {
        return try {
            // 1️⃣ 先获取项目标题
            val projectDoc = db.collection("projects").document(projectId).get().await()
            val projectTitle = projectDoc.getString("title")
                ?: projectDoc.getString("projectTitle")
                ?: "Unknown Project"

            // 2️⃣ 查询 reports
            val snapshot = db.collection("reports")
                .whereEqualTo("project_id", projectId)
                .get()
                .await()

            // 3️⃣ 如果没找到，尝试获取 project_id 是空字符串的 report（兼容旧数据）
            val reports = if (snapshot.isEmpty) {
                db.collection("reports")
                    .whereEqualTo("project_id", "")
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        try {
                            Report(
                                id = doc.id,
                                projectId = doc.getString("project_id") ?: "",
                                projectTitle = projectTitle,
                                reportedBy = doc.getString("user_id")?.takeIf { it.isNotEmpty() } ?: "Anonymous",
                                reportCategory = doc.getString("category") ?: "",
                                description = doc.getString("description") ?: "",
                                status = doc.getString("status") ?: "pending",
                                reportedAt = doc.getTimestamp("reported_at"),
                                resolvedAt = doc.getTimestamp("resolved_at"),
                                adminNotes = doc.getString("result") ?: ""
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
                            projectId = doc.getString("project_id") ?: "",
                            projectTitle = projectTitle,
                            reportedBy = doc.getString("user_id")?.takeIf { it.isNotEmpty() } ?: "Anonymous",
                            reportCategory = doc.getString("category") ?: "",
                            description = doc.getString("description") ?: "",
                            status = doc.getString("status") ?: "pending",
                            reportedAt = doc.getTimestamp("reported_at"),
                            resolvedAt = doc.getTimestamp("resolved_at"),
                            adminNotes = doc.getString("result") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            // 4️⃣ 按 reportedAt 排序
            val sortedReports = reports.sortedByDescending { it.reportedAt?.seconds ?: 0 }

            Result.success(sortedReports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
