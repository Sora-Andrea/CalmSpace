package com.calmspace.sleep

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

// ─────────────────────────────────────────────────────────────────────
// Sleep Schedule Manager
//
// Sets and cancels daily AlarmManager alarms for:
//   • Bedtime reminder — notification prompting the user to start a session
//   • Wake stop        — stops the active NoiseMonitorService session
//
// Uses setAlarmClock() which fires in Doze mode and needs no special
// runtime permission on any API level. Alarms reschedule themselves
// for the next night after firing (handled in SleepScheduleReceiver).
// ─────────────────────────────────────────────────────────────────────

object SleepScheduleManager {

    const val ACTION_BEDTIME_REMINDER = "com.calmspace.ACTION_BEDTIME_REMINDER"
    const val ACTION_WAKE_STOP        = "com.calmspace.ACTION_WAKE_STOP"

    private const val REQUEST_BEDTIME = 1001
    private const val REQUEST_WAKE    = 1002

    // ── Scheduling ────────────────────────────────────────────────────

    fun scheduleBedtimeReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = bedtimePendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(nextAlarmTimeMs(hour, minute), null),
            pendingIntent
        )
    }

    fun scheduleWakeStop(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = wakePendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(nextAlarmTimeMs(hour, minute), null),
            pendingIntent
        )
    }

    // ── Cancellation ──────────────────────────────────────────────────

    fun cancelBedtimeReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = bedtimePendingIntent(context, PendingIntent.FLAG_NO_CREATE)
            ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun cancelWakeStop(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = wakePendingIntent(context, PendingIntent.FLAG_NO_CREATE)
            ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    // ── Reschedule after boot ─────────────────────────────────────────

    fun rescheduleIfEnabled(context: Context) {
        val prefs = context.getSharedPreferences("calmspace_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("schedule_enabled", false)) return

        val bedHour   = prefs.getInt("bedtime_hour",   -1)
        val bedMinute = prefs.getInt("bedtime_minute",  -1)
        if (bedHour >= 0) scheduleBedtimeReminder(context, bedHour, bedMinute)

        val wakeHour   = prefs.getInt("wake_hour",   -1)
        val wakeMinute = prefs.getInt("wake_minute",  -1)
        if (wakeHour >= 0) scheduleWakeStop(context, wakeHour, wakeMinute)
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun nextAlarmTimeMs(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE,      minute)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

    private fun bedtimePendingIntent(context: Context, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context, REQUEST_BEDTIME,
            Intent(context, SleepScheduleReceiver::class.java).apply {
                action = ACTION_BEDTIME_REMINDER
            },
            flags or PendingIntent.FLAG_IMMUTABLE
        )

    private fun wakePendingIntent(context: Context, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context, REQUEST_WAKE,
            Intent(context, SleepScheduleReceiver::class.java).apply {
                action = ACTION_WAKE_STOP
            },
            flags or PendingIntent.FLAG_IMMUTABLE
        )
}
