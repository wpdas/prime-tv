package com.wpdas.primetv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wpdas.primetv.domain.model.FootballEvent
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme

/** Card 220×92 de "Futebol agora": selo AO VIVO, canal e partida. */
@Composable
fun FootballCard(
    event: FootballEvent,
    liveLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.card
    Surface(
        onClick = onClick,
        modifier = modifier.size(220.dp, 92.dp),
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.surface,
            contentColor = colors.text,
            focusedContainerColor = colors.selected,
            focusedContentColor = colors.text,
            pressedContainerColor = colors.selected,
            pressedContentColor = colors.text,
        ),
        scale = PrimeFocus.scale(),
        border = PrimeFocus.border(shape),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (event.isLive) LiveBadge(liveLabel)
                Text(event.channelName, style = PrimeTheme.typography.caption, color = colors.textSecondary, maxLines = 1)
            }
            Text(event.title, style = PrimeTheme.typography.bodyStrong, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
