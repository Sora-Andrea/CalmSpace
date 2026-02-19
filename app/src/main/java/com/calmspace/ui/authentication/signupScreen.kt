package com.calmspace.ui.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.background


import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SignupScreen(
    onBackToLogin: () -> Unit,
    onSignup: (String, String, String) -> Unit
) {

    // ─────────────────────────────────────────────
    // UI STATE (move to ViewModel later)
    // ─────────────────────────────────────────────
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // ─────────────────────────────────────────────
    // SCREEN LAYOUT
    // ─────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── APP TITLE ─────────
        Text(
            text = "CalmSpace",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── LOGO PLACEHOLDER ─────────
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = Color.LightGray,
                    shape = RoundedCornerShape(60.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("LOGO")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ───────── TAGLINE ─────────
        Text(
            text = "Bringing a little more peace\nand quiet to the world",
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── SCREEN TITLE ─────────
        Text(
            text = "Signup",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── USERNAME INPUT ─────────
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            placeholder = { Text("Enter Your Username") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ───────── EMAIL INPUT ─────────
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Enter Your Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ───────── PASSWORD INPUT ─────────
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Enter Your Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation =
                if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible }
                ) {
                    Icon(
                        imageVector =
                            if (passwordVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── SIGNUP ACTION ─────────
        Button(
            onClick = {
                // TODO:
                // - Validate username, email, password
                // - Create account via auth service
                // - Handle success & error states
                onSignup(username, email, password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Signup", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ───────── BACK TO LOGIN ─────────
        Row {
            Text(text = "Already have an account? ")
            TextButton(
                onClick = {
                    // TODO: Navigate back to login screen
                    onBackToLogin()
                }
            ) {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── OR DIVIDER ─────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text("  Or  ", color = Color.Gray)
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── FACEBOOK SIGNUP ─────────
        Button(
            onClick = {
                // TODO: Facebook signup
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Login with Facebook", color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ───────── GOOGLE SIGNUP ─────────
        OutlinedButton(
            onClick = {
                // TODO: Google signup
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Login with Google")
        }
    }
}
