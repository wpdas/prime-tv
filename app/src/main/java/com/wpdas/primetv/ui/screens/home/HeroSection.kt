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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.sp
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
    // Backdrop em toda a largura esticava a imagem (w1280 numa área de ~1780 px) e ficava estourada na TV.
    // Por padrão a arte fica só à direita da área de texto, sem ampliação; FULL_BLEED volta ao modo antigo.
    val fullBleed = HERO_FULL_BLEED && item.backdropUrl != null
    Box(
        modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(PrimeTheme.shapes.panel)
            .background(colors.surface),
    ) {
        // Arte: com backdrop (TMDB) ocupa tudo e o texto fica sobre um degradê; só com pôster, fica à direita.
        AnimatedContent(targetState = item.id, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "heroArt") {
            if (fullBleed) {
                BackdropArtwork(item, Modifier.fillMaxSize())
            } else {
                Row(Modifier.fillMaxSize()) {
                    Spacer(Modifier.width(HERO_ART_START))
                    BackdropArtwork(item, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
        if (fullBleed) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(0f to colors.surface, 0.42f to colors.surface.copy(alpha = 0.92f), 0.72f to Color.Transparent),
                ),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to colors.surface.copy(alpha = 0.6f))))
        } else {
            // Fusão suave entre o painel de texto e a arte.
            Box(
                Modifier
                    .padding(start = HERO_ART_START)
                    .fillMaxHeight()
                    .width(HERO_ART_FADE)
                    .background(Brush.horizontalGradient(listOf(colors.surface, colors.surface.copy(alpha = 0.7f), Color.Transparent))),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.7f to Color.Transparent, 1f to colors.surface.copy(alpha = 0.5f))))
        }
        Column(
            Modifier
                .width(560.dp)
                .fillMaxHeight()
                .padding(horizontal = 40.dp, vertical = 26.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item.badgeKey?.let { TagChip(t(it)) }
                Text(t("home.available_in_list"), style = typography.caption, color = colors.textSecondary)
            }
            // Os textos ocupam o espaço que sobra; os botões nunca encolhem.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                AnimatedContent(targetState = item, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "heroText") { current ->
                    val longTitle = current.title.length > 24
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            current.title,
                            style = if (longTitle) typography.heroTitle.copy(fontSize = 30.sp, lineHeight = 33.sp, letterSpacing = (-0.5).sp) else typography.heroTitle,
                            color = colors.text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(heroMeta(current), style = typography.body, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        current.synopsis?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = typography.body, color = colors.textBody, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 10.dp)) {
                // Séries retomam de onde pararam (o player decide o episódio), por isso o rótulo é genérico.
                PrimeButton(
                    text = t("common.watch"),
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
        if (showDots) {
            Row(
                Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 16.dp),
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

@Composable
private fun heroMeta(item: MediaItem): String {
    val parts = mutableListOf<String>()
    parts += when (item.type) {
        MediaType.MOVIE -> t("meta.movie")
        MediaType.SERIES -> t("meta.series")
        MediaType.CHANNEL -> t("meta.channel")
    }
    item.year?.let { parts += it.toString() }
    if (item.genres.isNotEmpty()) parts += item.genres.joinToString(", ")
    item.seasons?.let { parts += if (it == 1) t("meta.season_one") else t("meta.seasons", "n" to it) }
    item.episodes?.let { parts += if (it == 1) t("meta.episode_one") else t("meta.episodes", "n" to it) }
    item.rating?.let { parts += String.format(java.util.Locale("pt", "BR"), "%.1f", it) }
    return parts.joinToString(" · ")
}

/** Modo antigo: backdrop em toda a largura com o texto sobre um degradê. */
private const val HERO_FULL_BLEED = false
/** Onde a arte começa (fim da área de texto) e a largura da fusão. */
private val HERO_ART_START = 500.dp
private val HERO_ART_FADE = 140.dp
