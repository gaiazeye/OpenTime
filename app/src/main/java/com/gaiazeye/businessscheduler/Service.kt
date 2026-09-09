package com.gaiazeye.businessscheduler

data class Service(
    val id: String,
    val name: String,
    val category: String,
    val durationMinutes: Int,
    val price: Double,
    val description: String
)
