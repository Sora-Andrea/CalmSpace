package com.calmspace.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.calmspace.R

// ─────────────────────────────────────────────────────────────────────
// CalmSpace Typography
//
// Montserrat — headings, titles, labels, buttons (geometric sans-serif)
// Lora       — body text, descriptions (elegant serif)
//
// Font files go in app/src/main/res/font/ with these exact names:
//   montserrat_regular.ttf   montserrat_medium.ttf
//   montserrat_semibold.ttf  montserrat_bold.ttf
//   lora_regular.ttf         lora_medium.ttf
//   lora_bold.ttf
//
// Download from https://fonts.google.com/specimen/Montserrat
//           and https://fonts.google.com/specimen/Lora
// ─────────────────────────────────────────────────────────────────────

val Montserrat = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold, FontWeight.Bold),
)

val Lora = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_medium, FontWeight.Medium),
    Font(R.font.lora_bold, FontWeight.Bold),
)

val Typography = Typography(
    // ── Display ──
    displayLarge  = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.Bold,    fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.Bold,    fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
    displaySmall  = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.SemiBold,fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),

    // ── Headline ──
    headlineLarge  = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineSmall  = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.Medium,   fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),

    // ── Title ──
    titleLarge  = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.Medium,   fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall  = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),

    // ── Body (Lora) ──
    bodyLarge  = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall  = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),

    // ── Label (Montserrat) ──
    labelLarge  = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)
