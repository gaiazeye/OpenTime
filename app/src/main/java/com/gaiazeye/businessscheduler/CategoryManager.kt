package com.gaiazeye.businessscheduler

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CategoryManager {
    var categories = mutableListOf<String>()

    private val gson = Gson()

    private fun getUserPrefsName(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        return "AppPrefs_${uid}"
    }

    fun init(context: Context) {
        loadCategories(context)
        if (categories.isEmpty()) {
            categories.addAll(
                listOf("Barbering", "Consultation", "Fitness", "Hair", "Makeup", "Massage", "Nails", "Skincare")
            )
            saveCategories(context)
        }
    }

    fun addCategory(category: String, context: Context) {
        val trimmed = category.trim()
        if (trimmed.isNotEmpty() && !categories.any { it.equals(trimmed, ignoreCase = true) }) {
            categories.add(trimmed)
            categories.sortWith(String.CASE_INSENSITIVE_ORDER)
            saveCategories(context)
        }
    }

    fun deleteCategory(category: String, context: Context) {
        categories.removeAll { it.equals(category, ignoreCase = true) }
        saveCategories(context)
    }

    fun saveCategories(context: Context) {
        val sharedPrefs = context.getSharedPreferences(getUserPrefsName(), Context.MODE_PRIVATE)
        val json = gson.toJson(categories)
        sharedPrefs.edit().putString("categories", json).apply()
    }

    private fun loadCategories(context: Context) {
        categories.clear()
        val sharedPrefs = context.getSharedPreferences(getUserPrefsName(), Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("categories", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<String>>() {}.type
            categories = gson.fromJson(json, type)
        }
    }
}
