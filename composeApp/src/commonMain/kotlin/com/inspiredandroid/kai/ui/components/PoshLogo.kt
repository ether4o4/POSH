package com.inspiredandroid.kai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * POSH logo: a shell prompt (">_") in white on a red rounded square. Static —
 * replaces the animated two-circle mark inherited from the upstream app.
 */
@Composable
fun PoshLogo(
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val corner = this.size.minDimension * 0.22f
        drawRoundRect(
            color = Color(0xFFD32F2F),
            cornerRadius = CornerRadius(corner, corner),
        )
        val stroke = this.size.minDimension * 0.10f
        val chevron = Path().apply {
            moveTo(w * 0.24f, h * 0.32f)
            lineTo(w * 0.46f, h * 0.52f)
            lineTo(w * 0.24f, h * 0.72f)
        }
        drawPath(
            path = chevron,
            color = Color.White,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.55f, h * 0.72f),
            end = Offset(w * 0.78f, h * 0.72f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
