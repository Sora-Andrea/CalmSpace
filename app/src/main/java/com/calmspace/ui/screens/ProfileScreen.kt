package com.calmspace.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmspace.ui.components.AppIcons
import com.calmspace.ui.onboarding.PREF_Q_HEALTH_FACTORS
import com.calmspace.ui.onboarding.PREF_Q_SLEEP_ENVIRONMENT
import com.calmspace.ui.onboarding.PREF_Q_SLEEP_HABITS
import com.calmspace.ui.onboarding.PREF_Q_SLEEP_SCHEDULE

// ─────────────────────────────────────────────
// Profile Screen
// ─────────────────────────────────────────────

@Composable
fun ProfileScreen(
    onEditQuestionnaire: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current

    // ── User identity ──
    var userName     by remember { mutableStateOf("") }
    var userInitials by remember { mutableStateOf("") }
    var userEmail    by remember { mutableStateOf("") }

    // ── Sleep Preferences from SharedPreferences (mirrors Settings) ──
    var sleepGoalHours   by remember { mutableStateOf(8) }
    var reminderHour     by remember { mutableStateOf(22) }
    var reminderMinute   by remember { mutableStateOf(0) }
    var recordingQuality by remember { mutableStateOf("High") }
    var doNotDisturb     by remember { mutableStateOf(false) }

    // ── Questionnaire completion ──
    var questionnaireItems by remember {
        mutableStateOf(listOf(
            Pair("Sleep Schedule",    false),
            Pair("Sleep Environment", false),
            Pair("Sleep Habits",      false),
            Pair("Health Factors",    false)
        ))
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("calmspace_prefs", Context.MODE_PRIVATE)
        userName     = prefs.getString("user_name",     "Sleep User") ?: "Sleep User"
        userInitials = prefs.getString("user_initials", "SU")         ?: "SU"
        userEmail    = prefs.getString("user_email",    "")           ?: ""
        sleepGoalHours   = prefs.getInt("sleep_goal_hours", 8)
        reminderHour     = prefs.getInt("bedtime_reminder_hour",   22)
        reminderMinute   = prefs.getInt("bedtime_reminder_minute",  0)
        recordingQuality = prefs.getString("recording_quality", "High") ?: "High"
        doNotDisturb     = prefs.getBoolean("do_not_disturb", false)
        questionnaireItems = listOf(
            Pair("Sleep Schedule",    prefs.getString(PREF_Q_SLEEP_SCHEDULE,    "").isNullOrBlank().not()),
            Pair("Sleep Environment", prefs.getString(PREF_Q_SLEEP_ENVIRONMENT, "").isNullOrBlank().not()),
            Pair("Sleep Habits",      prefs.getString(PREF_Q_SLEEP_HABITS,      "").isNullOrBlank().not()),
            Pair("Health Factors",    prefs.getString(PREF_Q_HEALTH_FACTORS,    "").isNullOrBlank().not())
        )
    }

    val questionnaireProgress = questionnaireItems.count { it.second }
    val questionnaireTotal    = questionnaireItems.size

    // Placeholder stats
    val totalSessions = "147"
    val totalSleep    = "1,234h"
    val avgQuality    = "87%"
    val weeklyData    = listOf(0.6f, 0.8f, 0.5f, 0.9f, 0.4f, 0.7f, 0.85f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "PROFILE",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ───────── Avatar ─────────
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userInitials.ifBlank { "?" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = userName.ifBlank { "Sleep User" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Member since Jan 2025",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ───────── Stats Card ─────────
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(label = "Sessions",    value = totalSessions)
                VerticalDivider(modifier = Modifier.height(40.dp))
                StatItem(label = "Total Sleep", value = totalSleep)
                VerticalDivider(modifier = Modifier.height(40.dp))
                StatItem(label = "Avg Quality", value = avgQuality)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ───────── This Week Card ─────────
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Sleep Time", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("52h 30m", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text("+2.5h vs last week", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                WeeklyBarChart(values = weeklyData)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ───────── Sleep Preferences ─────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sleep Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onNavigateToSettings) {
                Text("Edit", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column {
                PreferenceRow(
                    icon  = AppIcons.Nightlight,
                    label = "Sleep Goal",
                    value = "$sleepGoalHours hours"
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                PreferenceRow(
                    icon  = AppIcons.Alarm,
                    label = "Bedtime Reminder",
                    value = profileFormatTime(reminderHour, reminderMinute)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                PreferenceRow(
                    icon  = AppIcons.HighQuality,
                    label = "Recording Quality",
                    value = recordingQuality
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                PreferenceRow(
                    icon  = AppIcons.DoNotDisturb,
                    label = "Do Not Disturb",
                    value = if (doNotDisturb) "On" else "Off"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ───────── Sleep Questionnaire ─────────
        Text("Sleep Questionnaire", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$questionnaireProgress of $questionnaireTotal sections complete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = "${((questionnaireProgress.toFloat() / questionnaireTotal) * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { questionnaireProgress.toFloat() / questionnaireTotal.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        )

        Spacer(modifier = Modifier.height(14.dp))

        questionnaireItems.forEach { (name, isCompleted) ->
            QuestionnaireItem(
                name        = name,
                isCompleted = isCompleted,
                onClick     = onEditQuestionnaire
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────
// Time formatter
// ─────────────────────────────────────────────

private fun profileFormatTime(hour: Int, minute: Int): String {
    val h12  = if (hour % 12 == 0) 12 else hour % 12
    val amPm = if (hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(h12, minute, amPm)
}

// ─────────────────────────────────────────────
// Preference Row
// ─────────────────────────────────────────────

@Composable
private fun PreferenceRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

// ─────────────────────────────────────────────
// Questionnaire Item
// ─────────────────────────────────────────────

@Composable
fun QuestionnaireItem(name: String, isCompleted: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isCompleted) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(22.dp)
                )
                Text(text = name, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(imageVector = AppIcons.ChevronRight, contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
        }
    }
}

// ─────────────────────────────────────────────
// Weekly Bar Chart
// ─────────────────────────────────────────────

@Composable
fun WeeklyBarChart(values: List<Float>) {
    val labels       = listOf("M", "T", "W", "T", "F", "S", "S")
    val primaryColor = MaterialTheme.colorScheme.primary
    val dimColor     = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val maxIndex     = values.indexOf(values.maxOrNull() ?: 0f)

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
            val count    = values.size
            val gapRatio = 0.4f
            val barW     = size.width / (count + (count - 1) * gapRatio)
            val gap      = barW * gapRatio
            val radius   = barW / 2f

            values.forEachIndexed { index, value ->
                val left      = index * (barW + gap)
                val barHeight = size.height * value.coerceIn(0f, 1f)
                val top       = size.height - barHeight
                drawRoundRect(
                    color        = if (index == maxIndex) primaryColor else dimColor,
                    topLeft      = Offset(left, top),
                    size         = Size(barW, barHeight),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            labels.forEach { day ->
                Text(text = day, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
            }
        }
    }
}

// ─────────────────────────────────────────────
// Stat Item
// ─────────────────────────────────────────────

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 14.sp)
    }
}
