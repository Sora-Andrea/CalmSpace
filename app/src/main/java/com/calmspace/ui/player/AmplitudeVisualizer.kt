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
import kotlin.math.min
import kotlin.math.sin

enum class AmplitudeVisualizerMode {
    HORIZONTAL,
    CIRCULAR
}

@Composable
fun AmplitudeVisualizer(
    levels: List<Float>,
    modifier: Modifier = Modifier,
    mode: AmplitudeVisualizerMode = AmplitudeVisualizerMode.HORIZONTAL,
    barColor: Color = MaterialTheme.colorScheme.primary,
    secondaryLevels: List<Float>? = null,
    secondaryBarColor: Color = MaterialTheme.colorScheme.tertiary,
    secondaryAngleOffsetDeg: Float = 0f,
    variant: VisualizerVariant = VisualizerVariantConfig.activeVariant
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
                    val start = Offset(
                        centerX + ringRadius * cos(radians).toFloat(),
                        centerY + ringRadius * sin(radians).toFloat()
                    )
                    val end = Offset(
                        centerX + (ringRadius + barLength) * cos(radians).toFloat(),
                        centerY + (ringRadius + barLength) * sin(radians).toFloat()
                    )
                    drawLine(
                        color = barColor.copy(alpha = (0.2f + 0.8f * level).coerceIn(0f, 1f)),
                        start = start,
                        end = end,
                        strokeWidth = barThickness,
                        cap = StrokeCap.Round
                    )
                }

                // Secondary ring uses same radial anchor with a slight angular offset.
                // This keeps source rings visually separated and reduces overlap clutter.
                secondaryLevels?.let { secLevels ->
                    if (secLevels.isEmpty()) return@let
                    val secAngleStep = 360f / secLevels.size
                    val innerRadiusFloor = min(size.width, size.height) * 0.16f
                    val inwardTravelLimit = (ringRadius - innerRadiusFloor).coerceAtLeast(barThickness)
                    secLevels.forEachIndexed { index, rawLevel ->
                        val level = rawLevel.coerceIn(0f, 1f)
                        val barLength = minBarLength + (maxBarLength - minBarLength) * level
                        val angle = when (variant) {
                            VisualizerVariant.DEFAULT ->
                                startAngle + secondaryAngleOffsetDeg + secAngleStep * index
                            VisualizerVariant.ALTERNATIVE ->
                                startAngle + secAngleStep * index
                        }
                        val radians = Math.toRadians(angle.toDouble())
                        val start = Offset(
                            centerX + ringRadius * cos(radians).toFloat(),
                            centerY + ringRadius * sin(radians).toFloat()
                        )
                        val end = when (variant) {
                            VisualizerVariant.DEFAULT -> Offset(
                                centerX + (ringRadius + barLength) * cos(radians).toFloat(),
                                centerY + (ringRadius + barLength) * sin(radians).toFloat()
                            )
                            VisualizerVariant.ALTERNATIVE -> {
                                // Keep inward style, but avoid hard-clamping too early.
                                // Early clamp makes bars appear frozen at high levels.
                                val inwardLength = (barLength * 0.92f).coerceAtMost(inwardTravelLimit)
                                val innerRadius = ringRadius - inwardLength
                                Offset(
                                    centerX + innerRadius * cos(radians).toFloat(),
                                    centerY + innerRadius * sin(radians).toFloat()
                                )
                            }
                        }
                        drawLine(
                            color = secondaryBarColor.copy(alpha = (0.2f + 0.8f * level).coerceIn(0f, 1f)),
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
}
