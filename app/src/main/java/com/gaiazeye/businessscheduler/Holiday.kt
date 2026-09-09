package com.gaiazeye.businessscheduler

data class Holiday(
    val date: String, // MM/dd/yyyy
    val name: String,
    var isClosed: Boolean = true
)
