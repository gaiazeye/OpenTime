package com.gaiazeye.businessscheduler

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ClientManager {
    var clients = mutableListOf<Client>()

    private fun getUserPrefsName(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        return "AppPrefs_${uid}"
    }

    fun saveClients(context: Context) {
        val sharedPrefs = context.getSharedPreferences(getUserPrefsName(), Context.MODE_PRIVATE)
        val json = Gson().toJson(clients)
        sharedPrefs.edit().putString("clients", json).apply()
    }

    fun updateClient(updatedClient: Client, context: Context) {
        val index = clients.indexOfFirst { it.id == updatedClient.id }
        if (index != -1) {
            clients[index] = updatedClient
            saveClients(context)
        }
    }

    fun deleteClient(id: String, context: Context) {
        clients.removeAll { it.id == id }
        saveClients(context)
    }

    fun loadClients(context: Context) {
        val sharedPrefs = context.getSharedPreferences(getUserPrefsName(), Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("clients", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Client>>() {}.type
            clients = Gson().fromJson(json, type)
        } else {
            clients.clear()
        }
    }
}
