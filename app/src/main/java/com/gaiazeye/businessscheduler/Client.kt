package com.gaiazeye.businessscheduler

import java.util.Locale

data class Client(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val business: String,
    val notes: String
) {
    fun getLastName(): String {
        val parts = name.trim().split("\\s+".toRegex())
        return if (parts.size > 1) parts.last() else parts.firstOrNull() ?: ""
    }
}
