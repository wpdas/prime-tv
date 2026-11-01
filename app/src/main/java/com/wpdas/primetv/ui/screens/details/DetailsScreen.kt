package com.wpdas.primetv.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.domain.model.Episode
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.model.MediaType
import com.wpdas.primetv.ui.components.BackLink
import com.wpdas.primetv.ui.components.EpisodeRow
import com.wpdas.primetv.ui.components.LoadingSpinner
import com.wpdas.primetv.ui.components.PosterArtwork
import com.wpdas.primetv.ui.components.PosterCard
import com.wpdas.primetv.ui.components.PrimeButton
import com.wpdas.primetv.ui.components.PrimeButtonStyle
import com.wpdas.primetv.ui.components.SelectableChip
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeTheme
import java.util.Locale

@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    onPlay: (itemId: String, episode: Episode?) -> Unit,
    onOpenItem: (MediaItem) -> Unit,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    val watchFocus = remember { FocusRequester() }
    LaunchedEffect(state.item?.id) { if (state.item != null) runCatching { watchFocus.requestFocus() } }
    // Ao voltar do player, o progresso do filme pode ter mudado.
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event -> if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.refreshProgress() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val item = state.display
    val context = androidx.compose.ui.platform.LocalContext.current
    val trailerIntent = androidx.compose.runtime.remember(state.tmdb?.trailerYoutubeKey) {
        state.tmdb?.trailerYoutubeKey?.let { key -> youtubeIntent(context, key) }
    }
    if (!state.loaded) {
        Box(Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) { LoadingSpinner(size = 32.dp) }
        return
    }
    if (item == null) {
        Column(Modifier.fillMaxSize().background(colors.background).padding(PrimeDimens.screenMargin), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BackLink(onClick = onBack, modifier = Modifier.focusRequester(watchFocus))
            Text(t("details.not_found"), style = typography.bodyLarge, color = colors.textSecondary)
        }
        LaunchedEffect(Unit) { runCatching { watchFocus.requestFocus() } }
        return
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        // Backdrop do TMDB ao fundo, bem escurecido, só para dar clima.
        item.backdropUrl?.let { url ->
            com.wpdas.primetv.ui.components.Artwork(url, colors.background, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(colors.background.copy(alpha = 0.82f)))
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(colors.background, colors.background.copy(alpha = 0.3f)))))
        }
    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = PrimeDimens.screenMargin, vertical = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        // ---------------- coluna esquerda: capa, metadados, ações ----------------
        Column(Modifier.width(420.dp).fillMaxHeight().focusGroup(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BackLink(onClick = onBack)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Bottom) {
                Box(Modifier.size(150.dp, 220.dp).clip(PrimeTheme.shapes.card)) { PosterArtwork(item, Modifier.fillMaxSize()) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(listOfNotNull(typeLabel(item.type), item.year?.toString()).joinToString(" · "), style = typography.caption, color = colors.textSecondary)
                    if (item.genres.isNotEmpty()) Text(item.genres.joinToString(", "), style = typography.caption, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (state.isSeries && (state.seasons.isNotEmpty() || item.seasons != null)) {
                        val seasonsCount = state.seasons.size.takeIf { it > 0 } ?: item.seasons ?: 0
                        val episodesCount = state.episodeCount.takeIf { it > 0 } ?: item.episodes ?: 0
                        val seasonsText = if (seasonsCount == 1) t("details.season_count_one") else t("details.seasons_count", "n" to seasonsCount)
                        Text("$seasonsText · ${t("details.episodes_count", "n" to episodesCount)}", style = typography.caption, color = colors.textSecondary)
                    }
                    item.rating?.let { Text(t("details.rating", "n" to String.format(Locale("pt", "BR"), "%.1f", it)), style = typography.caption, color = colors.accentSoftText) }
                }
            }
            Text(item.title, style = typography.screenTitle, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            item.synopsis?.let { Text(it, style = typography.body, color = colors.textBody, maxLines = 5, overflow = TextOverflow.Ellipsis) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                val resume = state.resumeEpisode
                val movieProgress = state.movieProgress
                val watchLabel = when {
                    state.isSeries && resume != null -> {
                        val label = "T${resume.season}:E${resume.number}"
                        if (state.resumeIsContinue) t("common.continue_episode", "episode" to label) else t("common.watch_episode", "episode" to label)
                    }
                    movieProgress != null && !movieProgress.finished && movieProgress.positionMs > 0 -> t("common.continue")
                    else -> t("common.watch")
                }
                PrimeButton(
                    text = watchLabel,
                    onClick = { onPlay(item.id, resume) },
                    icon = Icons.Rounded.PlayArrow,
                    enabled = !state.isSeries || resume != null,
                    modifier = Modifier.focusRequester(watchFocus),
                )
                PrimeButton(
                    text = t(if (item.inMyList) "details.in_my_list" else "details.my_list"),
                    onClick = viewModel::toggleMyList,
                    style = PrimeButtonStyle.Secondary,
                    icon = if (item.inMyList) Icons.Rounded.Check else Icons.Rounded.Add,
                    horizontalPadding = 18.dp,
                    textStyle = typography.listTitle,
                )
            }
            if (trailerIntent != null) {
                PrimeButton(
                    text = t("details.watch_trailer"),
                    onClick = { runCatching { context.startActivity(trailerIntent) } },
                    style = PrimeButtonStyle.Outline,
                    icon = Icons.Rounded.PlayArrow,
                    height = 44.dp,
                    horizontalPadding = 16.dp,
                )
            }
            state.tmdb?.let { Text(t("details.tmdb_credit"), style = typography.captionSmall, color = colors.textSecondary) }
        }

        // ---------------- coluna direita: temporadas/episódios ou relacionados ----------------
        Column(Modifier.weight(1f).fillMaxHeight().padding(top = 48.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.isSeries) {
                SeriesEpisodes(state, onSelectSeason = viewModel::selectSeason, onPlay = { onPlay(item.id, it) }, placeholder = Color(item.placeholderColor))
            } else if (state.related.isNotEmpty()) {
                Text(t("details.related", "category" to (state.categoryName ?: "")), style = typography.sectionTitle, color = colors.text)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize().focusRestorer().focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(PrimeDimens.rowGap),
                    verticalArrangement = Arrangement.spacedBy(PrimeDimens.rowGap),
                    contentPadding = PaddingValues(8.dp),
                ) {
                    items(state.related, key = { it.id }) { related -> PosterCard(item = related, onClick = { onOpenItem(related) }) }
                }
            }
        }
    }
    }
}

