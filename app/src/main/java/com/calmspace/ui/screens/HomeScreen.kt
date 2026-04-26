package com.calmspace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.calmspace.service.SleepSessionLogStore
import com.calmspace.service.buildSessionHistorySummary
import com.google.firebase.auth.FirebaseAuth
import com.calmspace.ui.theme.MoonGold
import com.calmspace.ui.screens.monitor.MonitorStarfield
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
// Home Screen
// Main dashboard shown after login / onboarding
// ─────────────────────────────────────────────

@Composable
fun HomeScreen(
    onStartSession: () -> Unit,
    onSeeAllSessions: () -> Unit // TODO: Navigate to full session history screen
) {

    // ─────────────────────────────────────────────
    // Placeholder Data
    // TODO: Replace with ViewModel + Room database
    // ─────────────────────────────────────────────

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var username by remember { mutableStateOf("") }
    var sessionSummary by remember { mutableStateOf(buildSessionHistorySummary(emptyList())) }
    val activeUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "local" }

    LaunchedEffect(Unit) {
        username = context.getSharedPreferences("calmspace_prefs", android.content.Context.MODE_PRIVATE)
            .getString("user_name", "") ?: ""
        sessionSummary = buildSessionHistorySummary(
            SleepSessionLogStore.getCompletedSessionsForUser(context, activeUserId)
        )
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                sessionSummary = buildSessionHistorySummary(
                    SleepSessionLogStore.getCompletedSessionsForUser(context, activeUserId)
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val sleepQualityMessage = if (sessionSummary.hasSessions) {
        "Average session quality: ${sessionSummary.avgQualityText}"
    } else {
        "Start your first session to build sleep insights."
    }
    val lastNightHours = sessionSummary.lastNightHoursText
    val lastNightDepth = sessionSummary.lastNightDepthText
    val lastNightQuality = sessionSummary.lastNightQualityText
    val weeklyData = sessionSummary.weeklyBarsLabeled
    val weeklyAverage = sessionSummary.weeklyAverageHoursText
    val recentSessionDate = sessionSummary.recentDateText
    val recentSessionSound = sessionSummary.recentSoundText
    val recentSessionDuration = sessionSummary.recentDurationText
    val recentSessionQuality = sessionSummary.recentQualityText

    // ─────────────────────────────────────────────
    // Layout
    // ─────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        MonitorStarfield(modifier = Modifier.fillMaxSize())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {

        // ───────── Header ─────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text  = "Welcome back",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
                Text(
                    text       = if (username.isNotBlank()) username else "CalmSpace",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // TODO: Wire to dark mode toggle or time-based display
            Icon(
                imageVector = Icons.Default.Bedtime,
                contentDescription = "Night mode",
                modifier = Modifier.size(28.dp),
                tint = MoonGold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = sleepQualityMessage,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── Sleep Overview Card ─────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sleep Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // TODO: Navigate to full history screen
                    TextButton(onClick = onSeeAllSessions) {
                        Text("View All")
                    }
                }

                Text(
                    text = "Last Night",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ───── Stats Row ─────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    // TODO: Replace with real data from last session
                    SleepStat(label = "Duration", value = lastNightHours)
                    SleepStat(label = "Avg Depth", value = lastNightDepth)
                    SleepStat(label = "Quality", value = lastNightQuality)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ───── Start Session Button ─────
                Button(
                    onClick = onStartSession,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Sleep Session")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── This Week Section ─────────
        Text(
            text = "This Week",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Mon – Sun",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text(
                    text = "Avg Quality",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                // TODO: Replace with real weekly average from Room
                Text(
                    text = weeklyAverage,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ───── Bar Chart ─────
                // TODO: Replace bar heights with real session duration data from Room
                WeeklyBarChart(data = weeklyData)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── Recent Sessions Section ─────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // TODO: Navigate to full session history screen
            TextButton(onClick = onSeeAllSessions) {
                Text("See All")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // TODO: Replace with real session list from Room (most recent only for now)
        SessionRow(
            date = recentSessionDate,
            sound = recentSessionSound,
            duration = recentSessionDuration,
            quality = recentSessionQuality
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
    } // end Box
}

// ─────────────────────────────────────────────
// Sleep Stat Component
// Displays a single labeled stat value
// ─────────────────────────────────────────────

@Composable
fun SleepStat(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

// ─────────────────────────────────────────────
// Weekly Bar Chart Component
// Simple canvas-free bar chart using Box composables
// TODO: Replace with Vico or MPAndroidChart when
//       real session data is available from Room
// ─────────────────────────────────────────────

@Composable
fun WeeklyBarChart(
    data: List<Pair<String, Float>> // Pair(dayLabel, relativeHeight 0.0-1.0)
) {
    val barMaxHeight = 80.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (day, value) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(barMaxHeight * value)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Session Row Component
// Displays a single past sleep session summary
// TODO: Update to accept a Session data class
//       once Room entity is defined
// ─────────────────────────────────────────────

@Composable
fun SessionRow(
    date: String,
    sound: String,
    duration: String,
    quality: String
) {
    Card(
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sound,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = duration,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = quality,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
