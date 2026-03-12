package com.calmspace.service

// ─────────────────────────────────────────────────────────────────────
// Sound Type
//
// Defines available sleep sound options. All types are generated
// algorithmically — no audio files required.
//
// Noise "colors" are named by analogy to light — they describe the
// frequency balance of the sound, from bass-heavy to treble-heavy.
// ─────────────────────────────────────────────────────────────────────

enum class SoundType(
    val displayName: String,
    val description: String,
    val detail: String
) {
    WHITE_NOISE(displayName = "Bright Static",   description = "", detail = ""),
    PINK_NOISE( displayName = "Balanced Rain",   description = "", detail = ""),
    BROWN_NOISE(displayName = "Deep Rumble",     description = "", detail = ""),
    BLUE_NOISE( displayName = "High Hiss",       description = "", detail = ""),
    GREY_NOISE( displayName = "Neutral Static",  description = "", detail = "")
}
