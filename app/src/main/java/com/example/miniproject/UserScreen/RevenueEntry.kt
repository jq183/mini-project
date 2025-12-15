package com.example.miniproject.UserScreen

data class RevenueEntry(
    val label: String,        // e.g. "Day 1", "Week 1", "Jan"
    val amount: Float,        // Revenue amount for this period
    val timestamp: Long? = null // Optional: Unix time (for sorting later)
)
