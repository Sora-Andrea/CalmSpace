package com.calmspace.masking

import kotlin.math.exp
import kotlin.math.max

private const val DEFAULT_SMOOTHING_EMA_TAU_MS = 700L
private const val DEFAULT_CONSENSUS_REQUIRED_STEPS = 2
private const val DEFAULT_ATTACK_RATE_PER_MS = 0.00030f
private const val DEFAULT_RELEASE_RATE_PER_MS = 0.00005f
private const val DEFAULT_MIN_WINNER_SCORE = 0.05f
private const val DEFAULT_MIN_WINNER_MARGIN = 0.010f
private const val DEFAULT_STRONG_WINNER_SCORE = 0.40f
private const val DEFAULT_UNKNOWN_RECOVERY_STEPS = 2

private const val DEFAULT_MASKING_BASELINE = 0.40f
private const val MASKING_MIN_VOLUME = 0.05f
private const val MASKING_MAX_VOLUME = 1.00f
private const val MAX_AUTOMATIC_INCREASE = 0.25f
private const val MAX_AUTOMATIC_DECREASE = 0.10f

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
private val fallbackBucketRules = YamnetLabelBucketRules.orderedRulesFromSource()

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

class MaskingDecisionEngine(
    private val smoothingTauMs: Long = DEFAULT_SMOOTHING_EMA_TAU_MS,
    private val consensusRequiredSteps: Int = DEFAULT_CONSENSUS_REQUIRED_STEPS,
    private val unknownRecoverySteps: Int = DEFAULT_UNKNOWN_RECOVERY_STEPS,
    private val attackRatePerMs: Float = DEFAULT_ATTACK_RATE_PER_MS,
    private val releaseRatePerMs: Float = DEFAULT_RELEASE_RATE_PER_MS,
    private val minWinnerScore: Float = DEFAULT_MIN_WINNER_SCORE,
    private val minWinnerMargin: Float = DEFAULT_MIN_WINNER_MARGIN,
    private val strongWinnerScore: Float = DEFAULT_STRONG_WINNER_SCORE,
    private val labelToBucket: (String) -> MaskingBucket = { label ->
        YamnetLabelBucketRules.classify(label, fallbackBucketRules)
    }
) {
    private val smoothedBuckets = FloatArray(MaskingBucket.values().size)
    private var hasSmoothingState = false
    private var lastSmoothingMs = 0L

    private var pendingWinner: MaskingBucket? = null
    private var pendingWinnerCount = 0
    private var confirmedWinner = MaskingBucket.UNKNOWN
    private var unknownHoldCount = 0

    private var lastTarget = DEFAULT_MASKING_BASELINE
    private var baselineVolume = DEFAULT_MASKING_BASELINE

    fun reset(newBaseline: Float = DEFAULT_MASKING_BASELINE) {
        baselineVolume = newBaseline.coerceIn(MASKING_MIN_VOLUME, MASKING_MAX_VOLUME)
        hasSmoothingState = false
        lastSmoothingMs = 0L
        pendingWinner = null
        pendingWinnerCount = 0
        confirmedWinner = MaskingBucket.UNKNOWN
        lastTarget = baselineVolume
    }

    // Audit notes:
    // 1) Map top-N labels into 6 buckets.
    // 2) Smooth scores with EMA for lower decision latency.
    // 3) Enforce winner/margin, configurable consensus, and priority rules.
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

        val mappedRanking = smoothed.indices
            .filter { it != MaskingBucket.UNKNOWN.ordinal }
            .sortedByDescending { smoothed[it] }

        // If no bucket received any mass this frame, hold in UNKNOWN.
        // This avoids index errors and avoids hard-failing when the current
        // top labels are outside all defined buckets.
        if (mappedRanking.isEmpty()) {
            return MaskingDecision(
                rawBucketScores = raw,
                smoothedBucketScores = smoothed,
                winner = MaskingBucket.UNKNOWN,
                winnerScore = 0f,
                runnerUpScore = 0f,
                runnerUpBucket = MaskingBucket.UNKNOWN,
                shouldAffectPlayback = false,
                targetVolume = lastTarget,
                reason = "No mapped bucket received mass in this window; holding.",
                displayWinner = bucketDisplayNames[MaskingBucket.UNKNOWN] ?: MaskingBucket.UNKNOWN.name
            )
        }

        val best = mappedRanking[0]
        val second = mappedRanking.getOrElse(1) { mappedRanking[0] }
        val bestScore = smoothed[best]
        val secondScore = smoothed[second]
        val bestBucket = MaskingBucket.values()[best]
        val secondBucket = MaskingBucket.values()[second]
        val alertScore = smoothed[MaskingBucket.ALERT.ordinal]

        val rawWinner: MaskingBucket
        val winnerReason: String

        val alertValid = alertScore >= minWinnerScore

        if (alertValid && alertScore >= bestScore - minWinnerMargin) {
            rawWinner = MaskingBucket.ALERT
            winnerReason = "Safety sound detected, reducing masking."
        } else if (bestScore >= minWinnerScore && (
                mappedRanking.size == 1 ||
                (bestScore - secondScore) >= minWinnerMargin ||
                bestScore >= strongWinnerScore
            )) {
            rawWinner = bestBucket
            winnerReason = when (rawWinner) {
                MaskingBucket.ALERT -> "Safety sound detected, reducing masking."
                MaskingBucket.VOICE -> "Speech detected, increasing masking quickly."
                MaskingBucket.TRAFFIC -> "Traffic detected, increasing masking."
                MaskingBucket.HOUSEHOLD -> "Household noise detected, moderate increase."
                MaskingBucket.NATURE -> "Nature-like sounds detected, keeping baseline."
                else -> "Holding in place."
            }
        } else {
            rawWinner = MaskingBucket.UNKNOWN
            winnerReason = "Confidence/margin not sufficient; holding volume."
        }

        val consensusWinner = resolveWinnerWithConsensus(rawWinner)
        val shouldAffectPlayback = consensusWinner != MaskingBucket.UNKNOWN && consensusWinner == rawWinner

        val clampedOffset = bucketTargetOffset[consensusWinner] ?: 0f
        val targetFromBaseline = baselineVolume + clampedOffset
        val minLimit = (baselineVolume - MAX_AUTOMATIC_DECREASE).coerceAtLeast(MASKING_MIN_VOLUME)
        val maxLimit = (baselineVolume + MAX_AUTOMATIC_INCREASE).coerceAtMost(MASKING_MAX_VOLUME)
        val computedTarget = targetFromBaseline.coerceIn(minLimit, maxLimit)

        val target = if (shouldAffectPlayback) computedTarget else lastTarget
        val finalReason = when {
            shouldAffectPlayback -> winnerReason
            consensusWinner == MaskingBucket.UNKNOWN -> winnerReason
            else -> "Waiting for consensus: ${pendingWinnerCount.coerceIn(0, consensusRequiredSteps)}/$consensusRequiredSteps"
        }

        lastTarget = target

        return MaskingDecision(
            rawBucketScores = raw,
            smoothedBucketScores = smoothed,
            winner = consensusWinner,
            winnerScore = bestScore,
            runnerUpScore = secondScore,
            runnerUpBucket = secondBucket,
            shouldAffectPlayback = shouldAffectPlayback,
            targetVolume = target,
            reason = finalReason,
            displayWinner = bucketDisplayNames[consensusWinner] ?: consensusWinner.name
        )
    }

    private fun resolveWinnerWithConsensus(rawWinner: MaskingBucket): MaskingBucket {
        if (rawWinner == MaskingBucket.ALERT) {
            pendingWinner = null
            pendingWinnerCount = 0
            unknownHoldCount = 0
            confirmedWinner = rawWinner
            return rawWinner
        }

        if (rawWinner == MaskingBucket.UNKNOWN) {
            pendingWinner = null
            pendingWinnerCount = 0
            unknownHoldCount = (unknownHoldCount + 1).coerceAtMost(unknownRecoverySteps.coerceAtLeast(1))
            if (unknownHoldCount >= unknownRecoverySteps.coerceAtLeast(1)) {
                confirmedWinner = rawWinner
                return rawWinner
            }
            return confirmedWinner
        }

        if (rawWinner == confirmedWinner) {
            pendingWinner = null
            pendingWinnerCount = 0
            unknownHoldCount = 0
            return confirmedWinner
        }

        if (pendingWinner != rawWinner) {
            pendingWinner = rawWinner
            pendingWinnerCount = 1
            unknownHoldCount = 0
        } else {
            pendingWinnerCount++
        }

        val requiredSteps = consensusRequiredSteps.coerceAtLeast(1)
        if (pendingWinnerCount >= requiredSteps) {
            confirmedWinner = rawWinner
            pendingWinner = null
            pendingWinnerCount = 0
            unknownHoldCount = 0
        }

        return confirmedWinner
    }

    private fun smoothBuckets(nowMs: Long, raw: FloatArray): FloatArray {
        val bucketCount = smoothedBuckets.size
        val smoothed = FloatArray(bucketCount)

        if (!hasSmoothingState) {
            raw.copyInto(smoothedBuckets)
            hasSmoothingState = true
            lastSmoothingMs = nowMs
            smoothedBuckets.copyInto(smoothed)
            return smoothed
        }

        val elapsedMs = (nowMs - lastSmoothingMs).coerceAtLeast(1L).toFloat()
        val alpha = if (smoothingTauMs <= 0L) {
            1f
        } else {
            (1f - exp(-elapsedMs / smoothingTauMs.toFloat())).coerceIn(0f, 1f)
        }

        for (i in 0 until bucketCount) {
            smoothedBuckets[i] = (1f - alpha) * smoothedBuckets[i] + alpha * raw[i]
            smoothed[i] = smoothedBuckets[i]
        }
        lastSmoothingMs = nowMs
        return smoothed
    }

    private fun mapPredictions(topPredictions: List<Pair<String, Float>>): FloatArray {
        val raw = FloatArray(MaskingBucket.values().size)
        for ((label, score) in topPredictions) {
            val bucket = labelToBucket(label)
            raw[bucket.ordinal] += score
        }
        return raw
    }

}

fun smoothMaskingVolume(current: Float, target: Float, deltaMs: Long): Float {
    return smoothMaskingVolume(current, target, deltaMs, DEFAULT_ATTACK_RATE_PER_MS, DEFAULT_RELEASE_RATE_PER_MS)
}

fun smoothMaskingVolume(
    current: Float,
    target: Float,
    deltaMs: Long,
    attackRatePerMs: Float,
    releaseRatePerMs: Float
): Float {
    if (deltaMs <= 0L) return current

    val clampedAttackRate = attackRatePerMs.coerceAtLeast(0.000001f)
    val clampedReleaseRate = releaseRatePerMs.coerceAtLeast(0.000001f)
    val allowedDelta = if (target > current) clampedAttackRate else clampedReleaseRate
    return if (target > current) {
        (current + allowedDelta * deltaMs).coerceAtMost(target)
    } else {
        (current - allowedDelta * deltaMs).coerceAtLeast(target)
    }
}
