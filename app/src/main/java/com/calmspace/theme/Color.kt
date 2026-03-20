package com.calmspace.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────
// CalmSpace — Sleep-Safe Palette Library
//
// All 6 themes share these rules:
//   • No whites, no bright accents — safe to view in a dark room
//   • Low luminance throughout — backgrounds near-black, text dim
//   • Moon Gold is always applied directly to moon icons — never in scheme
// ─────────────────────────────────────────────────────────────────────

// ── Moon Gold — ONLY for moon/Bedtime icons (every theme) ─────────────
val MoonGold = Color(0xFFC8A855)   // pale warm gold — moon icons only

// ─────────────────────────────────────────────────────────────────────
// DEEP WATER  (near-black navy + muted steel blue)
// ─────────────────────────────────────────────────────────────────────
val DeepWater            = Color(0xFF07090F)
val DeepWaterSurface     = Color(0xFF0C1220)
val DeepWaterVariant     = Color(0xFF131C2E)

val SteelBlue            = Color(0xFF3A6A98)
val SteelBlueOn          = Color(0xFF8AAFCC)
val SteelBlueContainer   = Color(0xFF0A2035)
val SteelBlueOnCont      = Color(0xFF8AAFCC)

val SlateBlue            = Color(0xFF253A52)
val SlateBlueOn          = Color(0xFF7A9AB5)
val SlateBlueContainer   = Color(0xFF0F1E30)
val SlateBlueOnCont      = Color(0xFF7A9AB5)

val DimTeal              = Color(0xFF2A6878)
val DimTealOn            = Color(0xFF709EB0)
val DimTealContainer     = Color(0xFF0A2830)
val DimTealOnCont        = Color(0xFF709EB0)

val WaterOnBackground    = Color(0xFFA0B8CC)
val WaterOnSurface       = Color(0xFFA0B8CC)
val WaterOnSurfaceVar    = Color(0xFF506878)
val WaterOutline         = Color(0xFF182838)
val WaterError           = Color(0xFF7A3040)
val WaterOnError         = Color(0xFFCCAAB0)

// ─────────────────────────────────────────────────────────────────────
// OCEAN  (deeper sea blue-black + bioluminescent teal)
// ─────────────────────────────────────────────────────────────────────
val OceanBg              = Color(0xFF050C14)
val OceanSurface         = Color(0xFF091525)
val OceanVariant         = Color(0xFF0E1E30)

val OceanBlue            = Color(0xFF256B8C)
val OceanBlueOn          = Color(0xFF7ABFD8)
val OceanBlueContainer   = Color(0xFF082232)
val OceanBlueOnCont      = Color(0xFF7ABFD8)

val OceanMid             = Color(0xFF1A4A5A)
val OceanMidOn           = Color(0xFF6AA8B8)
val OceanMidContainer    = Color(0xFF081820)
val OceanMidOnCont       = Color(0xFF6AA8B8)

val BiolumiTeal          = Color(0xFF1E7870)
val BiolumiTealOn        = Color(0xFF68B0A8)
val BiolumiTealContainer = Color(0xFF082825)
val BiolumiTealOnCont    = Color(0xFF68B0A8)

val OceanOnBg            = Color(0xFF88B8C8)
val OceanOnSurface       = Color(0xFF88B8C8)
val OceanOnSurfaceVar    = Color(0xFF446878)
val OceanOutline         = Color(0xFF102030)
val OceanError           = Color(0xFF703040)
val OceanOnError         = Color(0xFFC8A0A8)

// ─────────────────────────────────────────────────────────────────────
// FOREST  (near-black pine + deep mossy green)
// ─────────────────────────────────────────────────────────────────────
val ForestBg             = Color(0xFF070D08)
val ForestSurface        = Color(0xFF0A1510)
val ForestVariant        = Color(0xFF10201A)

val MossGreen            = Color(0xFF2E6048)
val MossGreenOn          = Color(0xFF78B090)
val MossGreenContainer   = Color(0xFF081E15)
val MossGreenOnCont      = Color(0xFF78B090)

val DeepMoss             = Color(0xFF204030)
val DeepMossOn           = Color(0xFF689880)
val DeepMossContainer    = Color(0xFF0A1810)
val DeepMossOnCont       = Color(0xFF689880)

val EarthOlive           = Color(0xFF486020)
val EarthOliveOn         = Color(0xFF98A878)
val EarthOliveContainer  = Color(0xFF182008)
val EarthOliveOnCont     = Color(0xFF98A878)

val ForestOnBg           = Color(0xFF90B898)
val ForestOnSurface      = Color(0xFF90B898)
val ForestOnSurfaceVar   = Color(0xFF486858)
val ForestOutline        = Color(0xFF182818)
val ForestError          = Color(0xFF6A3028)
val ForestOnError        = Color(0xFFC0A098)

// ─────────────────────────────────────────────────────────────────────
// SUNSET  (near-black warm + muted amber/rust)
// ─────────────────────────────────────────────────────────────────────
val SunsetBg             = Color(0xFF0D0806)
val SunsetSurface        = Color(0xFF1A1008)
val SunsetVariant        = Color(0xFF261810)

val MutedAmber           = Color(0xFF7A4820)
val MutedAmberOn         = Color(0xFFCC9870)
val MutedAmberContainer  = Color(0xFF281408)
val MutedAmberOnCont     = Color(0xFFCC9870)

val DimRust              = Color(0xFF5A3028)
val DimRustOn            = Color(0xFFB08078)
val DimRustContainer     = Color(0xFF1E100C)
val DimRustOnCont        = Color(0xFFB08078)

val DimWine              = Color(0xFF603048)
val DimWineOn            = Color(0xFFB08898)
val DimWineContainer     = Color(0xFF200C18)
val DimWineOnCont        = Color(0xFFB08898)

val SunsetOnBg           = Color(0xFFC0A888)
val SunsetOnSurface      = Color(0xFFC0A888)
val SunsetOnSurfaceVar   = Color(0xFF785848)
val SunsetOutline        = Color(0xFF281808)
val SunsetError          = Color(0xFF7A3020)
val SunsetOnError        = Color(0xFFCC9880)

// ─────────────────────────────────────────────────────────────────────
// SUNRISE  (near-black with purple-rose undertone + muted mauve)
// ─────────────────────────────────────────────────────────────────────
val SunriseBg            = Color(0xFF0C0A0C)
val SunriseSurface       = Color(0xFF18101A)
val SunriseVariant       = Color(0xFF231824)

val MutedMauve           = Color(0xFF6A3A58)
val MutedMauveOn         = Color(0xFFB888A0)
val MutedMauveContainer  = Color(0xFF221020)
val MutedMauveOnCont     = Color(0xFFB888A0)

val DeepLavender         = Color(0xFF4A3060)
val DeepLavenderOn       = Color(0xFF9878B0)
val DeepLavenderContainer = Color(0xFF180E20)
val DeepLavenderOnCont   = Color(0xFF9878B0)

val DustyRose            = Color(0xFF684038)
val DustyRoseOn          = Color(0xFFB09088)
val DustyRoseContainer   = Color(0xFF221410)
val DustyRoseOnCont      = Color(0xFFB09088)

val SunriseOnBg          = Color(0xFFB8A0B8)
val SunriseOnSurface     = Color(0xFFB8A0B8)
val SunriseOnSurfaceVar  = Color(0xFF686078)
val SunriseOutline       = Color(0xFF201830)
val SunriseError         = Color(0xFF7A3040)
val SunriseOnError       = Color(0xFFCCAAB0)
