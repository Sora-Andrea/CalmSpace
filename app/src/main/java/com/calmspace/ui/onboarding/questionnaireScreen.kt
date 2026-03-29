package com.calmspace.ui.onboarding

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// SharedPreferences keys — shared with ProfileScreen
const val PREF_Q_SLEEP_SCHEDULE    = "q_sleep_schedule"
const val PREF_Q_SLEEP_ENVIRONMENT = "q_sleep_environment"
const val PREF_Q_SLEEP_HABITS      = "q_sleep_habits"
const val PREF_Q_HEALTH_FACTORS    = "q_health_factors"

// ─────────────────────────────────────────────
// Questionnaire Screen
// Used for both first-time onboarding and
// re-editing from Profile. All answers are
// persisted to SharedPreferences.
// ─────────────────────────────────────────────

@Composable
fun QuestionnaireScreen(
    onFinish: () -> Unit,
    isEditing: Boolean = false
) {
    val context = LocalContext.current
    val prefs   = remember { context.getSharedPreferences("calmspace_prefs", Context.MODE_PRIVATE) }

    // Pre-populate with previously saved answers
    var sleepSchedule    by remember { mutableStateOf(prefs.getString(PREF_Q_SLEEP_SCHEDULE,    "") ?: "") }
    var sleepEnvironment by remember { mutableStateOf(prefs.getString(PREF_Q_SLEEP_ENVIRONMENT, "") ?: "") }
    var sleepHabits      by remember { mutableStateOf(prefs.getString(PREF_Q_SLEEP_HABITS,      "") ?: "") }
    val healthFactors = remember {
        val saved = prefs.getString(PREF_Q_HEALTH_FACTORS, "") ?: ""
        mutableStateListOf<String>().also { list ->
            if (saved.isNotEmpty()) list.addAll(saved.split(",").filter { it.isNotBlank() })
        }
    }

    var micPermission          by remember { mutableStateOf(false) }
    var notificationPermission by remember { mutableStateOf(false) }

    // Editing from Profile skips the permissions page
    val totalPages = if (isEditing) 4 else 5
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val progress   = (pagerState.currentPage + 1).toFloat() / totalPages.toFloat()
    val onLastPage = pagerState.currentPage == totalPages - 1

    fun saveAndFinish() {
        prefs.edit()
            .putString(PREF_Q_SLEEP_SCHEDULE,    sleepSchedule)
            .putString(PREF_Q_SLEEP_ENVIRONMENT, sleepEnvironment)
            .putString(PREF_Q_SLEEP_HABITS,      sleepHabits)
            .putString(PREF_Q_HEALTH_FACTORS,    healthFactors.joinToString(","))
            .putBoolean("questionnaire_completed", true)
            .apply()
        onFinish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Text(
            text       = if (isEditing) "Edit Questionnaire" else "CalmSpace",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (isEditing) {
            Text(
                text  = "Update your answers any time your sleep situation changes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {

                0 -> RadioQuestion(
                    title    = "What best describes your sleep schedule?",
                    options  = listOf(
                        "Regular schedule (same time daily)",
                        "Mostly regular (weekday/weekend split)",
                        "Irregular schedule",
                        "Shift work / rotating schedule"
                    ),
                    selected = sleepSchedule,
                    onSelect = { sleepSchedule = it }
                )

                1 -> RadioQuestion(
                    title    = "How would you describe your sleep environment?",
                    options  = listOf(
                        "Quiet",
                        "Occasionally noisy",
                        "Frequently noisy",
                        "Very noisy"
                    ),
                    selected = sleepEnvironment,
                    onSelect = { sleepEnvironment = it }
                )

                2 -> RadioQuestion(
                    title    = "How easily do you fall asleep?",
                    options  = listOf(
                        "Fall asleep easily",
                        "Occasionally have trouble falling asleep",
                        "Frequently struggle to fall asleep",
                        "Severe insomnia"
                    ),
                    selected = sleepHabits,
                    onSelect = { sleepHabits = it }
                )

                3 -> CheckboxQuestion(
                    title         = "Do any of these affect your sleep?",
                    options       = listOf(
                        "Stress or anxiety",
                        "Noise sensitivity",
                        "Light sensitivity",
                        "Sleep apnea or breathing issues",
                        "Chronic pain",
                        "None of the above"
                    ),
                    selectedItems = healthFactors
                )

                4 -> PermissionPage(
                    micPermission          = micPermission,
                    notificationPermission = notificationPermission,
                    onMicToggle            = { micPermission = it },
                    onNotificationToggle   = { notificationPermission = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress dots
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(totalPages) { index ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(8.dp)
                        .background(
                            color = if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.LightGray,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isEditing) {
            Button(
                onClick  = { saveAndFinish() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (onLastPage) "Save Changes" else "Save & Exit")
            }
        } else {
            if (onLastPage) {
                Button(
                    onClick  = { saveAndFinish() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled  = micPermission && notificationPermission
                ) {
                    Text("Finish Setup")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Radio Question
// ─────────────────────────────────────────────

@Composable
fun RadioQuestion(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))

        options.forEach { option ->
            val isSelected = option == selected
            if (isSelected) {
                Button(
                    onClick  = { onSelect(option) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                ) {
                    Text(option, fontWeight = FontWeight.Medium)
                }
            } else {
                OutlinedButton(
                    onClick  = { onSelect(option) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                ) {
                    Text(option)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Checkbox Question
// ─────────────────────────────────────────────

@Composable
fun CheckboxQuestion(
    title: String,
    options: List<String>,
    selectedItems: MutableList<String>
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))

        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked         = selectedItems.contains(option),
                    onCheckedChange = {
                        if (it) selectedItems.add(option) else selectedItems.remove(option)
                    }
                )
                Text(option, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────
// Permissions Page
// ─────────────────────────────────────────────

@Composable
fun PermissionPage(
    micPermission: Boolean,
    notificationPermission: Boolean,
    onMicToggle: (Boolean) -> Unit,
    onNotificationToggle: (Boolean) -> Unit
) {
    Column {
        Text("Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text  = "CalmSpace requires the following permissions to function correctly. " +
                    "All processing occurs locally on your device and no audio is recorded or stored.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        PermissionRow(
            title       = "Microphone Access",
            description = "Used to monitor ambient noise during sleep so CalmSpace can automatically " +
                          "adjust masking audio. Audio is analyzed in real time and never saved.",
            checked     = micPermission,
            onToggle    = onMicToggle
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionRow(
            title       = "Notifications",
            description = "Required to keep sleep sessions running overnight using a foreground service " +
                          "and to provide session controls without waking the screen.",
            checked     = notificationPermission,
            onToggle    = onNotificationToggle
        )
    }
}

// ─────────────────────────────────────────────
// Permission Row
// ─────────────────────────────────────────────

@Composable
fun PermissionRow(
    title: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Switch(checked = checked, onCheckedChange = onToggle)
        }
        Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}
