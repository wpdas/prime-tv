package com.wpdas.primetv.ui.screens.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpdas.primetv.domain.model.Channel
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.EpgProgram
import com.wpdas.primetv.domain.model.NowNext
import com.wpdas.primetv.domain.repository.CatalogRepository
import com.wpdas.primetv.domain.repository.ChannelFilter
import com.wpdas.primetv.domain.repository.LibraryRepository
import com.wpdas.primetv.domain.model.MediaType
import com.wpdas.primetv.data.player.PlayerFactory
import kotlinx.coroutines.launch
import com.wpdas.primetv.ui.components.RailCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class LiveUiState(
    val rail: List<RailCategory> = emptyList(),
    val selectedRailId: String = RAIL_ALL,
    val channels: List<Channel>? = null,
    /** Programa no ar por [Channel.epgKey]. */
    val nowPlaying: Map<String, EpgProgram> = emptyMap(),
    val now: Long = System.currentTimeMillis(),
    val focused: Channel? = null,
    val focusedNowNext: NowNext = NowNext(null, emptyList()),
    val favoriteIds: Set<String> = emptySet(),
    /** URL da prévia (1 s depois do foco) ou null. */
    val previewUrl: String? = null,
    /** Chave i18n de um aviso curto (favoritado/removido). */
    val toastKey: String? = null,
) {
    companion object {
        const val RAIL_ALL = "all"
        const val RAIL_FOOTBALL = "football"
        const val RAIL_FAVORITES = "favorites"
    }
}

private const val RAIL_ALL = LiveUiState.RAIL_ALL
private const val RAIL_FOOTBALL = LiveUiState.RAIL_FOOTBALL
private const val RAIL_FAVORITES = LiveUiState.RAIL_FAVORITES

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class LiveViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
    val playerFactory: PlayerFactory,
) : ViewModel() {

    private val _state = MutableStateFlow(LiveUiState())
    val state: StateFlow<LiveUiState> = _state.asStateFlow()

    private val selected = MutableStateFlow(RAIL_ALL)
    private val focusedChannel = MutableStateFlow<Channel?>(null)

    /** Tique por minuto para o relógio do guia e a barra de progresso. */
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    init {
        val favoriteChannels = library.favoriteIds(MediaType.CHANNEL)
        favoriteChannels.onEach { ids -> _state.update { it.copy(favoriteIds = ids.toSet()) } }.launchIn(viewModelScope)

        combine(
            catalog.categories(ContentSection.LIVE),
            catalog.categoryCounts(ContentSection.LIVE),
            catalog.footballChannelCount(),
            favoriteChannels,
        ) { categories, counts, football, favorites ->
            val byId = counts.associate { it.categoryId to it.count }
            val total = counts.sumOf { it.count }
            buildList {
                if (favorites.isNotEmpty()) add(RailCategory(RAIL_FAVORITES, "Favoritos", favorites.size))
                if (football > 0) add(RailCategory(RAIL_FOOTBALL, "Futebol", football))
                add(RailCategory(RAIL_ALL, "Todos", total))
                categories.forEach { add(RailCategory(it.id, it.name, byId[it.id] ?: 0)) }
            }
        }.onEach { rail -> _state.update { it.copy(rail = rail) } }.launchIn(viewModelScope)

        combine(selected, favoriteChannels) { id, favs -> filterOf(id, favs) }
            .flatMapLatest { filter -> catalog.channels(filter) }
            .onEach { list -> _state.update { it.copy(channels = list) } }
            .launchIn(viewModelScope)

        combine(selected, ticker, favoriteChannels) { id, now, favs -> filterOf(id, favs) to now }
            .onEach { (filter, now) ->
                val airing = runCatching { catalog.nowPlaying(filter, now) }.getOrDefault(emptyMap())
                _state.update { it.copy(nowPlaying = airing, now = now) }
            }
            .launchIn(viewModelScope)

        // Prévia: só depois de ~1 s parado no canal, para não abrir stream a cada tecla.
        focusedChannel.debounce(PREVIEW_DELAY_MS)
            .onEach { channel -> _state.update { it.copy(previewUrl = channel?.streamUrl) } }
            .launchIn(viewModelScope)

        focusedChannel.debounce(150)
            .flatMapLatest { channel ->
                if (channel == null) flowOf(null to NowNext(null, emptyList()))
                else catalog.nowNext(channel, System.currentTimeMillis(), upcoming = 2).map { channel to it }
            }
            .onEach { (channel, nowNext) -> _state.update { it.copy(focused = channel, focusedNowNext = nowNext) } }
            .launchIn(viewModelScope)
    }

    fun selectRail(id: String) {
        if (selected.value == id) return
        selected.value = id
        _state.update { it.copy(selectedRailId = id, channels = null) }
    }

    fun onChannelFocused(channel: Channel) {
        focusedChannel.value = channel
    }

    /** Segurar OK no canal: entra/sai dos favoritos e mostra um aviso curto. */
    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            val added = library.toggleFavorite(channel.id, MediaType.CHANNEL)
            _state.update { it.copy(toastKey = if (added) "favorites.added" else "favorites.removed") }
            delay(TOAST_MS)
            _state.update { if (it.toastKey != null) it.copy(toastKey = null) else it }
        }
    }

    /** Libera a prévia ao sair da tela para o player principal (servidores com 1 conexão). */
    fun stopPreview() {
        focusedChannel.value = null
        _state.update { it.copy(previewUrl = null) }
    }

    private fun filterOf(id: String, favorites: List<String>): ChannelFilter = when (id) {
        RAIL_ALL -> ChannelFilter.All
        RAIL_FOOTBALL -> ChannelFilter.Football
        RAIL_FAVORITES -> ChannelFilter.Favorites(favorites)
        else -> ChannelFilter.Category(id)
    }

    private companion object {
        const val PREVIEW_DELAY_MS = 1_000L
        const val TOAST_MS = 2_500L
    }
}
