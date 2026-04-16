package com.calmspace.ui.screens.monitor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────
// Monitor Starfield
//
//   MonitorStarfield — full-screen twinkling star field
//   ShootingStarRing — comet that arcs over the top of the visualizer
//                      (9 o'clock → 3 o'clock or back), ~2×/minute
// ─────────────────────────────────────────────────────────────────────

private data class Star(
    val x: Float,       // 0..1 fraction of canvas width
    val y: Float,       // 0..1 fraction of canvas height
    val size: Float,    // dot radius in px
    val phase: Float,   // 0..1 phase offset within the twinkle cycle
    val group: Int      // 0, 1, or 2 — selects twinkle speed group
)

@Composable
fun MonitorStarfield(modifier: Modifier = Modifier) {
    val stars = remember {
        (0 until 70).map {
            Star(
                x     = Random.nextFloat(),
                y     = Random.nextFloat(),
                size  = Random.nextFloat() * 1.5f + 0.5f,
                phase = Random.nextFloat(),          // 0..1
                group = Random.nextInt(3)
            )
        }
    }

    // Three independent twinkle cycles so stars don't all pulse in sync.
    // Using separate animateFloat values keeps each group perfectly smooth.
    val transition = rememberInfiniteTransition(label = "starfield")

    val t0 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "t0"
    )
    val t1 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "t1"
    )
    val t2 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "t2"
    )

    Canvas(modifier = modifier) {
        val times = listOf(t0, t1, t2)
        stars.forEach { star ->
            val t     = times[star.group]
            val cycle = (t + star.phase) % 1f          // 0..1, wraps seamlessly
            val alpha = ((sin(cycle * 2f * PI.toFloat()) + 1f) / 2f)
                .coerceIn(0.08f, 0.78f)
            drawCircle(
                color  = Color.White.copy(alpha = alpha),
                radius = star.size,
                center = Offset(star.x * size.width, star.y * size.height)
            )
        }
    }
}

// 9 o'clock = π rad, 3 o'clock = 2π rad.
// Going from 9→3 sweeps through 12 o'clock (top of circle).
private val ANGLE_9  = PI.toFloat()
private val ANGLE_3  = (2f * PI).toFloat()

@Composable
fun ShootingStarRing(
    modifier: Modifier = Modifier,
    initialDelayMs: Long = Random.nextLong(6_000L, 12_000L)
) {
    val angle      = remember { Animatable(ANGLE_9) }
    var isVisible  by remember { mutableStateOf(false) }
    var tailDir    by remember { mutableFloatStateOf(1f) }  // +1 = 9→3, -1 = 3→9

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(initialDelayMs)

        while (isActive) {
            val goForward = Random.nextBoolean()
            tailDir = if (goForward) 1f else -1f
            val startAngle = if (goForward) ANGLE_9 else ANGLE_3
            val endAngle   = if (goForward) ANGLE_3 else ANGLE_9

            angle.snapTo(startAngle)
            isVisible = true
            angle.animateTo(endAngle, animationSpec = tween(1600, easing = LinearEasing))
            isVisible = false

            // Wait 12–18 s before next shot (~4 per minute on average).
            kotlinx.coroutines.delay(Random.nextLong(12_000L, 18_000L))
        }
    }

    Canvas(modifier = modifier) {
        if (!isVisible) return@Canvas

        val orbitRadius = size.minDimension / 2f - 5f
        val headAngle   = angle.value
        val dir         = tailDir              // read snapshot for this frame

        // Comet: 16 segments, 30° of tail behind the head.
        val segments   = 16
        val tailSpanRad = (30f / 360f) * 2f * PI.toFloat()

        for (i in 0..segments) {
            val frac     = i.toFloat() / segments     // 0 = tail tip, 1 = head
            val segAngle = headAngle - dir * (1f - frac) * tailSpanRad
            val alpha    = frac * 0.88f
            val dotR     = 0.8f + frac * 2.6f

            drawCircle(
                color  = Color.White.copy(alpha = alpha),
                radius = dotR,
                center = Offset(
                    x = center.x + cos(segAngle.toDouble()).toFloat() * orbitRadius,
                    y = center.y + sin(segAngle.toDouble()).toFloat() * orbitRadius
                )
            )
        }

        // Bright head
        drawCircle(
            color  = Color.White.copy(alpha = 0.95f),
            radius = 4f,
            center = Offset(
                x = center.x + cos(headAngle.toDouble()).toFloat() * orbitRadius,
                y = center.y + sin(headAngle.toDouble()).toFloat() * orbitRadius
            )
        )
    }
}
