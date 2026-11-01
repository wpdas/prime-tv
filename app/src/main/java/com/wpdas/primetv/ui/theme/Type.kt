package com.wpdas.primetv.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.wpdas.primetv.R

/** Inter 400/500/600/700 empacotada no APK (res/font). */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun inter(size: Int, weight: FontWeight = FontWeight.Normal, lineHeight: Float? = null, letterSpacing: Float = 0f) =
    TextStyle(
        fontFamily = InterFontFamily,
        fontSize = size.sp,
        fontWeight = weight,
        lineHeight = (lineHeight ?: (size * 1.25f)).sp,
        letterSpacing = letterSpacing.sp,
    )

/**
 * Tamanhos tirados direto dos protótipos (base 1280×720, fontScale fixo em 1,
 * então 1 sp = 1 dp da base).
 */
@Immutable
data class PrimeTypography(
    val heroTitle: TextStyle = inter(40, FontWeight.Bold, lineHeight = 42f, letterSpacing = -0.8f),
    val screenTitle: TextStyle = inter(34, FontWeight.Bold, lineHeight = 38f, letterSpacing = -0.6f),
    val dialogTitle: TextStyle = inter(26, FontWeight.Bold, lineHeight = 30f),
    val title: TextStyle = inter(22, FontWeight.Bold, lineHeight = 26f),
    val subtitle: TextStyle = inter(20, FontWeight.Bold, lineHeight = 24f),
    val logo: TextStyle = inter(20, FontWeight.Bold, lineHeight = 24f, letterSpacing = -0.3f),
    val sectionTitle: TextStyle = inter(18, FontWeight.SemiBold, lineHeight = 22f),
    val tab: TextStyle = inter(17, FontWeight.Medium, lineHeight = 20f),
    val tabSelected: TextStyle = inter(17, FontWeight.SemiBold, lineHeight = 20f),
    val button: TextStyle = inter(18, FontWeight.SemiBold, lineHeight = 22f),
    val buttonMedium: TextStyle = inter(17, FontWeight.SemiBold, lineHeight = 20f),
    val buttonSmall: TextStyle = inter(15, FontWeight.Medium, lineHeight = 18f),
    val bodyLarge: TextStyle = inter(16, lineHeight = 24f),
    val body: TextStyle = inter(15, lineHeight = 22.5f),
    val bodyStrong: TextStyle = inter(15, FontWeight.SemiBold, lineHeight = 18f),
    val listTitle: TextStyle = inter(16, FontWeight.SemiBold, lineHeight = 20f),
    val cardTitle: TextStyle = inter(14, FontWeight.SemiBold, lineHeight = 17f),
    val cardBody: TextStyle = inter(14, lineHeight = 20f),
    val caption: TextStyle = inter(13, lineHeight = 16f),
    val captionStrong: TextStyle = inter(13, FontWeight.SemiBold, lineHeight = 16f),
    val captionSmall: TextStyle = inter(12, lineHeight = 14f),
    val badge: TextStyle = inter(11, FontWeight.Bold, lineHeight = 13f),
    val clock: TextStyle = inter(16, lineHeight = 20f),
)

val PrimeTypographyTokens = PrimeTypography()
