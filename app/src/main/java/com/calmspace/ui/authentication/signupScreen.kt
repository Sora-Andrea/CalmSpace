package com.calmspace.ui.authentication

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calmspace.ui.components.AppIcons

@Composable
fun SignupScreen(
    onBackToLogin: () -> Unit,
    onSignupSuccess: () -> Unit,
    viewModel: SignupViewModel = viewModel()
) {
    val signupState by viewModel.signupState
    val context = LocalContext.current

    var username        by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(signupState) {
        when (val state = signupState) {
            is SignupState.Success -> {
                context.getSharedPreferences("calmspace_prefs", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putString("user_name",     username.trim())
                    .putString("user_email",    email.trim())
                    .putString("user_initials", username.trim().take(2).uppercase())
                    .putBoolean("logged_in",    true)
                    .apply()
                onSignupSuccess()
                viewModel.resetState()
            }
            is SignupState.Error -> { Toast.makeText(context, state.message, Toast.LENGTH_LONG).show(); viewModel.resetState() }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(48.dp))

        // ───────── Icon ─────────
        Icon(
            imageVector        = AppIcons.Sleep,
            contentDescription = null,
            modifier           = Modifier.size(44.dp),
            tint               = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ───────── Header ─────────
        Text(
            text       = "Create Account",
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text      = "Start your sleep journey today",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        // ───────── Username ─────────
        OutlinedTextField(
            value         = username,
            onValueChange = { username = it },
            label         = { Text("Username") },
            placeholder   = { Text("Your display name") },
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(12.dp),
            singleLine    = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ───────── Email ─────────
        OutlinedTextField(
            value         = email,
            onValueChange = { email = it },
            label         = { Text("Email") },
            placeholder   = { Text("you@example.com") },
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(12.dp),
            singleLine    = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ───────── Password ─────────
        OutlinedTextField(
            value         = password,
            onValueChange = { password = it },
            label         = { Text("Password") },
            placeholder   = { Text("Min. 8 characters") },
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(12.dp),
            singleLine    = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon  = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector        = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ───────── Create Account button ─────────
        Button(
            onClick = {
                if (username.isBlank() || email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                viewModel.signUp(email, password, username)
            },
            enabled  = signupState !is SignupState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (signupState is SignupState.Loading) {
                CircularProgressIndicator(
                    color       = MaterialTheme.colorScheme.onPrimary,
                    modifier    = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text       = "Create Account",
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ───────── Divider ─────────
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Text(
                text  = "  or continue with  ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ───────── Social buttons ─────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick  = { /* TODO: Google signup */ },
                modifier = Modifier.weight(1f).height(48.dp),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("Google", fontWeight = FontWeight.Medium)
            }
            OutlinedButton(
                onClick  = { /* TODO: Facebook signup */ },
                modifier = Modifier.weight(1f).height(48.dp),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("Facebook", fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ───────── Sign in link ─────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text  = "Already have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            TextButton(onClick = onBackToLogin) {
                Text(
                    text       = "Sign In",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
