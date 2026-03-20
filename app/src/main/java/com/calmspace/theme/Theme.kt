package com.calmspace.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ─────────────────────────────────────────────────────────────────────
// CalmSpace Themes
//
// Six sleep-safe dark themes. All are low luminance — safe to view
// in a dark room without disrupting sleep.
// dynamicColor disabled — branded palettes always apply.
// Moon Gold (#C8A855) is applied directly to moon icons — not part
// of any scheme.
// ─────────────────────────────────────────────────────────────────────

enum class AppTheme {
    DEEP_WATER,
    OCEAN,
    FOREST,
    SUNSET,
    SUNRISE
}

// ── Deep Water ────────────────────────────────────────────────────────
private val DeepWaterScheme = darkColorScheme(
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

// ── Ocean ─────────────────────────────────────────────────────────────
private val OceanScheme = darkColorScheme(
    primary              = OceanBlue,
    onPrimary            = OceanBlueOn,
    primaryContainer     = OceanBlueContainer,
    onPrimaryContainer   = OceanBlueOnCont,
    secondary            = OceanMid,
    onSecondary          = OceanMidOn,
    secondaryContainer   = OceanMidContainer,
    onSecondaryContainer = OceanMidOnCont,
    tertiary             = BiolumiTeal,
    onTertiary           = BiolumiTealOn,
    tertiaryContainer    = BiolumiTealContainer,
    onTertiaryContainer  = BiolumiTealOnCont,
    background           = OceanBg,
    onBackground         = OceanOnBg,
    surface              = OceanSurface,
    onSurface            = OceanOnSurface,
    surfaceVariant       = OceanVariant,
    onSurfaceVariant     = OceanOnSurfaceVar,
    outline              = OceanOutline,
    error                = OceanError,
    onError              = OceanOnError,
)

// ── Forest ────────────────────────────────────────────────────────────
private val ForestScheme = darkColorScheme(
    primary              = MossGreen,
    onPrimary            = MossGreenOn,
    primaryContainer     = MossGreenContainer,
    onPrimaryContainer   = MossGreenOnCont,
    secondary            = DeepMoss,
    onSecondary          = DeepMossOn,
    secondaryContainer   = DeepMossContainer,
    onSecondaryContainer = DeepMossOnCont,
    tertiary             = EarthOlive,
    onTertiary           = EarthOliveOn,
    tertiaryContainer    = EarthOliveContainer,
    onTertiaryContainer  = EarthOliveOnCont,
    background           = ForestBg,
    onBackground         = ForestOnBg,
    surface              = ForestSurface,
    onSurface            = ForestOnSurface,
    surfaceVariant       = ForestVariant,
    onSurfaceVariant     = ForestOnSurfaceVar,
    outline              = ForestOutline,
    error                = ForestError,
    onError              = ForestOnError,
)

// ── Sunset ────────────────────────────────────────────────────────────
private val SunsetScheme = darkColorScheme(
    primary              = MutedAmber,
    onPrimary            = MutedAmberOn,
    primaryContainer     = MutedAmberContainer,
    onPrimaryContainer   = MutedAmberOnCont,
    secondary            = DimRust,
    onSecondary          = DimRustOn,
    secondaryContainer   = DimRustContainer,
    onSecondaryContainer = DimRustOnCont,
    tertiary             = DimWine,
    onTertiary           = DimWineOn,
    tertiaryContainer    = DimWineContainer,
    onTertiaryContainer  = DimWineOnCont,
    background           = SunsetBg,
    onBackground         = SunsetOnBg,
    surface              = SunsetSurface,
    onSurface            = SunsetOnSurface,
    surfaceVariant       = SunsetVariant,
    onSurfaceVariant     = SunsetOnSurfaceVar,
    outline              = SunsetOutline,
    error                = SunsetError,
    onError              = SunsetOnError,
)

// ── Sunrise ───────────────────────────────────────────────────────────
private val SunriseScheme = darkColorScheme(
    primary              = MutedMauve,
    onPrimary            = MutedMauveOn,
    primaryContainer     = MutedMauveContainer,
    onPrimaryContainer   = MutedMauveOnCont,
    secondary            = DeepLavender,
    onSecondary          = DeepLavenderOn,
    secondaryContainer   = DeepLavenderContainer,
    onSecondaryContainer = DeepLavenderOnCont,
    tertiary             = DustyRose,
    onTertiary           = DustyRoseOn,
    tertiaryContainer    = DustyRoseContainer,
    onTertiaryContainer  = DustyRoseOnCont,
    background           = SunriseBg,
    onBackground         = SunriseOnBg,
    surface              = SunriseSurface,
    onSurface            = SunriseOnSurface,
    surfaceVariant       = SunriseVariant,
    onSurfaceVariant     = SunriseOnSurfaceVar,
    outline              = SunriseOutline,
    error                = SunriseError,
    onError              = SunriseOnError,
)


@Composable
fun CalmSpaceTheme(
    theme: AppTheme = AppTheme.DEEP_WATER,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        AppTheme.DEEP_WATER -> DeepWaterScheme
        AppTheme.OCEAN      -> OceanScheme
        AppTheme.FOREST     -> ForestScheme
        AppTheme.SUNSET     -> SunsetScheme
        AppTheme.SUNRISE    -> SunriseScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
