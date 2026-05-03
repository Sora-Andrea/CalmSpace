package com.calmspace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    var recentSessions by remember { mutableStateOf<List<SleepSessionLogStore.CompletedSessionRecord>>(emptyList()) }
    var recentSessionsError by remember { mutableStateOf<String?>(null) }
    var expandedSessionIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    val activeUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "local" }

    fun refreshSessionHistory() {
        runCatching {
            val allSessions = SleepSessionLogStore.getCompletedSessions(context, activeUserId)
            val recent = SleepSessionLogStore.getRecentSessionsForUser(
                context = context,
                userId = activeUserId,
                limit = 10
            )
            allSessions to recent
        }.onSuccess { (allSessions, recent) ->
            sessionSummary = buildSessionHistorySummary(allSessions)
            recentSessions = recent
            recentSessionsError = null
        }.onFailure { error ->
            recentSessions = emptyList()
            recentSessionsError = "Failed to load recent sessions: ${error.message ?: "unknown error"}"
        }
    }

    LaunchedEffect(Unit) {
        username = context.getSharedPreferences("calmspace_prefs", android.content.Context.MODE_PRIVATE)
            .getString("user_name", "") ?: ""
        refreshSessionHistory()
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshSessionHistory()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val lastNightHours = sessionSummary.lastNightHoursText
    val weeklyData = sessionSummary.weeklyBarsLabeled
    val weeklyAverage = sessionSummary.weeklyAverageHoursText
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
                    horizontalArrangement = Arrangement.Center
                ) {
                    SleepStat(label = "Duration", value = lastNightHours)
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
                    text = "Avg Duration",
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

        when {
            recentSessionsError != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = recentSessionsError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            recentSessions.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "No recent sessions yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recentSessions.forEach { session ->
                        val sessionId = sessionStableId(session)
                        val isExpanded = expandedSessionIds.contains(sessionId)
                        RecentSessionExpandableRow(
                            session = session,
                            expanded = isExpanded,
                            onToggle = {
                                expandedSessionIds = if (isExpanded) {
                                    expandedSessionIds - sessionId
                                } else {
                                    expandedSessionIds + sessionId
                                }
                            }
                        )
                    }
                }
            }
        }

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

@Composable
private fun RecentSessionExpandableRow(
    session: SleepSessionLogStore.CompletedSessionRecord,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = formatSessionDateTime(session.startedAtUtcMs),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = trackDisplayName(session.trackId),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatDurationCompact(session.durationMs),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse session details" else "Expand session details"
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "End Reason: ${session.endReason}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Duration: ${formatDurationCompact(session.durationMs)}",
                    style = MaterialTheme.typography.bodySmall
                )

                val metrics = session.metrics
                if (metrics == null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Metrics unavailable for this session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Masking Playback: ${formatDurationCompact(metrics.maskingPlaybackMs)}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Bucket Durations",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (metrics.bucketDurationMs.isEmpty()) {
                        Text(
                            text = "No bucket activity recorded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        metrics.bucketDurationMs
                            .toList()
                            .sortedByDescending { it.second }
                            .forEach { (bucket, durationMs) ->
                                Text(
                                    text = "• $bucket: ${formatDurationCompact(durationMs)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Top Label Hits",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (metrics.labelHitCount.isEmpty()) {
                        Text(
                            text = "No label hits recorded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        metrics.labelHitCount
                            .toList()
                            .sortedByDescending { it.second }
                            .forEach { (label, count) ->
                                Text(
                                    text = "• $label: $count",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                    }
                }
            }
        }
    }
}

private fun sessionStableId(session: SleepSessionLogStore.CompletedSessionRecord): String {
    return "${session.startedAtUtcMs}_${session.endedAtUtcMs}"
}

private fun formatSessionDateTime(utcMs: Long): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.US)
    return Instant.ofEpochMilli(utcMs)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(formatter)
}

private fun formatDurationCompact(durationMs: Long): String {
    val safe = durationMs.coerceAtLeast(0L)
    val hours = safe / 3_600_000L
    val minutes = (safe % 3_600_000L) / 60_000L
    return "${hours}h ${minutes.toString().padStart(2, '0')}m"
}

private fun trackDisplayName(trackId: String?): String {
    return when (trackId) {
        "white_noise" -> "Bright Static"
        "pink_noise" -> "Balanced Rain"
        "brown_noise" -> "Deep Rumble"
        "blue_noise" -> "High Hiss"
        "grey_noise" -> "Neutral Static"
        null -> "Ambient Sound"
        else -> trackId.replace('_', ' ').replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
        }
    }
}
