package com.wpdas.primetv.ui.screens.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wpdas.primetv.data.repository.toMediaItem
import com.wpdas.primetv.domain.model.Episode
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.model.MediaType
import com.wpdas.primetv.domain.model.PlaylistException
import com.wpdas.primetv.domain.repository.CatalogRepository
import com.wpdas.primetv.domain.repository.LibraryRepository
import com.wpdas.primetv.domain.repository.RecommendationRepository
import com.wpdas.primetv.domain.model.TmdbInfo
import com.wpdas.primetv.domain.model.WatchProgress
import com.wpdas.primetv.ui.navigation.DetailsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailsUiState(
    val loaded: Boolean = false,
    val item: MediaItem? = null,
    val seasons: List<Int> = emptyList(),
    val selectedSeason: Int? = null,
    val episodes: List<Episode> = emptyList(),
    val episodeCount: Int = 0,
    val loadingEpisodes: Boolean = false,
    val episodesError: String? = null,
    val related: List<MediaItem> = emptyList(),
    val categoryName: String? = null,
    /** Progresso por id de episódio (séries) ou do próprio filme (chave = id do filme). */
    val progress: Map<String, WatchProgress> = emptyMap(),
    val tmdb: TmdbInfo? = null,
) {
    /** Item com os dados do TMDB por cima dos da lista (capa da lista continua tendo prioridade). */
    val display: MediaItem? get() {
        val base = item ?: return null
        val info = tmdb ?: return base
        return base.copy(
            backdropUrl = info.backdropUrl ?: base.backdropUrl,
            posterUrl = base.posterUrl ?: info.posterUrl,
            synopsis = info.overview?.takeIf { it.isNotBlank() } ?: base.synopsis,
            genres = info.genres.take(3).ifEmpty { base.genres },
            rating = info.rating ?: base.rating,
            year = base.year ?: info.year,
            seasons = base.seasons ?: info.seasons,
            episodes = base.episodes ?: info.episodes,
        )
    }
    val isSeries: Boolean get() = item?.type == MediaType.SERIES
    val seasonEpisodes: List<Episode> get() = episodes.filter { it.season == selectedSeason }
    val firstEpisode: Episode? get() = episodes.firstOrNull()

    /** Episódio para o botão principal: em andamento, o seguinte ao último concluído, ou o primeiro. */
    val resumeEpisode: Episode? get() {
        val latest = progress.values.filter { it.seriesId != null }.maxByOrNull { it.updatedAt } ?: return firstEpisode
        val index = episodes.indexOfFirst { it.id == latest.itemId }
        if (index < 0) return firstEpisode
        return if (latest.finished) episodes.getOrNull(index + 1) ?: episodes[index] else episodes[index]
    }
    val resumeIsContinue: Boolean get() {
        val ep = resumeEpisode ?: return false
        val p = progress[ep.id]
        return (p != null && !p.finished && p.positionMs > 0) || progress.values.any { it.seriesId != null }
    }
    val movieProgress: WatchProgress? get() = item?.takeIf { it.type == MediaType.MOVIE }?.let { progress[it.id] }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
    private val recommendations: RecommendationRepository,
) : ViewModel() {

    private val tmdbInfo = MutableStateFlow<TmdbInfo?>(null)
    private var enrichStarted = false

    /** Busca o TMDB uma vez por tela, assim que o item da lista estiver carregado. */
    private fun enrichOnce(item: MediaItem) {
        if (enrichStarted) return
        enrichStarted = true
        viewModelScope.launch { tmdbInfo.value = runCatching { recommendations.enrich(item) }.getOrNull() }
    }

    private val itemId: String = savedStateHandle.toRoute<DetailsRoute>().itemId
    private val selectedSeason = MutableStateFlow<Int?>(null)
    private val episodesStatus = MutableStateFlow(false to null as String?) // loading, error
    private val progressTick = MutableStateFlow(0)

    private val _state = MutableStateFlow(DetailsUiState())
    val state: StateFlow<DetailsUiState> = _state.asStateFlow()

    init {
        when {
            itemId.startsWith("xs-") || itemId.startsWith("ms-") -> observeSeries()
            else -> observeMovie()
        }
    }

    private fun observeMovie() {
        combine(catalog.movie(itemId), library.favoriteIds(), progressTick, tmdbInfo) { movie, favorites, _, info -> Triple(movie, favorites, info) }
            .flatMapLatest { (movie, favorites, info) ->
                if (movie == null) return@flatMapLatest flowOf(DetailsUiState(loaded = true))
                val item = movie.toMediaItem(inMyList = movie.id in favorites)
                enrichOnce(item)
                catalog.movies(movie.categoryId, RELATED + 1).map { related ->
                    DetailsUiState(
                        loaded = true,
                        item = item,
                        related = related.filter { it.id != movie.id }.take(RELATED).map { it.toMediaItem() },
                        categoryName = movie.categoryName,
                        progress = library.progress(movie.id)?.let { mapOf(movie.id to it) }.orEmpty(),
                        tmdb = info,
                    )
                }
            }.onEach { s -> _state.value = s }.launchIn(viewModelScope)
    }

    private fun observeSeries() {
        viewModelScope.launch {
            episodesStatus.value = true to null
            try {
                catalog.ensureEpisodes(itemId)
                episodesStatus.value = false to null
            } catch (e: PlaylistException) {
                episodesStatus.value = false to e.error.messageKey
            } catch (e: Exception) {
                episodesStatus.value = false to (e.message ?: e.javaClass.simpleName)
            }
        }
        combine(
            catalog.seriesById(itemId), catalog.episodes(itemId), selectedSeason, episodesStatus,
            combine(library.favoriteIds(), library.seriesProgress(itemId), tmdbInfo) { f, p, i -> Triple(f, p, i) },
        ) { series, episodes, chosen, status, (favorites, progress, info) ->
            if (series == null) return@combine DetailsUiState(loaded = true)
            enrichOnce(series.toMediaItem())
            val seasons = episodes.map { it.season }.distinct().sorted()
            val latest = progress.values.maxByOrNull { it.updatedAt }
            val resumeSeason = latest?.let { l -> episodes.firstOrNull { it.id == l.itemId }?.season }
            val season = chosen?.takeIf { it in seasons } ?: resumeSeason ?: seasons.firstOrNull()
            DetailsUiState(
                loaded = true,
                item = series.toMediaItem(inMyList = series.id in favorites).copy(seasons = seasons.size.takeIf { it > 0 }, episodes = episodes.size.takeIf { it > 0 }),
                progress = progress,
                tmdb = info,
                seasons = seasons,
                selectedSeason = season,
                episodes = episodes,
                episodeCount = episodes.size,
                loadingEpisodes = status.first && episodes.isEmpty(),
                episodesError = status.second,
                categoryName = series.categoryName,
            )
        }.onEach { s -> _state.value = s }.launchIn(viewModelScope)
    }

    fun selectSeason(season: Int) {
        selectedSeason.value = season
    }

    fun toggleMyList() {
        val item = _state.value.item ?: return
        viewModelScope.launch { library.toggleFavorite(item.id, item.type) }
    }

    /** Recarrega o progresso do filme ao voltar do player. */
    fun refreshProgress() {
        progressTick.update { it + 1 }
    }

    private companion object {
        const val RELATED = 12
    }
}