/** Abre o trailer no app do YouTube (celular ou TV). null quando nenhum está instalado. */
private fun youtubeIntent(context: android.content.Context, key: String): android.content.Intent? {
    val pm = context.packageManager
    val candidates = listOf(
        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("vnd.youtube:$key")),
        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com/watch?v=$key")).setPackage("com.google.android.youtube.tv"),
        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com/watch?v=$key")).setPackage("com.google.android.youtube"),
    )
    return candidates.firstOrNull { it.resolveActivity(pm) != null }
}

@Composable
private fun SeriesEpisodes(state: DetailsUiState, onSelectSeason: (Int) -> Unit, onPlay: (Episode) -> Unit, placeholder: Color) {
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    when {
        state.loadingEpisodes -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LoadingSpinner()
            Text(t("details.loading_episodes"), style = typography.body, color = colors.textSecondary)
        }
        state.episodesError != null && state.episodes.isEmpty() ->
            Text(t("details.episodes_error", "message" to t(state.episodesError)), style = typography.body, color = colors.live)
        state.episodes.isEmpty() -> Text(t("details.no_episodes"), style = typography.body, color = colors.textSecondary)
        else -> {
            LazyRow(Modifier.fillMaxWidth().focusRestorer().focusGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(4.dp)) {
                items(state.seasons, key = { it }) { season ->
                    SelectableChip(text = t("details.season", "n" to season), selected = season == state.selectedSeason, onClick = { onSelectSeason(season) })
                }
            }
            LazyColumn(Modifier.fillMaxSize().focusRestorer().focusGroup(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(4.dp)) {
                items(state.seasonEpisodes, key = { it.id }) { episode ->
                    val p = state.progress[episode.id]
                    EpisodeRow(
                        episode = episode,
                        onClick = { onPlay(episode) },
                        placeholderColor = placeholder,
                        progress = p?.takeUnless { it.finished }?.fraction ?: 0f,
                        stateLabel = if (p?.finished == true) t("details.watched") else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun typeLabel(type: MediaType): String = when (type) {
    MediaType.MOVIE -> t("meta.movie")
    MediaType.SERIES -> t("meta.series")
    MediaType.CHANNEL -> t("meta.channel")
}
