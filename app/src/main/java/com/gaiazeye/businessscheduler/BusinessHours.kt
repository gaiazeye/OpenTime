package com.gaiazeye.businessscheduler

data class BusinessHours(
    val dayOfWeek: Int, // java.util.Calendar.MONDAY, etc.
    var startTime: String = "09:00 AM",
    var endTime: String = "05:00 PM",
    var isClosed: Boolean = false
)
