package com.calmspace.ui.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(32.dp))

        // ───────── App Title ─────────
        Text(
            text = "CalmSpace",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ───────── Tagline ─────────
        Text(
            text = "Bringing a little more peace\nand quiet to the world",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ───────── Screen Title ─────────
        Text(
            text = "Login",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Welcome back!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── Email Input ─────────
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Enter Your Username / Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ───────── Password Input ─────────
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Enter Your Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── Login Action ─────────
        Button(
            onClick = {
                // TODO:
                // - Validate inputs
                // - Authenticate user
                // - Handle success / error
                onLogin(email, password)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ───────── Back Navigation ─────────
        TextButton(
            onClick = {
                // Navigate back to Welcome screen
                onBack()
            }
        ) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── Divider ─────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f))
            Text(
                text = "  Or  ",
                color = Color.Gray
            )
            Divider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── Social Login Placeholders ─────────
        OutlinedButton(
            onClick = {
                // TODO: Facebook login
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login with Facebook")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                // TODO: Google login
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login with Google")
        }
    }
}
