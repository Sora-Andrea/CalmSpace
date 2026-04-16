package com.calmspace.service

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * persistent session log store that keeps one active session marker (start timestamps)
 * and appends completed sessions to a JSON array for future use.
 */
object SleepSessionLogStore {
    private const val PREFS_NAME = "sleep_session_log"
    private const val KEY_ACTIVE_START_UTC_MS = "active_start_utc_ms"
    private const val KEY_ACTIVE_START_ELAPSED_MS = "active_start_elapsed_ms"
    private const val KEY_ACTIVE_USER_ID = "active_user_id"
    private const val KEY_ACTIVE_TRACK_ID = "active_track_id"
    private const val KEY_COMPLETED_SESSIONS_JSON = "completed_sessions_json"

    data class CompletedSessionRecord(
        val userId: String,
        val trackId: String?,
        val startedAtUtcMs: Long,
        val endedAtUtcMs: Long,
        val durationMs: Long,
        val endReason: String
    )

    fun markSessionStarted(
        context: Context,
        userId: String,
        trackId: String?,
        startUtcMs: Long,
        startElapsedMs: Long
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_ACTIVE_START_UTC_MS, startUtcMs)
            .putLong(KEY_ACTIVE_START_ELAPSED_MS, startElapsedMs)
            .putString(KEY_ACTIVE_USER_ID, userId)
            .putString(KEY_ACTIVE_TRACK_ID, trackId)
            .apply()
    }

    fun markSessionEnded(
        context: Context,
        endUtcMs: Long,
        endElapsedMs: Long,
        endReason: String
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val startUtcMs = prefs.getLong(KEY_ACTIVE_START_UTC_MS, -1L)
        val startElapsedMs = prefs.getLong(KEY_ACTIVE_START_ELAPSED_MS, -1L)
        if (startUtcMs <= 0L || startElapsedMs < 0L) {
            clearActiveSession(context)
            return
        }

        val durationMs = (endElapsedMs - startElapsedMs).coerceAtLeast(0L)
        val userId = prefs.getString(KEY_ACTIVE_USER_ID, "local").orEmpty()
        val trackId = prefs.getString(KEY_ACTIVE_TRACK_ID, null)

        val existing = prefs.getString(KEY_COMPLETED_SESSIONS_JSON, null)
        val array = runCatching { JSONArray(existing ?: "[]") }.getOrElse { JSONArray() }

        val record = JSONObject().apply {
            put("userId", userId)
            put("trackId", trackId)
            put("startedAtUtcMs", startUtcMs)
            put("endedAtUtcMs", endUtcMs)
            put("durationMs", durationMs)
            put("endReason", endReason)
        }
        array.put(record)

        prefs.edit()
            .putString(KEY_COMPLETED_SESSIONS_JSON, array.toString())
            .remove(KEY_ACTIVE_START_UTC_MS)
            .remove(KEY_ACTIVE_START_ELAPSED_MS)
            .remove(KEY_ACTIVE_USER_ID)
            .remove(KEY_ACTIVE_TRACK_ID)
            .apply()
    }

    fun clearActiveSession(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACTIVE_START_UTC_MS)
            .remove(KEY_ACTIVE_START_ELAPSED_MS)
            .remove(KEY_ACTIVE_USER_ID)
            .remove(KEY_ACTIVE_TRACK_ID)
            .apply()
    }

    fun getCompletedSessions(
        context: Context,
        userId: String? = null
    ): List<CompletedSessionRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_COMPLETED_SESSIONS_JSON, "[]").orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val records = mutableListOf<CompletedSessionRecord>()

        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val parsed = parseCompletedSession(obj) ?: continue
            if (userId == null || parsed.userId == userId) {
                records += parsed
            }
        }

        return records.sortedByDescending { it.endedAtUtcMs }
    }

    private fun parseCompletedSession(obj: JSONObject): CompletedSessionRecord? {
        val startedAtUtcMs = obj.optLong("startedAtUtcMs", -1L)
        val endedAtUtcMs = obj.optLong("endedAtUtcMs", -1L)
        val durationRaw = obj.optLong("durationMs", -1L)

        if (startedAtUtcMs <= 0L || endedAtUtcMs <= 0L) return null
        val durationMs = if (durationRaw >= 0L) {
            durationRaw
        } else {
            (endedAtUtcMs - startedAtUtcMs).coerceAtLeast(0L)
        }

        return CompletedSessionRecord(
            userId = obj.optString("userId", "local"),
            trackId = obj.optString("trackId", null),
            startedAtUtcMs = startedAtUtcMs,
            endedAtUtcMs = endedAtUtcMs,
            durationMs = durationMs,
            endReason = obj.optString("endReason", "unknown")
        )
    }
}
