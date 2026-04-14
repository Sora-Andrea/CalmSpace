package com.calmspace.ui.authentication

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val userId: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _loginState = mutableStateOf<LoginState>(LoginState.Idle)
    val loginState = _loginState

    fun login(email: String, password: String) {
        // TODO: re-enable Firebase auth when backend is ready
        _loginState.value = LoginState.Success("dev_user")
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}