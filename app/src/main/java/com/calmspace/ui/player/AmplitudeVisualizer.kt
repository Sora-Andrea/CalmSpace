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
import androidx.compose.ui.unit.dp

@Composable
fun AmplitudeVisualizer(
    levels: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        if (levels.isEmpty()) return@Canvas

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
}
