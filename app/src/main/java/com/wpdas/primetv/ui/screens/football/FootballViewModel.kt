package com.wpdas.primetv.ui.screens.football

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpdas.primetv.domain.football.MatchEvent
import com.wpdas.primetv.domain.football.MatchSchedule
import com.wpdas.primetv.domain.football.MatchStatus
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.HomeTab
import com.wpdas.primetv.domain.model.SyncState
import com.wpdas.primetv.domain.repository.CatalogRepository
import com.wpdas.primetv.domain.repository.ChannelFilter
import com.wpdas.primetv.domain.repository.HomeRepository
import com.wpdas.primetv.domain.sync.CatalogSync
import com.wpdas.primetv.ui.components.RailCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class FootballViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val home: HomeRepository,
    private val sync: CatalogSync,
    private val artWarmer: com.wpdas.primetv.domain.crests.MatchArtWarmer,
) : ViewModel() {

    private val _state = MutableStateFlow(FootballUiState())
    val state: StateFlow<FootballUiState> = _state.asStateFlow()

    /** Tique por minuto: status ao vivo/encerrado e rótulos do trilho. */
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    private var lastPlayedKey: String? = null

    init {
        combine(catalog.matchSchedule(), ticker) { events, now -> events to now }
            .onEach { (events, now) -> _state.update { it.copy(events = events, now = now) } }
            .launchIn(viewModelScope)

        // Escudos e competições dos jogos, só com a tela aberta; recomeça se a lista de jogos mudar.
        _state.map { it.events }
            .filterNotNull()
            .distinctUntilChanged { old, new -> old.map { it.home to it.away } == new.map { it.home to it.away } }
            .debounce(WARM_DELAY_MS)
            .mapLatest { events -> runCatching { artWarmer.warm(events) } }
            .launchIn(viewModelScope)

        combine(catalog.channels(ChannelFilter.Football), ticker) { channels, now ->
            val airing = runCatching { catalog.nowPlaying(ChannelFilter.Football, now) }.getOrDefault(emptyMap())
            // Slots genéricos ("Brasileirão", "Jogos do Dia") por último: raramente têm algo no ar.
            channels.sortedBy { if (MatchSchedule.isPlaceholder(it)) 1 else 0 } to airing
        }.onEach { (channels, airing) -> _state.update { it.copy(channels = channels, nowPlaying = airing) } }
            .launchIn(viewModelScope)

        home.availableTabs().onEach { tabs -> _state.update { it.copy(tabs = tabs) } }.launchIn(viewModelScope)

        sync.status.onEach { status ->
            _state.update { it.copy(refreshing = status[ContentSection.LIVE] is SyncState.Running || status[ContentSection.EPG] is SyncState.Running) }
        }.launchIn(viewModelScope)

        _state.onEach { s -> _state.update { it.copy(rail = buildRail(s)) } }.launchIn(viewModelScope)

        refreshIfStale()
    }

    private fun buildRail(s: FootballUiState): List<RailCategory> {
        val events = s.events.orEmpty()
        val live = events.count { it.status(s.now) == MatchStatus.LIVE }
        val rail = listOf(
            RailCategory(FootballUiState.RAIL_TODAY, "Jogos", events.size),
            RailCategory(FootballUiState.RAIL_LIVE, "Ao vivo agora", live),
            RailCategory(FootballUiState.RAIL_CHANNELS, "Canais", s.channels?.size),
        )
        return if (rail == s.rail) s.rail else rail
    }

    fun selectRail(id: String) {
        if (_state.value.selectedRailId == id) return
        _state.update { it.copy(selectedRailId = id) }
    }

    /** Os slots de jogo são renomeados pelo provedor ao longo do dia: lista de canais com mais de 1 h pede atualização. */
    private fun refreshIfStale() {
        val now = System.currentTimeMillis()
        val status = sync.status.value
        if (status.values.any { it is SyncState.Running }) return
        val sections = buildList {
            val live = status[ContentSection.LIVE]
            if (live !is SyncState.Done || now - live.finishedAt > LIVE_STALE_MS) add(ContentSection.LIVE)
            val epg = status[ContentSection.EPG]
            if (epg is SyncState.Done && now - epg.finishedAt > EPG_STALE_MS) add(ContentSection.EPG)
        }
        if (sections.isNotEmpty()) sync.sync(sections)
    }

    /** Botão "Atualizar lista": canais e guia agora. */
    fun refresh() = sync.sync(listOf(ContentSection.LIVE, ContentSection.EPG))

    fun markPlayed(event: MatchEvent) { lastPlayedKey = event.key }

    /** Ao voltar do player: foca o jogo que estava em tela cheia. */
    fun focusLastPlayed() {
        val key = lastPlayedKey ?: return
        lastPlayedKey = null
        _state.update { it.copy(focusEventKey = key) }
    }

    fun onFocusConsumed() = _state.update { it.copy(focusEventKey = null) }

    private companion object {
        /** Espera a tela assentar (e a lista de jogos parar de mudar) antes de buscar. */
        const val WARM_DELAY_MS = 800L
        const val LIVE_STALE_MS = 60 * 60_000L
        const val EPG_STALE_MS = 6 * 60 * 60_000L
    }
}
