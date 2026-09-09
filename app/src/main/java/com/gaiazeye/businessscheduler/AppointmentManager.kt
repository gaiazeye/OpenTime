package com.gaiazeye.businessscheduler

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Locale

object AppointmentManager {
    var appointments = mutableListOf<Appointment>()
    var selectedDateForAppointment: String? = null

    private fun getUserPrefsName(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        return "AppPrefs_${uid}"
    }

    fun saveAppointments(context: Context) {
        val sharedPrefs = context.getSharedPreferences(getUserPrefsName(), Context.MODE_PRIVATE)
        val json = Gson().toJson(appointments)
        sharedPrefs.edit().putString("appointments", json).apply()
    }

    fun deleteAppointment(id: String, context: Context) {
        appointments.removeAll { it.id == id }
        saveAppointments(context)
    }

    fun onClientUpdated(oldName: String, newName: String, context: Context) {
        if (oldName == newName || oldName.isEmpty()) return
        var updated = false
        for (appt in appointments) {
            if (appt.clientName.equals(oldName, ignoreCase = true)) {
                appt.clientName = newName
                updated = true
            }
        }
        if (updated) {
            saveAppointments(context)
        }
    }

    fun onServiceUpdated(oldName: String, newName: String, newPrice: Double?, context: Context) {
        if (oldName.isEmpty()) return
        var updated = false
        for (appt in appointments) {
            if (appt.serviceName.equals(oldName, ignoreCase = true)) {
                appt.serviceName = newName
                if (newPrice != null && newPrice > 0) {
                    appt.price = newPrice
                }
                updated = true
            }
        }
        if (updated) {
            saveAppointments(context)
        }
    }

    fun loadAppointments(context: Context) {
        val sharedPrefs = context.getSharedPreferences(getUserPrefsName(), Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("appointments", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Appointment>>() {}.type
            appointments = Gson().fromJson(json, type)
            sortAppointments()
        } else {
            appointments.clear()
        }
    }

    fun sortAppointments() {
        val formats = arrayOf(
            "MM/dd/yyyy h:mm a", "MM/dd/yyyy hh:mm a",
            "M/d/yyyy h:mm a", "M/d/yyyy hh:mm a",
            "MM/dd/yyyy H:mm", "MM/dd/yyyy HH:mm"
        )
        appointments.sortWith(compareBy { appt ->
            val combined = "${appt.date.trim()} ${appt.time.trim()}"
            var parsedTime = 0L
            for (fmt in formats) {
                try {
                    val time = SimpleDateFormat(fmt, Locale.getDefault()).parse(combined)?.time
                    if (time != null) {
                        parsedTime = time
                        break
                    }
                } catch (e: Exception) {}
            }
            parsedTime
        })
    }
}
