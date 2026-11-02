package com.wpdas.primetv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme

/**
 * Linha de canal da tela Ao vivo: número, logo, nome, programa atual com horário
 * e barra de progresso do EPG. Segurar OK favorita ([onLongClick]).
 */
@Composable
fun ChannelRow(
    number: String,
    name: String,
    logoUrl: String?,
    nowTitle: String?,
    timeRange: String?,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onFocused: (() -> Unit)? = null,
    qualityLabel: String? = null,
) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.card
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.fillMaxWidth().bringIntoViewOnFocus(12.dp).onFocusChanged { if (it.isFocused) onFocused?.invoke() },
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.surface,
            contentColor = colors.text,
            focusedContainerColor = colors.selected,
            focusedContentColor = colors.text,
            pressedContainerColor = colors.selected,
            pressedContentColor = colors.text,
        ),
        scale = PrimeFocus.noScale(),
        border = PrimeFocus.border(shape),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(number, style = PrimeTheme.typography.cardBody, color = colors.textSecondary, textAlign = TextAlign.End, modifier = Modifier.width(34.dp))
            Box(Modifier.size(56.dp, 36.dp).background(colors.surface2, PrimeTheme.shapes.small), contentAlignment = Alignment.Center) {
                if (logoUrl != null) {
                    Artwork(logoUrl, colors.surface2, Modifier.size(56.dp, 36.dp), name)
                } else {
                    Text(name.take(3).uppercase(), style = PrimeTheme.typography.badge, color = colors.textBody)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f, fill = false), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(name, style = PrimeTheme.typography.bodyStrong, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (qualityLabel != null) {
                            Text(qualityLabel, style = PrimeTheme.typography.badge, color = colors.textBody, modifier = Modifier.background(colors.surface2, PrimeTheme.shapes.small).padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                    if (timeRange != null) Text(timeRange, style = PrimeTheme.typography.caption, color = colors.textSecondary, maxLines = 1, modifier = Modifier.padding(start = 12.dp))
                }
                if (nowTitle != null) {
                    Text(nowTitle, style = PrimeTheme.typography.cardBody, color = colors.textBody, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    ThinProgressBar(progress, height = 3.dp, trackColor = colors.buttonSecondary, rounded = true)
                }
            }
        }
    }
}
