package com.wpdas.primetv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import com.wpdas.primetv.core.i18n.I18n
import com.wpdas.primetv.core.i18n.LocalI18n

val LocalPrimeColors = staticCompositionLocalOf { PrimeColorTokens }
val LocalPrimeTypography = staticCompositionLocalOf { PrimeTypographyTokens }
val LocalPrimeShapes = staticCompositionLocalOf { PrimeShapeTokens }

object PrimeTheme {
    val colors: PrimeColors @Composable get() = LocalPrimeColors.current
    val typography: PrimeTypography @Composable get() = LocalPrimeTypography.current
    val shapes: PrimeShapes @Composable get() = LocalPrimeShapes.current
}

/**
 * Tema do app.
 *
 * Além dos tokens, instala uma [Density] customizada: TV Boxes comuns reportam dpi
 * inconsistentes, então a largura da janela em px é mapeada para [PrimeDimens.BASE_WIDTH]
 * unidades (`density = larguraPx / 1280`, `fontScale = 1`). Assim as medidas dos
 * protótipos podem ser usadas direto em dp e a UI fica igual em qualquer aparelho.
 */
@Composable
fun PrimeTheme(
    i18n: I18n,
    scaleToDesign: Boolean = true,
    content: @Composable () -> Unit,
) {
    val windowInfo = LocalWindowInfo.current
    val systemDensity = LocalDensity.current
    val density = remember(windowInfo.containerSize.width, systemDensity, scaleToDesign) {
        val widthPx = windowInfo.containerSize.width
        if (!scaleToDesign || widthPx <= 0) systemDensity
        else Density(density = widthPx / PrimeDimens.BASE_WIDTH, fontScale = 1f)
    }
    val colors = PrimeColorTokens
    val typography = PrimeTypographyTokens
    val shapes = PrimeShapeTokens

    CompositionLocalProvider(
        LocalDensity provides density,
        LocalI18n provides i18n,
        LocalPrimeColors provides colors,
        LocalPrimeTypography provides typography,
        LocalPrimeShapes provides shapes,
    ) {
        MaterialTheme(
            colorScheme = tvColorScheme(colors),
            typography = tvTypography(typography),
            content = content,
        )
    }
}

/** Mapeia os tokens para o esquema do tv-material, para componentes da lib terem cores coerentes. */
private fun tvColorScheme(c: PrimeColors): ColorScheme = darkColorScheme(
    primary = c.accent,
    onPrimary = c.onAccent,
    primaryContainer = c.accentSoftBackground,
    onPrimaryContainer = c.accentSoftText,
    secondary = c.buttonSecondary,
    onSecondary = c.text,
    background = c.background,
    onBackground = c.text,
    surface = c.surface,
    onSurface = c.text,
    surfaceVariant = c.surface2,
    onSurfaceVariant = c.textSecondary,
    inverseSurface = c.text,
    inverseOnSurface = c.background,
    border = c.divider,
    error = c.live,
)

private fun tvTypography(t: PrimeTypography): Typography = Typography(
    displayLarge = t.heroTitle,
    displayMedium = t.screenTitle,
    displaySmall = t.dialogTitle,
    headlineLarge = t.title,
    headlineMedium = t.subtitle,
    headlineSmall = t.sectionTitle,
    titleLarge = t.subtitle,
    titleMedium = t.listTitle,
    titleSmall = t.cardTitle,
    bodyLarge = t.bodyLarge,
    bodyMedium = t.body,
    bodySmall = t.caption,
    labelLarge = t.button,
    labelMedium = t.buttonSmall,
    labelSmall = t.captionSmall,
)
