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

    // Existing calls reference this shared alias. KEEP.
    const val AMBIENT_PLAYBACK_FADE_IN_DURATION_MS = EXO_PLAYBACK_FADE_IN_DURATION_MS
}
