package com.calmspace.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// Privacy Policy Screen
// Displays CalmSpace privacy policy
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy & Permissions",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {

            // ───────── App Logo/Name ─────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CalmSpace",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your privacy is important to us. Here's how we protect your data during sleep tracking.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ───────── Permission Cards ─────────
            
            // Microphone Access Card
            PermissionCard(
                title = "Microphone Access",
                description = "Microphone access is used to detect ambient noise during sleep sessions. " +
                        "All audio processing happens locally on your device. No audio is recorded, stored, or transmitted.",
                buttonText = "Allow Microphone",
                onButtonClick = { /* TODO: Request microphone permission */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Notifications Card
            PermissionCard(
                title = "Notifications",
                description = "Notifications help you sleep better by sending bedtime reminders, " +
                        "monitoring sleep sessions, and keeping you informed of important updates.",
                buttonText = "Allow Notifications",
                onButtonClick = { /* TODO: Request notification permission */ }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ───────── Privacy Sections ─────────
            
            PrivacySection(
                title = "Data Storage",
                content = "All sleep data is stored locally on your device. We do not upload your " +
                        "sleep recordings, audio data, or personal information to external servers."
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrivacySection(
                title = "Audio Processing",
                content = "Audio captured by the microphone is analyzed in real-time to detect " +
                        "ambient noise levels. The audio is never recorded or saved. Only noise " +
                        "level measurements are stored."
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrivacySection(
                title = "Third-Party Services",
                content = "CalmSpace does not share your data with third-party services or " +
                        "advertisers. Your sleep data remains private and secure."
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ───────── Done Button ─────────
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────
// Permission Card Component
// ─────────────────────────────────────────────

@Composable
fun PermissionCard(
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onButtonClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                )
            ) {
                Text(buttonText)
            }
        }
    }
}

// ─────────────────────────────────────────────
// Privacy Section Component
// ─────────────────────────────────────────────

@Composable
fun PrivacySection(
    title: String,
    content: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}