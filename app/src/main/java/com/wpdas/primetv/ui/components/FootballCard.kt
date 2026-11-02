package com.wpdas.primetv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Card 260×92 de "Jogos de hoje": selo AO VIVO ou horário, competição/canal, partida e escudos. */
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
        modifier = modifier.size(260.dp, 92.dp).bringIntoViewOnFocus(),
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
        val match = if (event.home != null && event.away != null) Match(event.home, event.away)
        else MatchArtist.parse(event.title) ?: MatchArtist.parse(event.channelName)
        val league = event.league ?: match?.let { rememberLeagueLabel(null, it.home, it.away) }
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        event.isLive -> LiveBadge(liveLabel)
                        event.start != null -> Text(formatHour(event.start), style = PrimeTheme.typography.captionStrong, color = colors.text)
                    }
                    // Canal de evento tem o próprio jogo como nome: não repete o título enquanto a liga não chega.
                    Text(league ?: event.channelName.takeUnless { MatchArtist.parse(it) != null }.orEmpty(), style = PrimeTheme.typography.caption, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(event.title, style = PrimeTheme.typography.bodyStrong, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (match != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    TeamBadge(match.home, 30.dp)
                    Text("×", style = PrimeTheme.typography.captionSmall, color = colors.textSecondary)
                    TeamBadge(match.away, 30.dp)
                }
            }
        }
    }
}

internal fun formatHour(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(HOUR_MINUTE)
