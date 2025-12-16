package com.example.miniproject.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlin.random.Random

enum class Payments {
    OnlineBanking,
    TnG,
    Wallet,
    Unset
}

// Updated Data Class to match your exact Firebase Schema
data class Donation(
    val id: String = "",           // Matches "id"
    val projectId: String = "",    // Matches "projectId"
    val projectTitle: String = "", // Matches "projectTitle"
    val userId: String = "",       // Matches "userId"
    val amount: Double = 0.0,      // Matches "amount"
    val status: String = "completed", // Matches "status"
    val paymentMethod: Payments = Payments.Unset, // Kept for logic, usually stored as string
    val isAnonymous: Boolean = false,
    val donatedAt: Timestamp = Timestamp.now() // Matches "donatedAt"
)

class DonationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val donationsRef = db.collection("donations")
    private val projectsRef = db.collection("projects")

    fun createDonation(
        donation: Donation,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // 1. Generate ID matching format "D1765794654721_523"
        val timestamp = System.currentTimeMillis()
        val randomNum = Random.nextInt(100, 999)
        val generatedId = "D${timestamp}_$randomNum"

        val finalDonation = donation.copy(id = generatedId)

        // 2. Map fields to match your Firebase structure exactly
        val donationMap = hashMapOf(
            "id" to finalDonation.id,
            "projectId" to finalDonation.projectId,
            "projectTitle" to finalDonation.projectTitle,
            "userId" to finalDonation.userId,
            "amount" to finalDonation.amount,
            "status" to finalDonation.status,
            "donatedAt" to finalDonation.donatedAt,
            // Additional fields needed for app logic
            "paymentMethod" to finalDonation.paymentMethod.name,
            "isAnonymous" to finalDonation.isAnonymous
        )

        db.runBatch { batch ->
            // A. Create the Donation Record
            val docRef = donationsRef.document(generatedId)
            batch.set(docRef, donationMap)

            // B. Update the Project's Current Amount and Backer count
            val projectDoc = projectsRef.document(finalDonation.projectId)
            batch.update(projectDoc, "Current_Amount", com.google.firebase.firestore.FieldValue.increment(finalDonation.amount))
            batch.update(projectDoc, "backers", com.google.firebase.firestore.FieldValue.increment(1))
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            onError(e)
        }
    }

    fun getDonationsByUserId(
        userId: String,
        onSuccess: (List<Donation>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        donationsRef
            .whereEqualTo("userId", userId) // Updated to camelCase
            .orderBy("donatedAt", Query.Direction.DESCENDING) // Updated to camelCase
            .get()
            .addOnSuccessListener { snapshot ->
                val donations = snapshot.documents.mapNotNull { doc ->
                    try {
                        Donation(
                            id = doc.getString("id") ?: "",
                            projectId = doc.getString("projectId") ?: "",
                            projectTitle = doc.getString("projectTitle") ?: "Unknown",
                            userId = doc.getString("userId") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            status = doc.getString("status") ?: "completed",
                            paymentMethod = try {
                                // Default to Unset if field is missing or invalid
                                Payments.valueOf(doc.getString("paymentMethod") ?: "Unset")
                            } catch (e: Exception) { Payments.Unset },
                            isAnonymous = doc.getBoolean("isAnonymous") ?: false,
                            donatedAt = doc.getTimestamp("donatedAt") ?: Timestamp.now()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                onSuccess(donations)
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}