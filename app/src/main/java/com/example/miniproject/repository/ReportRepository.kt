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
    val adminNotes: String = "",
    val isAnonymous: Boolean = false
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

    // 获取待处理报告数量
    val pendingCount: Int
        get() = reports.count { it.status == "pending" }

    // 获取已解决报告数量
    val resolvedCount: Int
        get() = reports.count { it.status == "resolved" }

    // 获取已忽略报告数量
    val dismissedCount: Int
        get() = reports.count { it.status == "dismissed" }
}

class ReportRepository {
    private val db = FirebaseFirestore.getInstance()

    // 获取所有报告并自动按项目分组
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

    // 获取所有报告（未分组）
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

    // 更新单个报告状态
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

    // 批量更新项目的所有报告（处理整个项目）
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

    // 智能批量更新：只更新待处理的报告，保持已处理报告的状态
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

    // 获取特定项目的所有报告
    suspend fun getReportsForProject(projectId: String): Result<List<Report>> {
        return try {
            // 1️⃣ 先获取项目标题
            val projectDoc = db.collection("projects").document(projectId).get().await()
            val projectTitle = projectDoc.getString("Title") ?: "Unknown Project"

            // 2️⃣ 查询 Reports
            val snapshot = db.collection("Reports")
                .whereEqualTo("Project_ID", projectId)
                .get()
                .await()

            // 3️⃣ 如果没找到,尝试获取 Project_ID 是空字符串的 report(兼容旧数据)
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

            // 4️⃣ 按 reportedAt 排序
            val sortedReports = reports.sortedByDescending { it.reportedAt?.seconds ?: 0 }

            Result.success(sortedReports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 检查项目是否有新的待处理报告
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
}