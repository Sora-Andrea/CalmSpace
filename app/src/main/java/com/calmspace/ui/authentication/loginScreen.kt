package com.calmspace.ui.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun loginScreen(
    onBack: () -> Unit,
    onLogin: (String, String) -> Unit
) {

    // ─────────────────────────────────────────────
    // UI State (move to ViewModel later)
    // ─────────────────────────────────────────────
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // ─────────────────────────────────────────────
    // Layout
    // ─────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // ───────── Title ─────────
        Text("Login")

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── Email Input ─────────
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ───────── Password Input ─────────
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── Login Action ─────────
        Button(
            onClick = {
                // TODO:
                // - Validate inputs
                // - Call auth ViewModel / service
                // - Handle success & error
                onLogin(email, password)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ───────── Back Navigation ─────────
        Button(
            onClick = {
                // TODO:
                // Navigate back to Welcome screen
                onBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
