package com.calmspace.masking

data class MaskingDecisionPolicy(
    val smoothingTauMs: Long = 700L,
    val consensusRequiredSteps: Int = 2,
    val unknownRecoverySteps: Int = 2,
    val attackRatePerMs: Float = 0.00030f,
    val releaseRatePerMs: Float = 0.00005f,
    val minWinnerScore: Float = 0.05f,
    val minWinnerMargin: Float = 0.010f,
    val strongWinnerScore: Float = 0.40f,
    val baselineVolume: Float = 0.40f,
    val minVolume: Float = 0.05f,
    val maxVolume: Float = 1.00f,
    val maxAutomaticIncrease: Float = 0.25f,
    val maxAutomaticDecrease: Float = 0.10f,
    val winnerHoldTimeoutMs: Long = 30_000L,
    val bucketTargetOffsets: Map<MaskingBucket, Float> = mapOf(
        MaskingBucket.VOICE to 0.20f,
        MaskingBucket.HOUSEHOLD to 0.12f,
        MaskingBucket.TRAFFIC to 0.15f,
        MaskingBucket.NATURE to 0.00f,
        MaskingBucket.ALERT to 0.15f,
        MaskingBucket.UNKNOWN to 0.00f
    )
)

object MaskingDecisionProfiles {
    val V1 = MaskingDecisionPolicy()
}

fun maskingBucketDisplayName(bucket: MaskingBucket): String = when (bucket) {
    MaskingBucket.VOICE -> "Speech / Human Activity"
    MaskingBucket.HOUSEHOLD -> "Household Noise"
    MaskingBucket.TRAFFIC -> "Outdoor / Traffic"
    MaskingBucket.NATURE -> "Natural Ambient"
    MaskingBucket.ALERT -> "Outdoor / Traffic"
    MaskingBucket.UNKNOWN -> "Unknown / Low Confidence"
}

fun bucketScoresToDisplayList(scores: FloatArray): List<Pair<String, Float>> =
    MaskingBucket.values().mapIndexed { index, bucket ->
        maskingBucketDisplayName(bucket) to scores[index]
    }
