package com.calmspace.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min

enum class AmplitudeVisualizerMode {
    HORIZONTAL,
    CIRCULAR
}

@Composable
fun AmplitudeVisualizer(
    levels: List<Float>,
    modifier: Modifier = Modifier,
    mode: AmplitudeVisualizerMode = AmplitudeVisualizerMode.HORIZONTAL,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val resolvedModifier = if (mode == AmplitudeVisualizerMode.CIRCULAR) {
        modifier
    } else {
        modifier
            .fillMaxWidth()
            .height(120.dp)
    }

    Canvas(
        modifier = resolvedModifier
    ) {
        if (levels.isEmpty()) return@Canvas

        when (mode) {
            AmplitudeVisualizerMode.HORIZONTAL -> {
                val spacing = 6.dp.toPx()
                val totalSpacing = spacing * (levels.size - 1)
                val barWidth = ((size.width - totalSpacing) / levels.size).coerceAtLeast(2f)

                levels.forEachIndexed { index, rawLevel ->
                    val level = rawLevel.coerceIn(0f, 1f)
                    val barHeight = (size.height * (0.15f + 0.85f * level)).coerceAtMost(size.height)
                    val left = index * (barWidth + spacing)
                    val top = size.height - barHeight

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
            AmplitudeVisualizerMode.CIRCULAR -> {
                val barCount = levels.size
                if (barCount == 0) return@Canvas

                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val ringRadius = min(size.width, size.height) * 0.28f
                val barThickness = (min(size.width, size.height) * 0.02f).coerceAtLeast(2f)
                val minBarLength = min(size.width, size.height) * 0.05f
                val maxBarLength = min(size.width, size.height) * 0.18f
                val angleStep = 360f / barCount
                val startAngle = -90f // 12 o'clock

                levels.forEachIndexed { index, rawLevel ->
                    val level = rawLevel.coerceIn(0f, 1f)
                    val barLength = minBarLength + (maxBarLength - minBarLength) * level
                    val angle = startAngle + angleStep * index
                    val radians = Math.toRadians(angle.toDouble())
                    val startRadius = ringRadius
                    val endRadius = ringRadius + barLength
                    val start = Offset(
                        centerX + startRadius * cos(radians).toFloat(),
                        centerY + startRadius * sin(radians).toFloat()
                    )
                    val end = Offset(
                        centerX + endRadius * cos(radians).toFloat(),
                        centerY + endRadius * sin(radians).toFloat()
                    )
                    drawLine(
                        color = barColor,
                        start = start,
                        end = end,
                        strokeWidth = barThickness,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
