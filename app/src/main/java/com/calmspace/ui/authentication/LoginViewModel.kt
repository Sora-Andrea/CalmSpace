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
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                _loginState.value = LoginState.Success(user?.uid ?: "")
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                        "Invalid email or password."
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                        "No account found with this email."
                    else -> "Login failed: ${e.message}"
                }
                _loginState.value = LoginState.Error(errorMessage)
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}