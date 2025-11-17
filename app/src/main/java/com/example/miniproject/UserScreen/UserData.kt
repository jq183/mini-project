package com.example.miniproject.UserScreen

data class User(
    val userId: String = "",
    val email: String = "",
    val phone: String = "",
    val userType: String = "user", // "user" 或 "admin"
    val createdAt: Long = System.currentTimeMillis()
)