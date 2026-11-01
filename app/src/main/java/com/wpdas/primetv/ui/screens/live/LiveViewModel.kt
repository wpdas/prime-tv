package com.wpdas.primetv.ui.screens.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpdas.primetv.domain.model.Channel
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.EpgProgram
import com.wpdas.primetv.domain.model.NowNext
import com.wpdas.primetv.domain.repository.CatalogRepository
import com.wpdas.primetv.domain.repository.ChannelFilter
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
) {
    companion object {
        const val RAIL_ALL = "all"
        const val RAIL_FOOTBALL = "football"
    }
}

private const val RAIL_ALL = LiveUiState.RAIL_ALL
private const val RAIL_FOOTBALL = LiveUiState.RAIL_FOOTBALL

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class LiveViewModel @Inject constructor(
    private val catalog: CatalogRepository,
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
        combine(
            catalog.categories(ContentSection.LIVE),
            catalog.categoryCounts(ContentSection.LIVE),
            catalog.footballChannelCount(),
        ) { categories, counts, football ->
            val byId = counts.associate { it.categoryId to it.count }
            val total = counts.sumOf { it.count }
            buildList {
                if (football > 0) add(RailCategory(RAIL_FOOTBALL, "Futebol", football))
                add(RailCategory(RAIL_ALL, "Todos", total))
                categories.forEach { add(RailCategory(it.id, it.name, byId[it.id] ?: 0)) }
            }
        }.onEach { rail -> _state.update { it.copy(rail = rail) } }.launchIn(viewModelScope)

        selected.flatMapLatest { id -> catalog.channels(filterOf(id)) }
            .onEach { list -> _state.update { it.copy(channels = list) } }
            .launchIn(viewModelScope)

        combine(selected, ticker) { id, now -> id to now }
            .onEach { (id, now) ->
                val airing = runCatching { catalog.nowPlaying(filterOf(id), now) }.getOrDefault(emptyMap())
                _state.update { it.copy(nowPlaying = airing, now = now) }
            }
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

    private fun filterOf(id: String): ChannelFilter = when (id) {
        RAIL_ALL -> ChannelFilter.All
        RAIL_FOOTBALL -> ChannelFilter.Football
        else -> ChannelFilter.Category(id)
    }
}
