package com.calmspace.masking

import kotlin.math.max

private const val DEFAULT_MASKING_BASELINE = 0.40f
private const val MASKING_MIN_VOLUME = 0.05f
private const val MASKING_MAX_VOLUME = 1.00f
private const val MAX_AUTOMATIC_INCREASE = 0.25f
private const val MAX_AUTOMATIC_DECREASE = 0.10f
private const val MIN_WINNER_SCORE = 0.35f
private const val MIN_WINNER_MARGIN = 0.10f

private const val SMOOTHING_WINDOW_MS = 2000L

private val bucketTargetOffset = mapOf(
    MaskingBucket.VOICE to 0.20f,
    MaskingBucket.HOUSEHOLD to 0.12f,
    MaskingBucket.TRAFFIC to 0.15f,
    MaskingBucket.NATURE to 0.00f,
    MaskingBucket.ALERT to -0.05f,
    MaskingBucket.UNKNOWN to 0.00f
)

private val bucketDisplayNames = mapOf(
    MaskingBucket.VOICE to "Speech / Human Activity",
    MaskingBucket.HOUSEHOLD to "Household Noise",
    MaskingBucket.TRAFFIC to "Outdoor / Traffic",
    MaskingBucket.NATURE to "Natural Ambient",
    MaskingBucket.ALERT to "Safety / Alert",
    MaskingBucket.UNKNOWN to "Unknown / Low Confidence"
)

enum class MaskingBucket {
    VOICE,
    HOUSEHOLD,
    TRAFFIC,
    NATURE,
    ALERT,
    UNKNOWN
}

data class MaskingDecision(
    val rawBucketScores: FloatArray,
    val smoothedBucketScores: FloatArray,
    val winner: MaskingBucket,
    val winnerScore: Float,
    val runnerUpScore: Float,
    val runnerUpBucket: MaskingBucket,
    val shouldAffectPlayback: Boolean,
    val targetVolume: Float,
    val reason: String,
    val displayWinner: String
)

class MaskingDecisionEngine {
    private val history = mutableListOf<Pair<Long, FloatArray>>() 
    private var lastTarget = DEFAULT_MASKING_BASELINE
    private var baselineVolume = DEFAULT_MASKING_BASELINE

    fun reset(newBaseline: Float = DEFAULT_MASKING_BASELINE) {
        baselineVolume = newBaseline.coerceIn(MASKING_MIN_VOLUME, MASKING_MAX_VOLUME)
        history.clear()
        lastTarget = baselineVolume
    }

    // Audit notes:
    // 1) Map top-5 labels into 6 buckets.
    // 2) Smooth scores with a fixed time window.
    // 3) Enforce winner/margin and priority rules.
    // 4) Convert winning bucket into a target volume.
    fun evaluate(
        topPredictions: List<Pair<String, Float>>,
        nowMs: Long,
        userBaseline: Float = DEFAULT_MASKING_BASELINE
    ): MaskingDecision {
        val clampedBaseline = userBaseline.coerceIn(MASKING_MIN_VOLUME, MASKING_MAX_VOLUME)
        if (clampedBaseline != baselineVolume) {
            baselineVolume = clampedBaseline
            lastTarget = baselineVolume
        }

        val raw = mapPredictions(topPredictions)
        val mappedTotal = raw.sum()
        raw[MaskingBucket.UNKNOWN.ordinal] += max(0f, 1f - mappedTotal)

        val smoothed = smoothBuckets(nowMs, raw)

        val ranking = smoothed.indices.sortedByDescending { smoothed[it] }
        val best = ranking[0]
        val second = ranking.getOrElse(1) { ranking[0] }
        val bestScore = smoothed[best]
        val secondScore = smoothed[second]
        val bestBucket = MaskingBucket.values()[best]
        val secondBucket = MaskingBucket.values()[second]
        val alertScore = smoothed[MaskingBucket.ALERT.ordinal]

        val winner: MaskingBucket
        val shouldAffectPlayback: Boolean
        val reason: String

        val alertValid = alertScore >= MIN_WINNER_SCORE

        // Rule priority: confidence checks first, then ALERT override.
        if (alertValid && alertScore >= bestScore - MIN_WINNER_MARGIN) {
            winner = MaskingBucket.ALERT
            shouldAffectPlayback = true
            reason = "Safety sound detected, reducing masking."
        } else if (bestScore >= MIN_WINNER_SCORE && (bestScore - secondScore) >= MIN_WINNER_MARGIN && bestBucket != MaskingBucket.UNKNOWN) {
            winner = bestBucket
            shouldAffectPlayback = true
            reason = when (winner) {
                MaskingBucket.ALERT -> "Safety sound detected, reducing masking."
                MaskingBucket.VOICE -> "Speech detected, increasing masking quickly."
                MaskingBucket.TRAFFIC -> "Traffic detected, increasing masking."
                MaskingBucket.HOUSEHOLD -> "Household noise detected, moderate increase."
                MaskingBucket.NATURE -> "Nature-like sounds detected, keeping baseline."
                else -> "Holding in place."
            }
        } else {
            winner = MaskingBucket.UNKNOWN
            shouldAffectPlayback = false
            reason = "Confidence/margin not sufficient; holding volume."
        }

        // Keep all decisions relative to the latest user baseline so volume automation
        // always stays anchored to the manual slider position.
        val clampedOffset = bucketTargetOffset[winner] ?: 0f
        val targetFromBaseline = baselineVolume + clampedOffset
        val minLimit = (baselineVolume - MAX_AUTOMATIC_DECREASE).coerceAtLeast(MASKING_MIN_VOLUME)
        val maxLimit = (baselineVolume + MAX_AUTOMATIC_INCREASE).coerceAtMost(MASKING_MAX_VOLUME)
        val computedTarget = targetFromBaseline.coerceIn(minLimit, maxLimit)
        val target = if (shouldAffectPlayback) computedTarget else lastTarget

        lastTarget = target

        return MaskingDecision(
            rawBucketScores = raw,
            smoothedBucketScores = smoothed,
            winner = winner,
            winnerScore = bestScore,
            runnerUpScore = secondScore,
            runnerUpBucket = secondBucket,
            shouldAffectPlayback = shouldAffectPlayback,
            targetVolume = target,
            reason = reason,
            displayWinner = bucketDisplayNames[winner] ?: winner.name
        )
    }

