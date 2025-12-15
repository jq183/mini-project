package com.example.miniproject.repository

enum class Payments{
    OnlineBanking,
    TnG,
    Wallet,
    Unset
}

data class Donation (
    val donation_id: String = "",
    val id: String = "",
    val project_id: String = "",
    val amount: Double = 0.0,
    val paymentMethod: Payments = Payments.Unset,
    val isAnonymous: Boolean = false,
    val createdTime: String = "",
)

