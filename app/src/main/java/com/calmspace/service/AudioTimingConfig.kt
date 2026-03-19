package com.calmspace.service

import com.calmspace.masking.MaskingDecisionPolicy

object AudioTimingConfig {
    // ExoPlayer track playback fade-in curve.
    const val EXO_PLAYBACK_FADE_IN_DURATION_MS = 2500L
    const val EXO_PLAYBACK_FADE_IN_STEP_MS = 16L

    // Generated noise playback fade-in curve from service noise thread.
    const val GENERATED_NOISE_FADE_IN_DURATION_MS = 2500L
    const val GENERATED_NOISE_FADE_IN_STEP_MS = 16L

    // Headphone safety fade settings for generated playback.
    const val GENERATED_NOISE_HEADPHONE_FADE_IN_DURATION_MS = 1500L
    const val GENERATED_NOISE_HEADPHONE_FADE_IN_STEPS = 30

    // Shared YAMNet masking inference timing.
    const val MASKING_INFERENCE_MS = 180L
    const val MASKING_TOP_PREDICTIONS = 20

    // Tunables for the shared V1 masking profile.
    const val MASKING_VOLUME_ATTACK_RATE_PER_MS = 0.00030f
    const val MASKING_VOLUME_RELEASE_RATE_PER_MS = 0.00005f
    const val MASKING_SMOOTHING_EMA_TAU_MS = 280L
    const val MASKING_UNKNOWN_RECOVERY_STEPS = 2
    const val MASKING_CONSENSUS_REQUIRED_STEPS = 2
    // Keep confirmed bucket label alive for this long before allowing it to
    // fall back to unknown when no non-UNKNOWN bucket appears.
    const val MASKING_WINNER_HOLD_TIMEOUT_MS = 30_000L
    const val MASKING_MIN_WINNER_SCORE = 0.05f
    const val MASKING_MIN_WINNER_MARGIN = 0.010f
    const val MASKING_STRONG_WINNER_SCORE = 0.40f

    val MASKING_DECISION_PROFILE = MaskingDecisionPolicy(
        smoothingTauMs = MASKING_SMOOTHING_EMA_TAU_MS,
        consensusRequiredSteps = MASKING_CONSENSUS_REQUIRED_STEPS,
        unknownRecoverySteps = MASKING_UNKNOWN_RECOVERY_STEPS,
        attackRatePerMs = MASKING_VOLUME_ATTACK_RATE_PER_MS,
        releaseRatePerMs = MASKING_VOLUME_RELEASE_RATE_PER_MS,
        minWinnerScore = MASKING_MIN_WINNER_SCORE,
        minWinnerMargin = MASKING_MIN_WINNER_MARGIN,
        strongWinnerScore = MASKING_STRONG_WINNER_SCORE,
        winnerHoldTimeoutMs = MASKING_WINNER_HOLD_TIMEOUT_MS
    )

    // tau is the time constant for EMA smoothing:
    // TAU = MASKING_SMOOTHING_EMA_TAU_MS
    // alpha = 1 - exp(-dt / TAU)
    // Smaller TAU = faster reaction, less smoothing.
    // Larger TAU = slower reaction, smoother output.

    // Existing call sites expect one alias for playback fade duration.
    const val AMBIENT_PLAYBACK_FADE_IN_DURATION_MS = EXO_PLAYBACK_FADE_IN_DURATION_MS
}
