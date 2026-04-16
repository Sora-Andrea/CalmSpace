package com.calmspace.service

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private const val TARGET_SLEEP_HOURS = 8f
private const val MS_PER_HOUR = 3_600_000L
private const val MS_PER_MINUTE = 60_000L

data class SessionHistorySummary(
    val hasSessions: Boolean,
    val weeklyBarsLabeled: List<Pair<String, Float>>,
    val weeklyBarsValues: List<Float>,
    val weeklyAverageHoursText: String,
    val recentDateText: String,
    val recentSoundText: String,
    val recentDurationText: String,
    val recentQualityText: String,
    val totalSessionsText: String,
    val totalSleepText: String,
    val avgQualityText: String,
    val thisWeekTotalText: String,
    val weekDeltaText: String,
    val lastNightHoursText: String,
    val lastNightDepthText: String,
    val lastNightQualityText: String
)

fun buildSessionHistorySummary(
    records: List<SleepSessionLogStore.CompletedSessionRecord>,
    nowUtcMs: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): SessionHistorySummary {
    if (records.isEmpty()) {
        val zeroBars = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").map { it to 0f }
        return SessionHistorySummary(
            hasSessions = false,
            weeklyBarsLabeled = zeroBars,
            weeklyBarsValues = zeroBars.map { it.second },
            weeklyAverageHoursText = "0.0 hrs",
            recentDateText = "No sessions",
            recentSoundText = "Ambient Sound",
            recentDurationText = "0h 00m",
            recentQualityText = "No data",
            totalSessionsText = "0",
            totalSleepText = "0h",
            avgQualityText = "0%",
            thisWeekTotalText = "0h 00m",
            weekDeltaText = "No data yet",
            lastNightHoursText = "0h 00m",
            lastNightDepthText = "0%",
            lastNightQualityText = "0%"
        )
    }

    val today = Instant.ofEpochMilli(nowUtcMs).atZone(zoneId).toLocalDate()
    val thisWeekStart = today.with(DayOfWeek.MONDAY)
    val nextWeekStart = thisWeekStart.plusWeeks(1)
    val previousWeekStart = thisWeekStart.minusWeeks(1)
    val previousWeekEnd = thisWeekStart

    val thisWeekTotals = mutableMapOf<LocalDate, Long>()
    var thisWeekTotalMs = 0L
    var previousWeekTotalMs = 0L

    records.forEach { record ->
        val sessionDate = Instant.ofEpochMilli(record.startedAtUtcMs).atZone(zoneId).toLocalDate()
        if (!sessionDate.isBefore(thisWeekStart) && sessionDate.isBefore(nextWeekStart)) {
            thisWeekTotals[sessionDate] = (thisWeekTotals[sessionDate] ?: 0L) + record.durationMs
            thisWeekTotalMs += record.durationMs
        }
        if (!sessionDate.isBefore(previousWeekStart) && sessionDate.isBefore(previousWeekEnd)) {
            previousWeekTotalMs += record.durationMs
        }
    }

    val dayLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    val weeklyBars = dayLabels.mapIndexed { index, label ->
        val dayDate = thisWeekStart.plusDays(index.toLong())
        val dayTotal = thisWeekTotals[dayDate] ?: 0L
        val normalized = (dayTotal.toFloat() / (TARGET_SLEEP_HOURS * MS_PER_HOUR)).coerceIn(0f, 1f)
        label to normalized
    }

    val weeklyAverageHours = thisWeekTotalMs.toFloat() / (MS_PER_HOUR * 7f)
    val recent = records.maxByOrNull { it.endedAtUtcMs } ?: records.first()
    val recentRatio = (recent.durationMs.toFloat() / (TARGET_SLEEP_HOURS * MS_PER_HOUR)).coerceIn(0f, 1f)

    val totalSleepMs = records.sumOf { it.durationMs }
    val avgQualityPercent = ((records
        .map { (it.durationMs.toFloat() / (TARGET_SLEEP_HOURS * MS_PER_HOUR)).coerceIn(0f, 1f) }
        .average()) * 100.0).roundToInt().coerceIn(0, 100)

    val weekDeltaHours = (thisWeekTotalMs - previousWeekTotalMs).toFloat() / MS_PER_HOUR.toFloat()
    val weekDeltaText = when {
        previousWeekTotalMs == 0L && thisWeekTotalMs == 0L -> "No data yet"
        weekDeltaHours > 0f -> "+${"%.1f".format(Locale.US, weekDeltaHours)}h vs last week"
        weekDeltaHours < 0f -> "${"%.1f".format(Locale.US, weekDeltaHours)}h vs last week"
        else -> "0.0h vs last week"
    }

    return SessionHistorySummary(
        hasSessions = true,
        weeklyBarsLabeled = weeklyBars,
        weeklyBarsValues = weeklyBars.map { it.second },
        weeklyAverageHoursText = "${"%.1f".format(Locale.US, weeklyAverageHours)} hrs",
        recentDateText = formatDate(recent.startedAtUtcMs, zoneId),
        recentSoundText = trackIdToDisplayName(recent.trackId),
        recentDurationText = formatDurationCompact(recent.durationMs),
        recentQualityText = qualityLabel(recentRatio),
        totalSessionsText = records.size.toString(),
        totalSleepText = formatTotalSleep(totalSleepMs),
        avgQualityText = "$avgQualityPercent%",
        thisWeekTotalText = formatDurationCompact(thisWeekTotalMs),
        weekDeltaText = weekDeltaText,
        lastNightHoursText = formatDurationCompact(recent.durationMs),
        lastNightDepthText = "${(recentRatio * 100f).roundToInt().coerceIn(0, 100)}%",
        lastNightQualityText = "${(recentRatio * 100f).roundToInt().coerceIn(0, 100)}%"
    )
}

private fun formatDate(utcMs: Long, zoneId: ZoneId): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    return Instant.ofEpochMilli(utcMs).atZone(zoneId).toLocalDate().format(formatter)
}

private fun formatDurationCompact(durationMs: Long): String {
    val safe = durationMs.coerceAtLeast(0L)
    val hours = safe / MS_PER_HOUR
    val minutes = (safe % MS_PER_HOUR) / MS_PER_MINUTE
    return "${hours}h ${minutes.toString().padStart(2, '0')}m"
}

private fun formatTotalSleep(durationMs: Long): String {
    val safe = durationMs.coerceAtLeast(0L)
    val hours = safe / MS_PER_HOUR
    return "${hours}h"
}

private fun qualityLabel(ratio: Float): String {
    return when {
        ratio >= 1.0f -> "Excellent"
        ratio >= 0.85f -> "Good"
        ratio >= 0.65f -> "Fair"
        else -> "Short"
    }
}

private fun trackIdToDisplayName(trackId: String?): String {
    return when (trackId?.lowercase(Locale.US)) {
        "white_noise" -> "Bright Static"
        "pink_noise" -> "Balanced Rain"
        "brown_noise" -> "Deep Rumble"
        "blue_noise" -> "High Hiss"
        "grey_noise" -> "Neutral Static"
        null -> "Ambient Sound"
        else -> trackId
            .split('_', '-', ' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.replaceFirstChar { c ->
                    if (c.isLowerCase()) c.titlecase(Locale.US) else c.toString()
                }
            }
    }
}

