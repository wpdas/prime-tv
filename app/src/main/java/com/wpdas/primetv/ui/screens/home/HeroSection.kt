package com.wpdas.primetv.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.model.MediaType
import com.wpdas.primetv.ui.components.BackdropArtwork
import com.wpdas.primetv.ui.components.PrimeButton
import com.wpdas.primetv.ui.components.PrimeButtonStyle
import com.wpdas.primetv.ui.components.PrimeIconButton
import com.wpdas.primetv.ui.components.TagChip
import com.wpdas.primetv.ui.theme.PrimeTheme

/**
 * Destaque de 300 de altura: painel de texto (520) à esquerda e backdrop à direita,
 * com os pontos de paginação da rotação.
 */
@Composable
fun HeroSection(
    item: MediaItem,
    dots: Int,
    activeDot: Int,
    showDots: Boolean,
    onWatch: (MediaItem) -> Unit,
    onMoreInfo: (MediaItem) -> Unit,
    onToggleMyList: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    watchFocusRequester: FocusRequester? = null,
) {
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    Row(
        modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(PrimeTheme.shapes.panel)
            .background(colors.surface),
    ) {
        Column(
            Modifier
                .width(520.dp)
                .fillMaxHeight()
                .padding(horizontal = 40.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item.badgeKey?.let { TagChip(t(it)) }
                Text(t("home.available_in_list"), style = typography.caption, color = colors.textSecondary)
            }
            AnimatedContent(targetState = item, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "heroText") { current ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(current.title, style = typography.heroTitle, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(heroMeta(current), style = typography.body, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(current.synopsis.orEmpty(), style = typography.body, color = colors.textBody, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 2.dp)) {
                val watchLabel = if (item.type == MediaType.SERIES) t("common.watch_episode", "episode" to "T1:E1") else t("common.watch")
                PrimeButton(
                    text = watchLabel,
                    onClick = { onWatch(item) },
                    icon = Icons.Rounded.PlayArrow,
                    modifier = if (watchFocusRequester != null) Modifier.focusRequester(watchFocusRequester) else Modifier,
                )
                PrimeButton(text = t("common.more_info"), onClick = { onMoreInfo(item) }, style = PrimeButtonStyle.Secondary)
                PrimeIconButton(
                    icon = if (item.inMyList) Icons.Rounded.Check else Icons.Rounded.Add,
                    contentDescription = t(if (item.inMyList) "common.my_list_remove" else "common.my_list_add"),
                    onClick = { onToggleMyList(item) },
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            AnimatedContent(targetState = item.id, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "heroArt") {
                BackdropArtwork(item, Modifier.fillMaxSize())
            }
            // Degradê para o texto do painel não "cortar" seco na arte.
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(Brush.horizontalGradient(listOf(colors.surface, Color.Transparent))),
            )
            if (showDots) {
                Row(
                    Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(dots) { i ->
                        Box(
                            Modifier
                                .size(if (i == activeDot) 22.dp else 8.dp, 4.dp)
                                .background(if (i == activeDot) colors.text else colors.dotInactive, RoundedCornerShape(2.dp)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun heroMeta(item: MediaItem): String {
    val parts = mutableListOf<String>()
    parts += when (item.type) {
        MediaType.MOVIE -> t("meta.movie")
        MediaType.SERIES -> t("meta.series")
        MediaType.CHANNEL -> t("meta.channel")
    }
    if (item.genres.isNotEmpty()) parts += item.genres.joinToString(", ")
    item.seasons?.let { parts += if (it == 1) t("meta.season_one") else t("meta.seasons", "n" to it) }
    item.episodes?.let { parts += if (it == 1) t("meta.episode_one") else t("meta.episodes", "n" to it) }
    item.rating?.let { parts += String.format(java.util.Locale("pt", "BR"), "%.1f", it) }
    return parts.joinToString(" · ")
}
