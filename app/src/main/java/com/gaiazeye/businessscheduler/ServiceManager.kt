package com.gaiazeye.businessscheduler

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ServiceManager {
    var services = mutableListOf<Service>()

    private fun getUserPrefsName(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        return "AppPrefs_${uid}"
    }

    fun sortServices() {
        services.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    fun saveServices(context: Context) {
        sortServices()
        val sharedPrefs = context.getSharedPreferences(getUserPrefsName(), Context.MODE_PRIVATE)
        val json = Gson().toJson(services)
        sharedPrefs.edit().putString("services", json).apply()
    }

    fun updateService(updatedService: Service, context: Context) {
        val index = services.indexOfFirst { it.id == updatedService.id }
        if (index != -1) {
            services[index] = updatedService
            saveServices(context)
        }
    }

    fun deleteService(id: String, context: Context) {
        services.removeAll { it.id == id }
        saveServices(context)
    }

    fun loadServices(context: Context) {
        val sharedPrefs = context.getSharedPreferences(getUserPrefsName(), Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("services", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Service>>() {}.type
            services = Gson().fromJson(json, type)
        } else {
            services.clear()
            services.addAll(
                listOf(
                    Service("1", "Hair Cut & Style", "Hair", 45, 45.0, "Standard cut and blow dry"),
                    Service("2", "Gel Manicure", "Nails", 45, 35.0, "Gel nail polish and cuticle care"),
                    Service("3", "Deep Tissue Massage", "Massage", 60, 80.0, "60 minute full body massage")
                )
            )
            saveServices(context)
        }
        sortServices()
    }
}
