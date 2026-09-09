package com.gaiazeye.businessscheduler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repo.login(email, password)
            onResult(result.isSuccess, result.exceptionOrNull()?.message)
        }
    }

    fun signup(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repo.signup(email, password)
            onResult(result.isSuccess, result.exceptionOrNull()?.message)
        }
    }

    fun logout() = repo.logout()
}
