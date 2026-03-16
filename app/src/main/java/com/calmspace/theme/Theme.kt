package com.calmspace.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ─────────────────────────────────────────────────────────────────────
// CalmSpace Theme — Dark Water + Moon Gold
//
// Near-black navy background, moon gold accents throughout,
// dim teal masking ring. Sleep-safe: low luminance, no harsh whites.
// dynamicColor disabled — branded palette always applies.
// ─────────────────────────────────────────────────────────────────────

private val DarkWaterScheme = darkColorScheme(
    primary              = SteelBlue,
    onPrimary            = SteelBlueOn,
    primaryContainer     = SteelBlueContainer,
    onPrimaryContainer   = SteelBlueOnCont,

    secondary            = SlateBlue,
    onSecondary          = SlateBlueOn,
    secondaryContainer   = SlateBlueContainer,
    onSecondaryContainer = SlateBlueOnCont,

    tertiary             = DimTeal,
    onTertiary           = DimTealOn,
    tertiaryContainer    = DimTealContainer,
    onTertiaryContainer  = DimTealOnCont,

    background           = DeepWater,
    onBackground         = WaterOnBackground,

    surface              = DeepWaterSurface,
    onSurface            = WaterOnSurface,
    surfaceVariant       = DeepWaterVariant,
    onSurfaceVariant     = WaterOnSurfaceVar,

    outline              = WaterOutline,

    error                = WaterError,
    onError              = WaterOnError,
)

@Composable
fun CalmSpaceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkWaterScheme,
        typography = Typography,
        content = content
    )
}
