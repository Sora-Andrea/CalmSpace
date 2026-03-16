package com.calmspace.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────
// CalmSpace — Dark Water Theme
//
// Sleep-safe dark palette. Low luminance throughout — no whites, no
// bright accents. Everything is deep navy and muted blue-grey, safe
// to look at in a dark room without disrupting sleep.
//
// primary  = dim steel blue  (interactive elements, ambient ring)
// tertiary = dim teal        (masking ring — subtle distinction only)
// ─────────────────────────────────────────────────────────────────────

// ── Background & Surfaces ─────────────────────────────────────────────
val DeepWater        = Color(0xFF07090F)   // near-black — main background
val DeepWaterSurface = Color(0xFF0C1220)   // cards, dialogs
val DeepWaterVariant = Color(0xFF131C2E)   // input fields, chips, nav bar

// ── Moon Gold — ONLY for moon/Bedtime icons ───────────────────────────
val MoonGold             = Color(0xFFC8A855)   // pale warm moon gold — moon icons only

// ── Primary — Muted Steel Blue ────────────────────────────────────────
val SteelBlue            = Color(0xFF3A6A98)   // primary — dim readable blue
val SteelBlueOn          = Color(0xFF8AAFCC)
val SteelBlueContainer   = Color(0xFF0A2035)
val SteelBlueOnCont      = Color(0xFF8AAFCC)

// ── Secondary — Deep Slate ────────────────────────────────────────────
val SlateBlue            = Color(0xFF253A52)   // secondary — quieter blue-grey
val SlateBlueOn          = Color(0xFF7A9AB5)
val SlateBlueContainer   = Color(0xFF0F1E30)
val SlateBlueOnCont      = Color(0xFF7A9AB5)

// ── Tertiary — Dim Teal (masking ring) ────────────────────────────────
val DimTeal              = Color(0xFF2A6878)   // tertiary — deep teal, low glow
val DimTealOn            = Color(0xFF709EB0)
val DimTealContainer     = Color(0xFF0A2830)
val DimTealOnCont        = Color(0xFF709EB0)

// ── Text & Outlines ───────────────────────────────────────────────────
val WaterOnBackground    = Color(0xFFA0B8CC)   // primary text — dim blue-white
val WaterOnSurface       = Color(0xFFA0B8CC)
val WaterOnSurfaceVar    = Color(0xFF506878)   // hint / secondary text
val WaterOutline         = Color(0xFF182838)   // dividers, card borders

// ── Error ─────────────────────────────────────────────────────────────
val WaterError           = Color(0xFF7A3040)   // dim muted red — won't blast at night
val WaterOnError         = Color(0xFFCCAAB0)
