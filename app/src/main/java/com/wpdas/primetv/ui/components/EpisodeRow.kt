package com.wpdas.primetv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wpdas.primetv.domain.model.Episode
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme

/** Linha de episódio: miniatura 160×90 com progresso, número e título, duração, sinopse e estado. */
@Composable
fun EpisodeRow(
    episode: Episode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    stateLabel: String? = null,
    placeholderColor: Color = PrimeTheme.colors.surface2,
) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.card
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().bringIntoViewOnFocus(12.dp),
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
            Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(160.dp, 90.dp).clip(PrimeTheme.shapes.small)) {
                Artwork(episode.posterUrl, placeholderColor, Modifier.size(160.dp, 90.dp))
                if (progress > 0f) ThinProgressBar(progress, Modifier.align(Alignment.BottomStart))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "${episode.number}. ${episode.title}",
                        style = PrimeTheme.typography.listTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    episode.durationSec?.takeIf { it > 0 }?.let {
                        Text("${it / 60} min", style = PrimeTheme.typography.caption, color = colors.textSecondary)
                    }
                }
                episode.plot?.let {
                    Text(it, style = PrimeTheme.typography.cardBody, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (stateLabel != null) Text(stateLabel, style = PrimeTheme.typography.caption, color = colors.textSecondary)
        }
    }
}
