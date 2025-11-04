package com.rescuemate.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds a subtle shadow effect to any composable
 */
fun Modifier.cosmicShadow(
    elevation: Dp = 4.dp,
    color: Color = Color.Black.copy(alpha = 0.25f),
    spread: Dp = 0.dp
) = this.drawBehind {
    val shadowColor = color.toArgb()
    val transparent = color.copy(alpha = 0f).toArgb()

    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = transparent

        frameworkPaint.setShadowLayer(
            elevation.toPx(),
            0f,
            elevation.toPx() * 0.5f,
            shadowColor
        )

        canvas.drawRoundRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height,
            radiusX = 16.dp.toPx(),
            radiusY = 16.dp.toPx(),
            paint = paint
        )
    }
}

/**
 * Strong shadow for elevated cards
 */
fun Modifier.cosmicShadowStrong() = this.cosmicShadow(
    elevation = 8.dp,
    color = Color.Black.copy(alpha = 0.35f)
)

/**
 * Subtle shadow for minimal elevation
 */
fun Modifier.cosmicShadowSubtle() = this.cosmicShadow(
    elevation = 2.dp,
    color = Color.Black.copy(alpha = 0.15f)
)

