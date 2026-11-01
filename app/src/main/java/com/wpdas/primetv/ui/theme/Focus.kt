package com.wpdas.primetv.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.ClickableSurfaceShape

/**
 * Foco padrão do D-pad: anel de 3 dp (violeta, ou claro sobre violeta) desenhado
 * 3 dp para fora do componente, mais escala de ~1,05.
 */
object PrimeFocus {
    @Composable
    fun border(shape: Shape, color: Color = PrimeTheme.colors.accent): ClickableSurfaceBorder =
        ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(PrimeDimens.focusRing, color),
                inset = PrimeDimens.focusRingOffset,
                shape = shape,
            ),
        )

    @Composable
    fun scale(focused: Float = PrimeDimens.FOCUS_SCALE): ClickableSurfaceScale =
        ClickableSurfaceDefaults.scale(focusedScale = focused, pressedScale = focused)

    @Composable
    fun noScale(): ClickableSurfaceScale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f)

    @Composable
    fun shape(shape: Shape): ClickableSurfaceShape = ClickableSurfaceDefaults.shape(shape = shape)
}
