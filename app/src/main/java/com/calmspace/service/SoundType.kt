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
    WHITE_NOISE(
        displayName = "White Noise",
        description = "Classic, even hiss across all frequencies",
        detail = "Equal energy at every frequency. The most recognized masking sound — great for blocking a wide range of disruptions."
    ),
    PINK_NOISE(
        displayName = "Pink Noise",
        description = "Balanced, natural sound like steady rainfall",
        detail = "Reduces in energy as frequency rises (1/f). Feels warmer than white noise and mirrors natural sounds. Popular in sleep research."
    ),
    BROWN_NOISE(
        displayName = "Brown Noise",
        description = "Deep, rumbling bass like wind or a waterfall",
        detail = "Even more bass than pink noise (1/f²). Rich and low, similar to a strong breeze or distant thunder. Favored for deep focus and sleep."
    ),
    BLUE_NOISE(
        displayName = "Blue Noise",
        description = "Bright, airy hiss like ocean spray",
        detail = "More energy at higher frequencies. Crisp and sharp — useful for masking high-pitched disturbances like voices or electronics."
    ),
    GREY_NOISE(
        displayName = "Grey Noise",
        description = "Perceptually flat — tuned to how you hear",
        detail = "Adjusted to match the human ear's equal-loudness curve, so every frequency sounds equally loud to you. Subtle and non-fatiguing."
    )
}
