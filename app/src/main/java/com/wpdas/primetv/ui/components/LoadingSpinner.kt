package com.wpdas.primetv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wpdas.primetv.ui.theme.PrimeTheme

/** Anel girando: trilha cinza com um arco violeta (como nos protótipos). */
@Composable
fun LoadingSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    strokeWidth: Dp = 3.dp,
    color: Color = PrimeTheme.colors.accent,
    trackColor: Color = PrimeTheme.colors.divider,
) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    Canvas(modifier.size(size)) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val inset = strokeWidth.toPx() / 2
        val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
        drawArc(trackColor, 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
        drawArc(color, angle, 90f, false, Offset(inset, inset), arcSize, style = stroke)
    }
}
