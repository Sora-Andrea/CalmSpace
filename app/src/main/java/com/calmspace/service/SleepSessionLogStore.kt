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
    private const val KEY_METRICS = "metrics"
    private const val KEY_BUCKET_DURATION_MS = "bucketDurationMs"
    private const val KEY_LABEL_HIT_COUNT = "labelHitCount"
    private const val KEY_MASKING_PLAYBACK_MS = "maskingPlaybackMs"

    data class SessionMetrics(
        val bucketDurationMs: Map<String, Long>,
        val labelHitCount: Map<String, Int>,
        val maskingPlaybackMs: Long
    )

    data class CompletedSessionRecord(
        val userId: String,
        val trackId: String?,
        val startedAtUtcMs: Long,
        val endedAtUtcMs: Long,
        val durationMs: Long,
        val endReason: String,
        val metrics: SessionMetrics? = null
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
        endReason: String,
        metrics: SessionMetrics? = null
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
            if (metrics != null) {
                put(KEY_METRICS, metricsToJson(metrics))
            }
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

    /**
     * Returns sessions for a specific user and falls back to legacy "local" rows
     * when scoped rows are not present yet.
     */
    fun getCompletedSessionsForUser(
        context: Context,
        userId: String
    ): List<CompletedSessionRecord> {
        val scoped = getCompletedSessions(context, userId)
        if (scoped.isNotEmpty() || userId == "local") {
            return scoped
        }
        return getCompletedSessions(context, "local")
    }

    /**
     * Returns most recent sessions for a specific user
     */
    fun getRecentSessionsForUser(
        context: Context,
        userId: String,
        limit: Int = 5
    ): List<CompletedSessionRecord> {
        val safeLimit = limit.coerceAtLeast(0)
        return getCompletedSessions(context, userId).take(safeLimit)
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
            trackId = if (obj.has("trackId") && !obj.isNull("trackId")) {
                obj.optString("trackId")
            } else {
                null
            },
            startedAtUtcMs = startedAtUtcMs,
            endedAtUtcMs = endedAtUtcMs,
            durationMs = durationMs,
            endReason = obj.optString("endReason", "unknown"),
            metrics = obj.optJSONObject(KEY_METRICS)?.let { metricsFromJson(it) }
        )
    }

    private fun metricsToJson(metrics: SessionMetrics): JSONObject {
        return JSONObject().apply {
            put(KEY_BUCKET_DURATION_MS, mapLongToJson(metrics.bucketDurationMs))
            put(KEY_LABEL_HIT_COUNT, mapIntToJson(metrics.labelHitCount))
            put(KEY_MASKING_PLAYBACK_MS, metrics.maskingPlaybackMs.coerceAtLeast(0L))
        }
    }

    private fun metricsFromJson(obj: JSONObject): SessionMetrics {
        val bucketDurationMs = jsonToLongMap(obj.optJSONObject(KEY_BUCKET_DURATION_MS))
        val labelHitCount = jsonToIntMap(obj.optJSONObject(KEY_LABEL_HIT_COUNT))
        val maskingPlaybackMs = obj.optLong(KEY_MASKING_PLAYBACK_MS, 0L).coerceAtLeast(0L)
        return SessionMetrics(
            bucketDurationMs = bucketDurationMs,
            labelHitCount = labelHitCount,
            maskingPlaybackMs = maskingPlaybackMs
        )
    }

    private fun mapLongToJson(map: Map<String, Long>): JSONObject {
        return JSONObject().apply {
            map.forEach { (key, value) ->
                put(key, value.coerceAtLeast(0L))
            }
        }
    }

    private fun mapIntToJson(map: Map<String, Int>): JSONObject {
        return JSONObject().apply {
            map.forEach { (key, value) ->
                put(key, value.coerceAtLeast(0))
            }
        }
    }

    private fun jsonToLongMap(obj: JSONObject?): Map<String, Long> {
        if (obj == null) return emptyMap()
        val map = LinkedHashMap<String, Long>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = obj.optLong(key, 0L).coerceAtLeast(0L)
        }
        return map
    }

    private fun jsonToIntMap(obj: JSONObject?): Map<String, Int> {
        if (obj == null) return emptyMap()
        val map = LinkedHashMap<String, Int>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = obj.optInt(key, 0).coerceAtLeast(0)
        }
        return map
    }
}
