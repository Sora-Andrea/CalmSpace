package com.calmspace.masking

import kotlin.math.exp
import kotlin.math.max

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
    private val policy: MaskingDecisionPolicy = MaskingDecisionProfiles.V1,
    private val labelToBucket: (String) -> MaskingBucket = { _ ->
        MaskingBucket.UNKNOWN
    }
) {
    constructor(
        smoothingTauMs: Long = MaskingDecisionProfiles.V1.smoothingTauMs,
        consensusRequiredSteps: Int = MaskingDecisionProfiles.V1.consensusRequiredSteps,
        unknownRecoverySteps: Int = MaskingDecisionProfiles.V1.unknownRecoverySteps,
        winnerHoldTimeoutMs: Long = MaskingDecisionProfiles.V1.winnerHoldTimeoutMs,
        attackRatePerMs: Float = MaskingDecisionProfiles.V1.attackRatePerMs,
        releaseRatePerMs: Float = MaskingDecisionProfiles.V1.releaseRatePerMs,
        minWinnerScore: Float = MaskingDecisionProfiles.V1.minWinnerScore,
        minWinnerMargin: Float = MaskingDecisionProfiles.V1.minWinnerMargin,
        strongWinnerScore: Float = MaskingDecisionProfiles.V1.strongWinnerScore,
        labelToBucket: (String) -> MaskingBucket = { _ ->
            MaskingBucket.UNKNOWN
        }
    ) : this(
        policy = MaskingDecisionPolicy(
            smoothingTauMs = smoothingTauMs,
            consensusRequiredSteps = consensusRequiredSteps,
            unknownRecoverySteps = unknownRecoverySteps,
            attackRatePerMs = attackRatePerMs,
            releaseRatePerMs = releaseRatePerMs,
            minWinnerScore = minWinnerScore,
            minWinnerMargin = minWinnerMargin,
            strongWinnerScore = strongWinnerScore,
            winnerHoldTimeoutMs = winnerHoldTimeoutMs
        ),
        labelToBucket = labelToBucket
    )

    private val smoothedBuckets = FloatArray(MaskingBucket.values().size)
    private var hasSmoothingState = false
    private var lastSmoothingMs = 0L

    private var pendingWinner: MaskingBucket? = null
    private var pendingWinnerCount = 0
    private var confirmedWinner = MaskingBucket.UNKNOWN
    private var unknownHoldCount = 0
    private var lastNonUnknownPredictionMs = 0L

    private var lastTarget = policy.baselineVolume
    private var baselineVolume = policy.baselineVolume

    fun reset(newBaseline: Float = policy.baselineVolume) {
        baselineVolume = newBaseline.coerceIn(policy.minVolume, policy.maxVolume)
        hasSmoothingState = false
        lastSmoothingMs = 0L
        pendingWinner = null
        pendingWinnerCount = 0
        confirmedWinner = MaskingBucket.UNKNOWN
        unknownHoldCount = 0
        lastNonUnknownPredictionMs = 0L
        lastTarget = baselineVolume
    }

    // Audit notes:
    // 1) Map top-N labels into masking buckets.
    // 2) Smooth scores with EMA for lower decision latency.
    // 3) Enforce winner/margin, configurable consensus, and priority rules.
    // 4) Convert winning bucket into a target volume.
    fun evaluate(
        topPredictions: List<Pair<String, Float>>,
        nowMs: Long,
        userBaseline: Float = policy.baselineVolume
    ): MaskingDecision {
        val clampedBaseline = userBaseline.coerceIn(policy.minVolume, policy.maxVolume)
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

        if (mappedRanking.isNotEmpty()) {
            lastNonUnknownPredictionMs = nowMs
        }

        val canKeepWinnerPastTimeout = canKeepWinnerPastTimeout(nowMs)

        val rawWinner: MaskingBucket
        val winnerReason: String
        val bestScore: Float
        val secondScore: Float
        val secondBucket: MaskingBucket

        if (mappedRanking.isEmpty()) {
            bestScore = 0f
            secondScore = 0f
            secondBucket = MaskingBucket.UNKNOWN
            rawWinner = MaskingBucket.UNKNOWN
            winnerReason = when {
                canKeepWinnerPastTimeout -> "No mapped bucket in this window; holding current bucket."
                confirmedWinner != MaskingBucket.UNKNOWN -> "No mapped bucket in this window; winner expired."
                else -> "No mapped bucket in this window; holding current value."
            }
        } else {
            val best = mappedRanking[0]
            val second = mappedRanking.getOrElse(1) { mappedRanking[0] }
            val bestBucket = MaskingBucket.values()[best]
            secondBucket = MaskingBucket.values()[second]
            bestScore = smoothed[best]
            secondScore = smoothed[second]

            if (bestScore >= policy.minWinnerScore &&
                (mappedRanking.size == 1 ||
                        (bestScore - secondScore) >= policy.minWinnerMargin ||
                        bestScore >= policy.strongWinnerScore)
            ) {
                rawWinner = bestBucket
                winnerReason = when (rawWinner) {
                    MaskingBucket.VOICE -> "Speech detected, increasing masking quickly."
                    MaskingBucket.TRAFFIC -> "Traffic detected (including alert-like sounds), increasing masking."
                    MaskingBucket.HOUSEHOLD -> "Household noise detected, moderate increase."
                    MaskingBucket.NATURE -> "Nature-like sounds detected, keeping baseline."
                    else -> "Holding in place."
                }
            } else {
                rawWinner = MaskingBucket.UNKNOWN
                winnerReason = if (canKeepWinnerPastTimeout) {
                    "Confidence/margin not sufficient; holding current bucket."
                } else {
                    "Confidence/margin not sufficient; volume hold."
                }
            }
        }

        val consensusWinner = resolveWinnerWithConsensus(rawWinner, nowMs)
        val shouldAffectPlayback = consensusWinner != MaskingBucket.UNKNOWN && consensusWinner == rawWinner

        val clampedOffset = policy.bucketTargetOffsets[consensusWinner] ?: 0f
        val targetFromBaseline = baselineVolume + clampedOffset
        val minLimit = (baselineVolume - policy.maxAutomaticDecrease).coerceAtLeast(policy.minVolume)
        val maxLimit = (baselineVolume + policy.maxAutomaticIncrease).coerceAtMost(policy.maxVolume)
        val computedTarget = targetFromBaseline.coerceIn(minLimit, maxLimit)

        val target = if (shouldAffectPlayback) computedTarget else lastTarget
        val finalReason = when {
            shouldAffectPlayback -> winnerReason
            consensusWinner == MaskingBucket.UNKNOWN -> winnerReason
            else -> "Waiting for consensus: ${pendingWinnerCount.coerceIn(0, policy.consensusRequiredSteps)}/${policy.consensusRequiredSteps}"
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
            displayWinner = maskingBucketDisplayName(consensusWinner)
        )
    }

    private fun canKeepWinnerPastTimeout(nowMs: Long): Boolean {
        return policy.winnerHoldTimeoutMs > 0L &&
                confirmedWinner != MaskingBucket.UNKNOWN &&
                lastNonUnknownPredictionMs > 0L &&
                (nowMs - lastNonUnknownPredictionMs) < policy.winnerHoldTimeoutMs
    }

    private fun resolveWinnerWithConsensus(
        rawWinner: MaskingBucket,
        nowMs: Long
    ): MaskingBucket {
        if (rawWinner == MaskingBucket.UNKNOWN) {
            if (confirmedWinner != MaskingBucket.UNKNOWN && canKeepWinnerPastTimeout(nowMs)) {
                return confirmedWinner
            }
            pendingWinner = null
            pendingWinnerCount = 0
            unknownHoldCount = (unknownHoldCount + 1).coerceAtMost(policy.unknownRecoverySteps.coerceAtLeast(1))
            if (unknownHoldCount >= policy.unknownRecoverySteps.coerceAtLeast(1)) {
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

        val requiredSteps = policy.consensusRequiredSteps.coerceAtLeast(1)
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
        val alpha = if (policy.smoothingTauMs <= 0L) {
            1f
        } else {
            (1f - exp(-elapsedMs / policy.smoothingTauMs.toFloat())).coerceIn(0f, 1f)
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
            val mapped = labelToBucket(label)
            val bucket = when (mapped) {
                MaskingBucket.ALERT -> MaskingBucket.TRAFFIC
                else -> mapped
            }
            raw[bucket.ordinal] += score
        }
        return raw
    }
}

fun smoothMaskingVolume(current: Float, target: Float, deltaMs: Long): Float {
    return smoothMaskingVolume(
        current,
        target,
        deltaMs,
        MaskingDecisionProfiles.V1.attackRatePerMs,
        MaskingDecisionProfiles.V1.releaseRatePerMs
    )
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
