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
import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.calmspace.sleep.SleepScheduleManager
import com.calmspace.ui.components.AppIcons
import com.calmspace.ui.theme.AppTheme

// ─────────────────────────────────────────────
// Settings Screen
// User preferences, notifications, audio, general settings
// ─────────────────────────────────────────────

private val themeDisplayNames = mapOf(
    AppTheme.DEEP_WATER to "Deep Water",
    AppTheme.OCEAN      to "Ocean",
    AppTheme.FOREST     to "Forest",
    AppTheme.SUNSET     to "Sunset",
    AppTheme.SUNRISE    to "Sunrise"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToMediaPlayer:   () -> Unit = {},
    onNavigateToProfile:       () -> Unit = {},
    onLogOut:                  () -> Unit = {},
    currentTheme:              AppTheme   = AppTheme.DEEP_WATER,
    onThemeSelected:           (AppTheme) -> Unit = {}
) {
    val context = LocalContext.current

    // ─────────────────────────────────────────────
    // Placeholder State
    // TODO: Replace with ViewModel + Room database
    // ─────────────────────────────────────────────

    var userName     by remember { mutableStateOf("Sleep User") }
    var userInitials by remember { mutableStateOf("SU") }

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

    var showThemePicker  by remember { mutableStateOf(false) }
    var showLogOutDialog by remember { mutableStateOf(false) }

    // Schedule state — loaded from SharedPreferences
    var scheduleEnabled  by remember { mutableStateOf(false) }
    var bedtimeHour      by remember { mutableStateOf(22) }
    var bedtimeMinute    by remember { mutableStateOf(30) }
    var wakeHour         by remember { mutableStateOf(7) }
    var wakeMinute       by remember { mutableStateOf(0) }
    var showBedtimePicker by remember { mutableStateOf(false) }
    var showWakePicker    by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("calmspace_prefs", android.content.Context.MODE_PRIVATE)
        userName     = prefs.getString("user_name",     "Sleep User") ?: "Sleep User"
        userInitials = prefs.getString("user_initials", "SU")         ?: "SU"
        scheduleEnabled = prefs.getBoolean("schedule_enabled", false)
        bedtimeHour     = prefs.getInt("bedtime_hour",    22)
        bedtimeMinute   = prefs.getInt("bedtime_minute",  30)
        wakeHour        = prefs.getInt("wake_hour",        7)
        wakeMinute      = prefs.getInt("wake_minute",      0)
    }

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
            onClick = onNavigateToProfile,
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
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = userInitials.ifBlank { "?" },
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer
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

        // ───────── SCHEDULE ─────────
        SectionHeader(title = "SCHEDULE")

        Spacer(modifier = Modifier.height(8.dp))

        SettingsToggleItem(
            icon = AppIcons.Alarm,
            title = "Enable Schedule",
            checked = scheduleEnabled,
            onToggle = { enabled ->
                val prefs = context.getSharedPreferences("calmspace_prefs", android.content.Context.MODE_PRIVATE)
                if (enabled) {
                    // Android 12+ requires explicit user permission for exact alarms
                    val alarmManager = context.getSystemService(AlarmManager::class.java)
                    val canSchedule = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                            alarmManager.canScheduleExactAlarms()
                    if (!canSchedule) {
                        // Open system settings so the user can grant Alarms & Reminders
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        // Don't enable until permission is granted
                        return@SettingsToggleItem
                    }
                    scheduleEnabled = true
                    prefs.edit().putBoolean("schedule_enabled", true).apply()
                    SleepScheduleManager.scheduleBedtimeReminder(context, bedtimeHour, bedtimeMinute)
                    SleepScheduleManager.scheduleWakeStop(context, wakeHour, wakeMinute)
                } else {
                    scheduleEnabled = false
                    prefs.edit().putBoolean("schedule_enabled", false).apply()
                    SleepScheduleManager.cancelBedtimeReminder(context)
                    SleepScheduleManager.cancelWakeStop(context)
                }
            }
        )

        SettingsItem(
            icon = AppIcons.Nightlight,
            title = "Bedtime",
            value = formatTime(bedtimeHour, bedtimeMinute),
            onClick = { showBedtimePicker = true }
        )

        SettingsItem(
            icon = AppIcons.Alarm,
            title = "Wake Time",
            value = formatTime(wakeHour, wakeMinute),
            onClick = { showWakePicker = true }
        )

        // Bedtime time picker dialog
        if (showBedtimePicker) {
            val state = rememberTimePickerState(
                initialHour = bedtimeHour,
                initialMinute = bedtimeMinute,
                is24Hour = false
            )
            AlertDialog(
                onDismissRequest = { showBedtimePicker = false },
                title = { Text("Bedtime") },
                text = { TimePicker(state = state) },
                confirmButton = {
                    TextButton(onClick = {
                        bedtimeHour   = state.hour
                        bedtimeMinute = state.minute
                        val prefs = context.getSharedPreferences("calmspace_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit()
                            .putInt("bedtime_hour",   state.hour)
                            .putInt("bedtime_minute", state.minute)
                            .apply()
                        if (scheduleEnabled) {
                            SleepScheduleManager.scheduleBedtimeReminder(context, state.hour, state.minute)
                        }
                        showBedtimePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showBedtimePicker = false }) { Text("Cancel") }
                }
            )
        }

        // Wake time picker dialog
        if (showWakePicker) {
            val state = rememberTimePickerState(
                initialHour = wakeHour,
                initialMinute = wakeMinute,
                is24Hour = false
            )
            AlertDialog(
                onDismissRequest = { showWakePicker = false },
                title = { Text("Wake Time") },
                text = { TimePicker(state = state) },
                confirmButton = {
                    TextButton(onClick = {
                        wakeHour   = state.hour
                        wakeMinute = state.minute
                        val prefs = context.getSharedPreferences("calmspace_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit()
                            .putInt("wake_hour",   state.hour)
                            .putInt("wake_minute", state.minute)
                            .apply()
                        if (scheduleEnabled) {
                            SleepScheduleManager.scheduleWakeStop(context, state.hour, state.minute)
                        }
                        showWakePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showWakePicker = false }) { Text("Cancel") }
                }
            )
        }

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

        SettingsItem(
            icon = AppIcons.Palette,
            title = "App Appearance",
            value = themeDisplayNames[currentTheme] ?: currentTheme.name,
            onClick = { showThemePicker = true }
        )

        if (showThemePicker) {
            AlertDialog(
                onDismissRequest = { showThemePicker = false },
                title = { Text("Choose Theme") },
                text = {
                    Column {
                        AppTheme.entries.forEach { theme ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onThemeSelected(theme)
                                        showThemePicker = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = themeDisplayNames[theme] ?: theme.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (theme == currentTheme) FontWeight.Bold else FontWeight.Normal
                                )
                                if (theme == currentTheme) {
                                    Icon(
                                        imageVector = AppIcons.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemePicker = false }) {
                        Text("Close")
                    }
                }
            )
        }

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

        SettingsItem(
            icon = AppIcons.Sound,
            title = "Media Player / Visualizer",
            value = null,
            onClick = onNavigateToMediaPlayer
        )

        SettingsItem(
            icon = AppIcons.ExitToApp,
            title = "Log Out",
            value = null,
            onClick = { showLogOutDialog = true }
        )

        if (showLogOutDialog) {
            AlertDialog(
                onDismissRequest = { showLogOutDialog = false },
                title  = { Text("Log Out") },
                text   = { Text("Are you sure you want to log out?") },
                confirmButton  = {
                    TextButton(onClick = {
                        context.getSharedPreferences("calmspace_prefs", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("logged_in", false).apply()
                        showLogOutDialog = false
                        onLogOut()
                    }) {
                        Text("Log Out", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogOutDialog = false }) { Text("Cancel") }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────
// Section Header Component
// ─────────────────────────────────────────────

private fun formatTime(hour: Int, minute: Int): String {
    val h12 = when {
        hour == 0  -> 12
        hour > 12  -> hour - 12
        else       -> hour
    }
    val amPm = if (hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(h12, minute, amPm)
}

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