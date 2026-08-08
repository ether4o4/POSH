package com.inspiredandroid.kai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val PoshRed = Color(0xFFE0191D)

/**
 * POSH logo: a white "P" whose bowl carries a red ">" chevron with a black
 * shadow edge, on a red rounded square — same mark as the launcher icon.
 */
@Composable
fun PoshLogo(
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val corner = this.size.minDimension * 0.18f
        drawRoundRect(
            color = PoshRed,
            cornerRadius = CornerRadius(corner, corner),
        )
        // P stem
        drawRect(
            color = Color.White,
            topLeft = Offset(w * 0.30f, h * 0.22f),
            size = androidx.compose.ui.geometry.Size(w * 0.11f, h * 0.56f),
        )
        // P bowl: flat top edge into a right-half arc back to the stem
        val bowl = Path().apply {
            moveTo(w * 0.41f, h * 0.22f)
            lineTo(w * 0.55f, h * 0.22f)
            arcTo(
                rect = Rect(
                    left = w * 0.55f - w * 0.18f,
                    top = h * 0.22f,
                    right = w * 0.55f + w * 0.18f,
                    bottom = h * 0.58f,
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false,
            )
            lineTo(w * 0.41f, h * 0.58f)
            close()
        }
        drawPath(bowl, Color.White)
        // Chevron shadow (black), offset down-right behind the red chevron
        val shadowOffset = w * 0.012f
        drawPath(chevronPath(w, h, shadowOffset), Color.Black)
        // Red chevron — the bowl's counter, pointing right
        drawPath(chevronPath(w, h, 0f), PoshRed)
    }
}

private fun chevronPath(w: Float, h: Float, offset: Float): Path = Path().apply {
    moveTo(w * 0.44f + offset, h * 0.30f + offset)
    lineTo(w * 0.62f + offset, h * 0.40f + offset)
    lineTo(w * 0.44f + offset, h * 0.50f + offset)
    lineTo(w * 0.44f + offset, h * 0.445f + offset)
    lineTo(w * 0.53f + offset, h * 0.40f + offset)
    lineTo(w * 0.44f + offset, h * 0.355f + offset)
    close()
}
