package com.example.miniproject.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.UUID

enum class Payments {
    OnlineBanking,
    TnG,
    Wallet,
    Unset
}

// Updated Data Class to match your Schema
data class Donation(
    val donation_id: String = "",
    val project_id: String = "",
    val user_id: String = "", // From Auth
    val amount: Double = 0.0,
    val paymentMethod: Payments = Payments.Unset,
    val isAnonymous: Boolean = false,
    val created_at: Timestamp = Timestamp.now()
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
        // 1. Generate a unique Donation ID (UUID)
        val generatedId = UUID.randomUUID().toString()
        val finalDonation = donation.copy(donation_id = generatedId)

        val donationMap = hashMapOf(
            "Donation_ID" to finalDonation.donation_id,
            "Project_ID" to finalDonation.project_id,
            "User_ID" to finalDonation.user_id,
            "Amount" to finalDonation.amount,
            "Payment_method" to finalDonation.paymentMethod.name, // Store as String enum
            "is_anonymous" to finalDonation.isAnonymous,
            "Created_at" to finalDonation.created_at
        )

        db.runBatch { batch ->
            // A. Create the Donation Record
            val docRef = donationsRef.document(generatedId)
            batch.set(docRef, donationMap)

            // B. Update the Project's Current Amount and Backer count
            // Note: In a real app, use FieldValue.increment to avoid race conditions
            val projectDoc = projectsRef.document(finalDonation.project_id)
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
            .whereEqualTo("User_ID", userId)
            .orderBy("Created_at", Query.Direction.DESCENDING) // Latest first
            .get()
            .addOnSuccessListener { snapshot ->
                val donations = snapshot.documents.mapNotNull { doc ->
                    try {
                        Donation(
                            donation_id = doc.getString("Donation_ID") ?: "",
                            project_id = doc.getString("Project_ID") ?: "",
                            user_id = doc.getString("User_ID") ?: "",
                            amount = doc.getDouble("Amount") ?: 0.0,
                            paymentMethod = try {
                                Payments.valueOf(doc.getString("Payment_method") ?: "Unset")
                            } catch (e: Exception) { Payments.Unset },
                            isAnonymous = doc.getBoolean("is_anonymous") ?: false,
                            created_at = doc.getTimestamp("Created_at") ?: Timestamp.now()
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