package com.wpdas.primetv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme

/** Selo estático (ex.: "Em alta esta semana", "Detectado: Xtream Codes", tipo na busca). */
@Composable
fun TagChip(text: String, modifier: Modifier = Modifier, accent: Boolean = true) {
    val colors = PrimeTheme.colors
    Text(
        text = text,
        style = PrimeTheme.typography.captionStrong,
        color = if (accent) colors.accentSoftText else colors.textBody,
        modifier = modifier
            .background(if (accent) colors.accentSoftBackground else colors.surface2, PrimeTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Selo "AO VIVO". */
@Composable
fun LiveBadge(text: String, modifier: Modifier = Modifier) {
    val colors = PrimeTheme.colors
    Text(
        text = text,
        style = PrimeTheme.typography.badge,
        color = colors.onAccent,
        modifier = modifier
            .background(colors.live, PrimeTheme.shapes.small.copy(all = androidx.compose.foundation.shape.CornerSize(4.dp)))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

/** Chip clicável em formato de pílula (temporadas, palavras-chave, filtros). */
@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.pill
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) colors.text else colors.surface2,
            contentColor = if (selected) colors.background else colors.textBody,
            focusedContainerColor = if (selected) colors.text else colors.selected,
            focusedContentColor = if (selected) colors.background else colors.text,
            pressedContainerColor = colors.text,
            pressedContentColor = colors.background,
        ),
        scale = PrimeFocus.scale(),
        border = PrimeFocus.border(shape),
    ) {
        Box(Modifier.fillMaxHeight().padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = if (selected) PrimeTheme.typography.bodyStrong else PrimeTheme.typography.buttonSmall,
                color = Color.Unspecified,
                maxLines = 1,
            )
        }
    }
}
