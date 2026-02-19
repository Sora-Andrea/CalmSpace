package com.calmspace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calmspace.ui.components.AppIcons

// ─────────────────────────────────────────────
// Settings Screen
// User preferences, notifications, audio, general settings
// ─────────────────────────────────────────────

@Composable
fun SettingsScreen(
    onNavigateToPrivacyPolicy: () -> Unit = {}
) {

    // ─────────────────────────────────────────────
    // Placeholder State
    // TODO: Replace with ViewModel + Room database
    // ─────────────────────────────────────────────

    // TODO: Replace with real user data from Room
    val userName = "User"

    // Sleep Preferences
    // TODO: Wire to user preferences in Room
    var sleepGoal by remember { mutableStateOf("8 hours") }
    var bedtimeReminder by remember { mutableStateOf("10:00 PM") }
    var smartNotifications by remember { mutableStateOf(true) }

    // Notifications
    // TODO: Wire to notification settings in Room
    var pushNotifications by remember { mutableStateOf(true) }
    var sleepReminders by remember { mutableStateOf(true) }
    var weeklyReports by remember { mutableStateOf(false) }

    // Audio
    // TODO: Wire to audio settings in Room
    var quietHoursStart by remember { mutableStateOf("10 PM") }
    var quietHoursEnd by remember { mutableStateOf("7 AM") }
    var recordingQuality by remember { mutableStateOf("High") }
    var autoplaySounds by remember { mutableStateOf(false) }

    // General
    // TODO: Wire to app settings in Room
    var appTheme by remember { mutableStateOf("Dark") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {

        Text(
            text = "SETTINGS",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ───────── User Profile Card ─────────
        // TODO: Navigate to profile edit screen
        Card(
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // TODO: Replace with real user photo
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "U",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }

                    Text(
                        text = userName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = "Edit profile",
                    tint = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── SLEEP PREFERENCES ─────────
        SectionHeader(title = "SLEEP PREFERENCES")

        Spacer(modifier = Modifier.height(8.dp))

        // TODO: Navigate to sleep goal picker
        SettingsItem(
            icon = AppIcons.Nightlight,
            title = "Sleep Goal",
            value = sleepGoal,
            onClick = { }
        )

        // TODO: Navigate to bedtime reminder time picker
        SettingsItem(
            icon = AppIcons.Notification,
            title = "Bedtime Reminder",
            value = bedtimeReminder,
            onClick = { }
        )

        // TODO: Wire to notification preferences in Room
        SettingsToggleItem(
            icon = AppIcons.NotificationActive,
            title = "Smart Notifications",
            checked = smartNotifications,
            onToggle = { smartNotifications = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── NOTIFICATIONS ─────────
        SectionHeader(title = "NOTIFICATIONS")

        Spacer(modifier = Modifier.height(8.dp))

        // TODO: Wire to notification settings in Room
        SettingsToggleItem(
            icon = AppIcons.Notification,
            title = "Push Notifications",
            checked = pushNotifications,
            onToggle = { pushNotifications = it }
        )

        SettingsToggleItem(
            icon = AppIcons.Alarm,
            title = "Sleep Reminders",
            checked = sleepReminders,
            onToggle = { sleepReminders = it }
        )

        SettingsToggleItem(
            icon = AppIcons.Calendar,
            title = "Weekly Reports",
            checked = weeklyReports,
            onToggle = { weeklyReports = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── AUDIO ─────────
        SectionHeader(title = "AUDIO")

        Spacer(modifier = Modifier.height(8.dp))

        // TODO: Navigate to quiet hours time range picker
        SettingsItem(
            icon = AppIcons.Nightlight,
            title = "Quiet Hours",
            value = "$quietHoursStart - $quietHoursEnd",
            onClick = { }
        )

        // TODO: Navigate to sound library screen
        SettingsItem(
            icon = AppIcons.Sound,
            title = "Sound Library",
            value = null,
            onClick = { }
        )

        // TODO: Navigate to recording quality picker
        SettingsItem(
            icon = AppIcons.HighQuality,
            title = "Recording Quality",
            value = recordingQuality,
            onClick = { }
        )

        // TODO: Wire to audio settings in Room
        SettingsToggleItem(
            icon = AppIcons.Play,
            title = "Auto-play Sounds",
            checked = autoplaySounds,
            onToggle = { autoplaySounds = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── GENERAL ─────────
        SectionHeader(title = "GENERAL")

        Spacer(modifier = Modifier.height(8.dp))

        // TODO: Navigate to theme picker (Light/Dark/System)
        SettingsItem(
            icon = AppIcons.Palette,
            title = "App Appearance",
            value = appTheme,
            onClick = { }
        )

        // TODO: Navigate to Do Not Disturb settings
        SettingsItem(
            icon = AppIcons.DoNotDisturb,
            title = "Do Not Disturb",
            value = null,
            onClick = { }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── SUPPORT ─────────
        SectionHeader(title = "SUPPORT")

        Spacer(modifier = Modifier.height(8.dp))

        // TODO: Navigate to help center / FAQ
        SettingsItem(
            icon = AppIcons.Help,
            title = "Help Center",
            value = null,
            onClick = { }
        )

        // TODO: Navigate to about screen
        SettingsItem(
            icon = AppIcons.Info,
            title = "About CalmSpace",
            value = "v1.0.2",
            onClick = { }
        )

        // Navigate to privacy policy screen
        SettingsItem(
            icon = AppIcons.Shield,
            title = "Privacy Policy",
            value = null,
            onClick = onNavigateToPrivacyPolicy
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── DEVELOPER ─────────
        SectionHeader(title = "DEVELOPER")

        Spacer(modifier = Modifier.height(8.dp))

        // TODO: Navigate to questionnaire documentation
        SettingsItem(
            icon = AppIcons.Description,
            title = "Questionnaire Documentation",
            value = null,
            onClick = { }
        )

        // TODO: Show logout confirmation dialog
        SettingsItem(
            icon = AppIcons.ExitToApp,
            title = "Log Out",
            value = null,
            onClick = { }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────
// Section Header Component
// ─────────────────────────────────────────────

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = Color.Gray,
        fontWeight = FontWeight.Bold
    )
}

// ─────────────────────────────────────────────
// Settings Item Component (with optional value)
// ─────────────────────────────────────────────

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    value: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Gray
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────
// Settings Toggle Item Component
// ─────────────────────────────────────────────

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Gray
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onToggle
        )
    }
}