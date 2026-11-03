package com.wpdas.primetv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.wpdas.primetv.domain.football.LeagueNames
import com.wpdas.primetv.domain.football.MatchEvent
import com.wpdas.primetv.domain.football.MatchStatus
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme

/**
 * Linha do guia de jogos (altura 84): competição | mandante + escudo | horário/status | escudo + visitante | canal.
 * [dateLabel] aparece sob o horário ("02/11"); [statusLabel] substitui a data quando o jogo acabou.
 */
@Composable
fun MatchRow(
    event: MatchEvent,
    status: MatchStatus,
    liveLabel: String,
    finishedLabel: String,
    dateLabel: String,
    moreChannelsLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: (() -> Unit)? = null,
) {
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    val shape = PrimeTheme.shapes.card
    val leagueInfo = rememberLeague(event.leagueHint, event.home, event.away, event.start)
    val league = leagueInfo.label
    val dim = status == MatchStatus.FINISHED
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(84.dp).onFocusChanged { if (it.isFocused) onFocused?.invoke() },
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
        Row(Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // Competição
            // Competição desconhecida: coluna vazia (nada de chute pela liga do clube).
            Row(Modifier.width(176.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (league != null) {
                    LeagueBadge(league, leagueInfo.badgeUrl, 36.dp)
                    Text(league, style = typography.caption, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            // Mandante
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                Text(event.home, style = typography.listTitle, color = if (dim) colors.textSecondary else colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false))
                TeamBadge(event.home, 40.dp)
            }
            // Horário / status
            Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(formatHour(event.start), style = typography.title, color = if (dim) colors.textSecondary else colors.text)
                when (status) {
                    MatchStatus.LIVE -> LiveBadge(liveLabel)
                    MatchStatus.FINISHED -> Text(finishedLabel, style = typography.captionSmall, color = colors.textSecondary)
                    MatchStatus.UPCOMING -> Text(dateLabel, style = typography.captionSmall, color = colors.textSecondary)
                }
            }
            // Visitante
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TeamBadge(event.away, 40.dp)
                Text(event.away, style = typography.listTitle, color = if (dim) colors.textSecondary else colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
            // Canal: num canal de evento o nome é o próprio jogo; mostra a categoria ("Jogos do dia").
            val channelLabel = if (event.fromChannelName) event.categoryName?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "" else event.channelName
            Column(Modifier.width(132.dp), horizontalAlignment = Alignment.End) {
                Text(channelLabel, style = typography.caption, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                if (moreChannelsLabel != null) Text(moreChannelsLabel, style = typography.captionSmall, color = colors.textSecondary, textAlign = TextAlign.End)
            }
        }
    }
}

/** Escudo da competição: imagem da base pública quando houver; senão iniciais; sem nome ainda, um círculo neutro. */
@Composable
private fun LeagueBadge(league: String?, badgeUrl: String?, size: androidx.compose.ui.unit.Dp) {
    val colors = PrimeTheme.colors
    val fallback: @Composable () -> Unit = {
        Box(Modifier.size(size).background(colors.surface2, CircleShape), contentAlignment = Alignment.Center) {
            if (league != null) Text(LeagueNames.initials(league), style = PrimeTheme.typography.badge, color = colors.text)
        }
    }
    if (badgeUrl == null) {
        fallback()
        return
    }
    coil3.compose.SubcomposeAsyncImage(
        model = badgeUrl,
        contentDescription = league,
        modifier = Modifier.size(size),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        loading = { fallback() },
        error = { fallback() },
    )
}
