package com.calmspace.ui.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// Welcome Screen
// Entry point before Login / Signup
// ─────────────────────────────────────────────

@Composable
fun welcomeScreen(
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(48.dp))

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

        Spacer(modifier = Modifier.height(48.dp))

        // ───────── Welcome Text ─────────
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── Login Button ─────────
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onLoginClick
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ───────── Signup Button ─────────
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSignupClick
        ) {
            Text("Sign Up")
        }
    }
}
