package com.calmspace.ui.authentication

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class SignupState {
    object Idle : SignupState()
    object Loading : SignupState()
    data class Success(val userId: String) : SignupState()
    data class Error(val message: String) : SignupState()
}

class SignupViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _signupState = mutableStateOf<SignupState>(SignupState.Idle)
    val signupState = _signupState

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            _signupState.value = SignupState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                _signupState.value = SignupState.Success(user?.uid ?: "")
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                        "This email is already registered."
                    is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                        "Password should be at least 6 characters."
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                        "Invalid email format."
                    else -> "Sign-up failed: ${e.message}"
                }
                _signupState.value = SignupState.Error(errorMessage)
            }
        }
    }

    fun resetState() {
        _signupState.value = SignupState.Idle
    }
}