package com.wpdas.primetv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme

enum class PrimeButtonStyle { Primary, Secondary, Outline, Ghost }

/**
 * Botão focável pelo D-pad. Primário: violeta com texto branco e anel de foco claro.
 * Secundário: `#2A2A33`. Outline: borda fina (ex.: "Ver trailer"). Ghost: sem fundo (ícones da barra).
 */
@Composable
fun PrimeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PrimeButtonStyle = PrimeButtonStyle.Primary,
    icon: ImageVector? = null,
    height: Dp = 48.dp,
    horizontalPadding: Dp = if (style == PrimeButtonStyle.Primary) 26.dp else 22.dp,
    textStyle: TextStyle = when (style) {
        PrimeButtonStyle.Primary -> PrimeTheme.typography.button
        PrimeButtonStyle.Secondary -> PrimeTheme.typography.buttonMedium
        else -> PrimeTheme.typography.buttonSmall
    },
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.card
    val (container, content, focusRing) = when (style) {
        PrimeButtonStyle.Primary -> Triple(colors.accent, colors.onAccent, colors.text)
        PrimeButtonStyle.Secondary -> Triple(colors.buttonSecondary, colors.text, colors.accent)
        PrimeButtonStyle.Outline -> Triple(Color.Transparent, colors.textBody, colors.accent)
        PrimeButtonStyle.Ghost -> Triple(Color.Transparent, colors.textSecondary, colors.accent)
    }
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = content,
            focusedContainerColor = if (style == PrimeButtonStyle.Primary) colors.accentPressed else container,
            focusedContentColor = if (style == PrimeButtonStyle.Ghost) colors.text else content,
            pressedContainerColor = if (style == PrimeButtonStyle.Primary) colors.accentPressed else colors.surface2,
            pressedContentColor = content,
            disabledContainerColor = container.copy(alpha = 0.5f),
            disabledContentColor = content.copy(alpha = 0.6f),
        ),
        scale = PrimeFocus.scale(),
        border = if (style == PrimeButtonStyle.Outline) {
            ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(1.dp, colors.divider), shape = shape),
                focusedBorder = Border(BorderStroke(3.dp, focusRing), inset = 3.dp, shape = shape),
            )
        } else PrimeFocus.border(shape, focusRing),
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().padding(PaddingValues(horizontal = horizontalPadding)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = text, style = textStyle, maxLines = 1)
        }
    }
}

/** Botão quadrado só com ícone (ex.: "+ Minha lista", Buscar, Configurações). */
@Composable
fun PrimeIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PrimeButtonStyle = PrimeButtonStyle.Secondary,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    enabled: Boolean = true,
) {
    val colors = PrimeTheme.colors
    val shape = if (style == PrimeButtonStyle.Ghost) PrimeTheme.shapes.pill else PrimeTheme.shapes.card
    val (container, content) = when (style) {
        PrimeButtonStyle.Primary -> colors.accent to colors.onAccent
        PrimeButtonStyle.Secondary -> colors.buttonSecondary to colors.text
        else -> Color.Transparent to colors.textSecondary
    }
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = content,
            focusedContainerColor = if (style == PrimeButtonStyle.Ghost) colors.surface2 else container,
            focusedContentColor = colors.text,
            pressedContainerColor = colors.surface2,
            pressedContentColor = colors.text,
        ),
        scale = PrimeFocus.scale(),
        border = PrimeFocus.border(shape, if (style == PrimeButtonStyle.Primary) colors.text else colors.accent),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(iconSize))
        }
    }
}
