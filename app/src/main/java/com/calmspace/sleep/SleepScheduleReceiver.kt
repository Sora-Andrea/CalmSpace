package com.calmspace.sleep

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.calmspace.MainActivity
import com.calmspace.service.NoiseMonitorService

// ─────────────────────────────────────────────────────────────────────
// Sleep Schedule Receiver
//
// Handles three broadcast actions:
//   ACTION_BEDTIME_REMINDER — shows a notification to start a session
//   ACTION_WAKE_STOP        — stops the active session + shows notification
//   ACTION_BOOT_COMPLETED   — reschedules alarms after device reboot
//
// Each alarm re-schedules itself for the following night before returning.
// ─────────────────────────────────────────────────────────────────────

class SleepScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ensureNotificationChannel(context)
        val prefs = context.getSharedPreferences("calmspace_prefs", Context.MODE_PRIVATE)

        when (intent.action) {

            SleepScheduleManager.ACTION_BEDTIME_REMINDER -> {
                showNotification(
                    context,
                    id      = NOTIF_BEDTIME_ID,
                    title   = "Time for sleep",
                    text    = "Tap to start your CalmSpace session",
                    withOpenIntent = true
                )
                // Re-schedule for next night
                val h = prefs.getInt("bedtime_hour",   -1)
                val m = prefs.getInt("bedtime_minute",  -1)
                if (h >= 0) SleepScheduleManager.scheduleBedtimeReminder(context, h, m)
            }

            SleepScheduleManager.ACTION_WAKE_STOP -> {
                // Stop the monitoring service if it's running
                context.stopService(Intent(context, NoiseMonitorService::class.java))
                showNotification(
                    context,
                    id    = NOTIF_WAKE_ID,
                    title = "Good morning",
                    text  = "Your scheduled sleep session has ended"
                )
                // Re-schedule for next morning
                val h = prefs.getInt("wake_hour",   -1)
                val m = prefs.getInt("wake_minute",  -1)
                if (h >= 0) SleepScheduleManager.scheduleWakeStop(context, h, m)
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                SleepScheduleManager.rescheduleIfEnabled(context)
            }
        }
    }

    // ── Notifications ─────────────────────────────────────────────────

    private fun showNotification(
        context: Context,
        id: Int,
        title: String,
        text: String,
        withOpenIntent: Boolean = false
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (withOpenIntent) {
            val openIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(openIntent)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, builder.build())
    }

    private fun ensureNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Sleep Schedule",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Bedtime reminders and wake notifications"
            }
        )
    }

    companion object {
        const val CHANNEL_ID      = "calmspace_schedule"
        const val NOTIF_BEDTIME_ID = 2001
        const val NOTIF_WAKE_ID    = 2002
    }
}
