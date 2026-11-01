package com.wpdas.primetv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.model.MediaType
import androidx.compose.ui.unit.dp

/**
 * Arte de um item (pôster da lista, backdrop do TMDB). Sem URL, desenha um
 * gradiente com a cor de placeholder, para a UI ter forma mesmo sem imagem.
 */
@Composable
fun Artwork(
    url: String?,
    placeholderColor: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Box(modifier.background(placeholderGradient(placeholderColor))) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Pôster do item; canais com nome de partida e sem logo próprio ganham a arte de escudos. */
@Composable
fun PosterArtwork(item: MediaItem, modifier: Modifier = Modifier) {
    val match = if (item.type == MediaType.CHANNEL && item.posterUrl == null) MatchArtist.parse(item.title) else null
    if (match != null) MatchArt(match, modifier, badgeSize = 48.dp)
    else Artwork(item.posterUrl, Color(item.placeholderColor), modifier, item.title)
}

@Composable
fun BackdropArtwork(item: MediaItem, modifier: Modifier = Modifier) {
    val match = if (item.type == MediaType.CHANNEL && item.backdropUrl == null && item.posterUrl == null) MatchArtist.parse(item.title) else null
    if (match != null) MatchArt(match, modifier, badgeSize = 72.dp)
    else Artwork(item.backdropUrl ?: item.posterUrl, Color(item.placeholderColor), modifier, null)
}

private fun placeholderGradient(base: Color): Brush = Brush.linearGradient(
    colors = listOf(base.lighten(0.18f), base, base.darken(0.25f)),
)

private fun Color.lighten(f: Float) = Color(
    red + (1f - red) * f, green + (1f - green) * f, blue + (1f - blue) * f, alpha,
)

private fun Color.darken(f: Float) = Color(red * (1f - f), green * (1f - f), blue * (1f - f), alpha)
