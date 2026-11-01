package com.wpdas.primetv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wpdas.primetv.ui.theme.PrimeTheme

/** Barra fina de progresso (cards, EPG, player). [progress] de 0 a 1. */
@Composable
fun ThinProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    trackColor: Color = Color.Transparent,
    color: Color = PrimeTheme.colors.accent,
    rounded: Boolean = false,
) {
    val shape = if (rounded) RoundedCornerShape(height / 2) else RoundedCornerShape(0.dp)
    Box(modifier.fillMaxWidth().height(height).background(trackColor, shape)) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(color, shape),
        )
    }
}
