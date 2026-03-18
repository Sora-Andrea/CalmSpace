package com.calmspace.service

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


    // YAMNet masking control tuning.
    const val MASKING_INFERENCE_MS = 180L
    const val MASKING_VOLUME_ATTACK_RATE_PER_MS = 0.00030f
    const val MASKING_VOLUME_RELEASE_RATE_PER_MS = 0.00005f
    const val MASKING_SMOOTHING_EMA_TAU_MS = 280L
    const val MASKING_UNKNOWN_RECOVERY_STEPS = 2
    const val MASKING_CONSENSUS_REQUIRED_STEPS = 1
    const val MASKING_MIN_WINNER_SCORE = 0.05f
    const val MASKING_MIN_WINNER_MARGIN = 0.010f
    const val MASKING_STRONG_WINNER_SCORE = 0.40f
    const val MASKING_TOP_PREDICTIONS = 20

//tau is the time constant for the EMA smoothing
//TAU  “how quickly old values decay and new predictions take over.”
//With 280ms, each new frame pulls the smoothed score toward current raw score at a moderate speed instead of jumping abruptly.
//alpha = 1 - exp(-dt / TAU)
//TAU = MASKING_SMOOTHING_EMA_TAU_MS
//Smaller TAU = faster reaction, less smoothing
//Larger TAU = slower reaction, smoother output

    // ---------------------------------------------------------------
    //                      DO NOT MODIFY
    // ---------------------------------------------------------------
   
    // Existing calls reference this shared alias. KEEP.
    const val AMBIENT_PLAYBACK_FADE_IN_DURATION_MS = EXO_PLAYBACK_FADE_IN_DURATION_MS

    // Backward-compatible tuning names kept to avoid unresolved references errors
    // due active refactors and branch merges (FIX THIS LATER).
    const val SMOOTHING_EMA_TAU_MS = MASKING_SMOOTHING_EMA_TAU_MS
    const val CONSENSUS_REQUIRED_STEPS = MASKING_CONSENSUS_REQUIRED_STEPS
    const val UNKNOWN_RECOVERY_STEPS = MASKING_UNKNOWN_RECOVERY_STEPS
}
