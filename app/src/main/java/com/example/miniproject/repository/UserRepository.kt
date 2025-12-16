package com.example.miniproject.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser

    fun addBalanceListener(onBalanceChange: (Double) -> Unit): ListenerRegistration? {
        val userId = currentUser?.uid ?: return null

        return db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) {
                    onBalanceChange(0.0) // Default to 0 if error or new user
                    return@addSnapshotListener
                }
                // We use "walletBalance" as the field name in Firestore
                val balance = snapshot.getDouble("walletBalance") ?: 0.0
                onBalanceChange(balance)
            }
    }

    fun topUpWallet(amount: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val userId = currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            // Get current, default to 0.0, then add
            val newBalance = (snapshot.getDouble("walletBalance") ?: 0.0) + amount
            transaction.update(userRef, "walletBalance", newBalance)
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener {
            onError(it.message ?: "Top up failed")
        }
    }

    fun deductWallet(amount: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val userId = currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentBalance = snapshot.getDouble("walletBalance") ?: 0.0

            if (currentBalance < amount) {
                throw com.google.firebase.firestore.FirebaseFirestoreException(
                    "Insufficient balance",
                    com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED
                )
            }
            transaction.update(userRef, "walletBalance", currentBalance - amount)
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener {
            onError(it.message ?: "Deduction failed")
        }
    }
}