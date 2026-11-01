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
) {
    val isSeries: Boolean get() = item?.type == MediaType.SERIES
    val seasonEpisodes: List<Episode> get() = episodes.filter { it.season == selectedSeason }
    val firstEpisode: Episode? get() = episodes.firstOrNull()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
) : ViewModel() {

    private val itemId: String = savedStateHandle.toRoute<DetailsRoute>().itemId
    private val selectedSeason = MutableStateFlow<Int?>(null)
    private val episodesStatus = MutableStateFlow(false to null as String?) // loading, error

    private val _state = MutableStateFlow(DetailsUiState())
    val state: StateFlow<DetailsUiState> = _state.asStateFlow()

    init {
        when {
            itemId.startsWith("xs-") || itemId.startsWith("ms-") -> observeSeries()
            else -> observeMovie()
        }
    }

    private fun observeMovie() {
        catalog.movie(itemId).flatMapLatest { movie ->
            if (movie == null) return@flatMapLatest flowOf(DetailsUiState(loaded = true))
            catalog.movies(movie.categoryId, RELATED + 1).map { related ->
                DetailsUiState(
                    loaded = true,
                    item = movie.toMediaItem(),
                    related = related.filter { it.id != movie.id }.take(RELATED).map { it.toMediaItem() },
                    categoryName = movie.categoryName,
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
        combine(catalog.seriesById(itemId), catalog.episodes(itemId), selectedSeason, episodesStatus) { series, episodes, chosen, status ->
            if (series == null) return@combine DetailsUiState(loaded = true)
            val seasons = episodes.map { it.season }.distinct().sorted()
            val season = chosen?.takeIf { it in seasons } ?: seasons.firstOrNull()
            DetailsUiState(
                loaded = true,
                item = series.toMediaItem().copy(seasons = seasons.size.takeIf { it > 0 }, episodes = episodes.size.takeIf { it > 0 }),
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
        _state.update { s -> s.copy(item = s.item?.copy(inMyList = !(s.item.inMyList))) }
    }

    private companion object {
        const val RELATED = 12
    }
}
