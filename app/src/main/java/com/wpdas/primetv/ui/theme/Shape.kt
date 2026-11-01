package com.wpdas.primetv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

@Immutable
data class PrimeShapes(
    val small: RoundedCornerShape = RoundedCornerShape(6.dp),
    val card: RoundedCornerShape = RoundedCornerShape(8.dp),
    val panel: RoundedCornerShape = RoundedCornerShape(12.dp),
    val panelLarge: RoundedCornerShape = RoundedCornerShape(16.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(50),
)

val PrimeShapeTokens = PrimeShapes()
