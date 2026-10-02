package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AudioBlue
import com.example.ui.theme.AudioTeal
import com.example.ui.theme.RecordingRed

@Composable
fun LiveWaveformVisualizer(
    amplitudes: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val barCount = 32
        val spacing = 4.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(2.dp.toPx())

        val colors = listOf(RecordingRed, AudioTeal)

        val normalizedData = if (amplitudes.isEmpty() || !isRecording) {
            List(barCount) { 0.08f }
        } else {
            val list = amplitudes.takeLast(barCount)
            if (list.size < barCount) {
                List(barCount - list.size) { 0.06f } + list
            } else {
                list
            }
        }

        for (i in 0 until barCount) {
            val amp = normalizedData.getOrElse(i) { 0.05f }
            val barHeight = (height * amp * 0.95f).coerceIn(4.dp.toPx(), height)
            val left = i * (barWidth + spacing)
            val top = centerY - (barHeight / 2f)

            drawRoundRect(
                brush = Brush.verticalGradient(colors),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                alpha = if (isRecording) pulseAlpha else 0.4f
            )
        }
    }
}

@Composable
fun StaticWaveformVisualizer(
    sampleAmplitudesString: String,
    playedProgress: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier,
    barCount: Int = 28,
    activeColor: Color = AudioBlue,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val samples = remember(sampleAmplitudesString) {
        if (sampleAmplitudesString.isBlank()) {
            List(barCount) { 0.2f }
        } else {
            val parsed = sampleAmplitudesString.split(",").mapNotNull { it.trim().toFloatOrNull() }
            if (parsed.isEmpty()) {
                List(barCount) { 0.2f }
            } else {
                downsample(parsed, barCount)
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val spacing = 3.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(2.dp.toPx())

        val currentPlayedIndex = (playedProgress * barCount).toInt().coerceIn(0, barCount)

        for (i in 0 until barCount) {
            val amp = samples.getOrElse(i) { 0.2f }.coerceIn(0.1f, 1f)
            val barHeight = (height * amp * 0.9f).coerceIn(4.dp.toPx(), height)
            val left = i * (barWidth + spacing)
            val top = centerY - (barHeight / 2f)

            val color = if (i <= currentPlayedIndex && playedProgress > 0f) activeColor else inactiveColor

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

private fun downsample(data: List<Float>, targetCount: Int): List<Float> {
    if (data.size <= targetCount) {
        return data + List(targetCount - data.size) { 0.15f }
    }
    val step = data.size.toDouble() / targetCount.toDouble()
    return List(targetCount) { i ->
        val idx = (i * step).toInt().coerceIn(0, data.lastIndex)
        data[idx]
    }
}
