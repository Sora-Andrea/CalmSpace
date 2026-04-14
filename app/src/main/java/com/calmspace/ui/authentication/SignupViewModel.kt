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
        // TODO: re-enable Firebase auth when backend is ready
        _signupState.value = SignupState.Success("dev_user")
    }

    fun resetState() {
        _signupState.value = SignupState.Idle
    }
}