    private fun smoothBuckets(nowMs: Long, raw: FloatArray): FloatArray {
        val cutoffMs = nowMs - SMOOTHING_WINDOW_MS
        history.removeIf { it.first < cutoffMs }
        history.add(nowMs to raw)

        val averages = FloatArray(MaskingBucket.values().size)
        if (history.isEmpty()) return averages

        for (frame in history) {
            for (index in averages.indices) {
                averages[index] += frame.second[index]
            }
        }
        for (index in averages.indices) {
            averages[index] /= history.size.toFloat()
        }
        return averages
    }

    private fun mapPredictions(topPredictions: List<Pair<String, Float>>): FloatArray {
        val raw = FloatArray(MaskingBucket.values().size)
        for ((label, score) in topPredictions) {
            val bucket = mapLabelToBucket(label)
            raw[bucket.ordinal] += score
        }
        return raw
    }

    private fun mapLabelToBucket(label: String): MaskingBucket {
        val normalized = label.lowercase().replace('_', ' ')

        val voiceKeywords = listOf(
            "speech", "conversation", "narration", "whisper", "child", "laughter", "laugh",
            "television", "radio", "telecast", "narrative", "talk", "spoken"
        )

        val householdKeywords = listOf(
            "vacuum", "blender", "dryer", "dishwasher", "washing machine", "microwave",
            "air conditioner", "fan", "printer", "kitchen", "refrigerator", "toaster"
        )

        val trafficKeywords = listOf(
            "car", "truck", "bus", "motorcycle", "engine", "traffic", "train", "aircraft",
            "road", "vehicle", "motor"
        )

        val natureKeywords = listOf(
            "rain", "wind", "bird", "ocean", "stream", "water", "rustling", "leaf", "waterfall",
            "waves"
        )

        val alertKeywords = listOf(
            "alarm", "siren", "smoke", "beep", "knock", "doorbell", "breaking", "crash"
        )

        return when {
            voiceKeywords.any { it in normalized } -> MaskingBucket.VOICE
            householdKeywords.any { it in normalized } -> MaskingBucket.HOUSEHOLD
            trafficKeywords.any { it in normalized } -> MaskingBucket.TRAFFIC
            natureKeywords.any { it in normalized } -> MaskingBucket.NATURE
            alertKeywords.any { it in normalized } -> MaskingBucket.ALERT
            else -> MaskingBucket.UNKNOWN
        }
    }
}

fun smoothMaskingVolume(current: Float, target: Float, deltaMs: Long): Float {
    if (deltaMs <= 0L) return current

    // 3% every 300ms attack, 1% every 300ms release.
    val attackRatePerMs = 0.0001f
    val releaseRatePerMs = 0.0000334f
    val allowedDelta = if (target > current) attackRatePerMs else releaseRatePerMs
    return if (target > current) {
        (current + allowedDelta * deltaMs).coerceAtMost(target)
    } else {
        (current - allowedDelta * deltaMs).coerceAtLeast(target)
    }
}
