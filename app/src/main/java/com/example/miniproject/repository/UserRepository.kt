package com.example.miniproject.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser

    // 1. Real-time Balance Listener
    fun addBalanceListener(onBalanceChange: (Double) -> Unit): ListenerRegistration? {
        val userId = currentUser?.uid ?: return null

        // UPDATED: Pointing to "wallet" collection
        return db.collection("wallet").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // UPDATED: Field is now "balance"
                    val balance = snapshot.getDouble("balance") ?: 0.0
                    onBalanceChange(balance)
                } else {
                    onBalanceChange(0.0)
                }
            }
    }

    // 2. Top Up (Matches the image structure)
    fun topUpWallet(amount: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val userId = currentUser?.uid ?: return
        // UPDATED: Pointing to "wallet" collection
        val walletRef = db.collection("wallet").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(walletRef)

            // UPDATED: Read "balance" instead of "walletBalance"
            val currentBalance = snapshot.getDouble("balance") ?: 0.0
            val newBalance = currentBalance + amount

            // UPDATED: Prepare data to match your screenshot
            // We save both 'balance' and 'userId'
            val data = hashMapOf(
                "balance" to newBalance,
                "userId" to userId
            )

            // Use SET with Merge. If doc exists, it updates fields.
            // If it doesn't exist, it creates it with these fields.
            transaction.set(walletRef, data, SetOptions.merge())
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener {
            onError(it.message ?: "Top up failed")
        }
    }

    // 3. Deduct Wallet
    fun deductWallet(amount: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val userId = currentUser?.uid ?: return
        val walletRef = db.collection("wallet").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(walletRef)
            val currentBalance = snapshot.getDouble("balance") ?: 0.0

            if (currentBalance < amount) {
                throw com.google.firebase.firestore.FirebaseFirestoreException(
                    "Insufficient balance",
                    com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED
                )
            }
            // UPDATED: Update "balance" field
            transaction.update(walletRef, "balance", currentBalance - amount)
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener {
            onError(it.message ?: "Deduction failed")
        }
    }
    
    fun initializeWallet() {
        val userId = currentUser?.uid ?: return
        val walletRef = db.collection("wallet").document(userId)

        walletRef.get().addOnSuccessListener { snapshot ->
            // Only create if it does NOT exist
            if (!snapshot.exists()) {
                val initialData = hashMapOf(
                    "balance" to 0.0,
                    "userId" to userId
                )
                // Create the document
                walletRef.set(initialData)
            }
        }
    }
}