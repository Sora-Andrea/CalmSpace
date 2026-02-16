package com.calmspace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
// Profile Screen
// Single scrollable page with user stats,
// insights, preferences, and questionnaire
// ─────────────────────────────────────────────

@Composable
fun ProfileScreen() {

    // ─────────────────────────────────────────────
    // Placeholder Data
    // TODO: Replace with ViewModel + Room database
    // ─────────────────────────────────────────────

    // TODO: Replace with real user data from Room
    val memberSince = "Jan 2025"
    val totalSessions = "147"
    val totalSleep = "1,234h"
    val avgQuality = "87%"

    // TODO: Replace with weekly session data from Room
    val weeklyData = listOf(0.6f, 0.8f, 0.5f, 0.9f, 0.4f, 0.7f, 0.85f)
    val weeklyAverage = "52h 30m"
    val weeklyChange = "+2.5h vs last week"

    // TODO: Replace with questionnaire completion status from Room
    val questionnaireProgress = 2
    val questionnaireTotal = 4
    val questionnaireItems = listOf(
        Pair("Sleep Schedule", true),
        Pair("Sleep Environment", true),
        Pair("Sleep Habits", false),
        Pair("Health Factors", false)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ───────── Profile Header ─────────
        Text(
            text = "PROFILE",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ───── User Avatar ─────
        // TODO: Replace with real user photo from profile
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "U",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Member since $memberSince",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ───── Stats Row ─────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Sessions", value = totalSessions)
            StatItem(label = "Total\nSleep", value = totalSleep)
            StatItem(label = "Avg\nQuality", value = avgQuality)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── This Week Card ─────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text(
                    text = "This Week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Sleep Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = weeklyAverage,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = weeklyChange,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ───── Bar Chart ─────
                WeeklyBarChart(
                    data = listOf(
                        Pair("M", weeklyData[0]),
                        Pair("T", weeklyData[1]),
                        Pair("W", weeklyData[2]),
                        Pair("T", weeklyData[3]),
                        Pair("F", weeklyData[4]),
                        Pair("S", weeklyData[5]),
                        Pair("S", weeklyData[6])
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── Sleep Insights Section ─────────
        Column(modifier = Modifier.fillMaxWidth()) {

            Text(
                text = "Sleep Insights",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Coming Soon",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    // TODO: Display personalized sleep insights based on session history
                    // - Best sleep quality days/times
                    // - Most common noise disruptions
                    // - Recommended sound adjustments
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── Sleep Preferences Section ─────────
        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sleep Preferences",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // TODO: Navigate to preferences edit screen
                TextButton(onClick = { }) {
                    Text("Edit")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No preferences set",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    // TODO: Display user preferences:
                    // - Preferred sleep sounds
                    // - Noise sensitivity level
                    // - Bedtime schedule
                    // - Do Not Disturb settings
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── Sleep Questionnaire Section ─────────
        Column(modifier = Modifier.fillMaxWidth()) {

            Text(
                text = "Sleep Questionnaire",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$questionnaireProgress/$questionnaireTotal Complete",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ───── Progress Bar ─────
            Column {
                Text(
                    text = "Progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { questionnaireProgress.toFloat() / questionnaireTotal.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ───── Questionnaire Items ─────
            questionnaireItems.forEach { (name, isCompleted) ->
                QuestionnaireItem(
                    name = name,
                    isCompleted = isCompleted,
                    onClick = {
                        // TODO: Navigate to questionnaire screen to edit this section
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────
// Questionnaire Item Component
// ─────────────────────────────────────────────

@Composable
fun QuestionnaireItem(
    name: String,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Circle,
                    contentDescription = null,
                    tint = if (isCompleted)
                        MaterialTheme.colorScheme.primary
                    else
                        Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                text = ">",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
        }
    }
}

// ─────────────────────────────────────────────
// Stat Item Component
// ─────────────────────────────────────────────

@Composable
fun StatItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            lineHeight = 14.sp
        )
    }
}