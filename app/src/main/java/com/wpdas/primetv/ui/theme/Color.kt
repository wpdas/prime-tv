package com.wpdas.primetv.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Tokens de cor do design (design/README.md). Nunca use cor literal fora daqui. */
@Immutable
data class PrimeColors(
    val background: Color = Color(0xFF0B0B0F),
    val surface: Color = Color(0xFF16161C),
    val surface2: Color = Color(0xFF22222A),
    val surfaceMenu: Color = Color(0xFF1A1A21),
    val buttonSecondary: Color = Color(0xFF2A2A33),
    val text: Color = Color(0xFFF2F2F2),
    val textBody: Color = Color(0xFFD4D4DC),
    val textSecondary: Color = Color(0xFFA1A1AD),
    val accent: Color = Color(0xFF8B5CF6),
    val accentPressed: Color = Color(0xFF7340E8),
    val accentSoftText: Color = Color(0xFFDDD6FE),
    val accentSoftBackground: Color = Color(0xFF2E2350),
    val selected: Color = Color(0xFF1E1A2E),
    val live: Color = Color(0xFFC2263B),
    val divider: Color = Color(0xFF3A3A46),
    val dotInactive: Color = Color(0xFF55555F),
    val onAccent: Color = Color(0xFFFFFFFF),
    val scrim: Color = Color(0xC7050508),
)

val PrimeColorTokens = PrimeColors()
