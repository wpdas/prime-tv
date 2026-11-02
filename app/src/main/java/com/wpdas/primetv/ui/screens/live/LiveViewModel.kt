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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.firstOrNull
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
    /** Dígitos digitados para pular ao canal com esse número. */
    val channelInput: String? = null,
    /** Canal a focar (número digitado); a tela consome e zera. */
    val jumpToChannelId: String? = null,
    val previewSound: Boolean = false,
    /** Abas da barra superior (Kids/Anime só com conteúdo). */
    val tabs: List<com.wpdas.primetv.domain.model.HomeTab> = com.wpdas.primetv.domain.model.HomeTab.entries.filter { it !in com.wpdas.primetv.domain.model.HomeTab.OPTIONAL },
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
    private val settings: com.wpdas.primetv.domain.repository.SettingsRepository,
    private val liveState: com.wpdas.primetv.data.local.prefs.LiveStateStore,
    private val home: com.wpdas.primetv.domain.repository.HomeRepository,
    val playerFactory: PlayerFactory,
) : ViewModel() {

    /** Posição lembrada da última visita (categoria + canal), aplicada quando a lista carregar. */
    private var restoreChannelId: String? = null

    private var digitJob: kotlinx.coroutines.Job? = null

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
        viewModelScope.launch {
            val saved = liveState.read()
            if (saved != null) {
                restoreChannelId = saved.channelId
                selectRail(saved.railId)
                if (saved.channelId != null) _state.update { it.copy(jumpToChannelId = saved.channelId) }
            }
        }
        // Lembra categoria e canal em foco (com atraso, para não gravar a cada tecla).
        combine(selected, focusedChannel.debounce(800)) { rail, ch -> rail to ch?.id }
            .distinctUntilChanged()
            .onEach { (rail, ch) -> liveState.save(rail, ch ?: restoreChannelId) }
            .launchIn(viewModelScope)
        settings.settings.onEach { s -> _state.update { it.copy(previewSound = s.previewSound) } }.launchIn(viewModelScope)
        home.availableTabs().onEach { tabs -> _state.update { it.copy(tabs = tabs) } }.launchIn(viewModelScope)
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
        restoreChannelId = null
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

    /** Dígito do controle: acumula e, após uma pausa, foca o canal com esse número na lista atual. */
    fun onDigit(digit: Int) {
        val current = (_state.value.channelInput ?: "").takeIf { it.length < 4 } ?: ""
        val input = current + digit
        _state.update { it.copy(channelInput = input) }
        digitJob?.cancel()
        digitJob = viewModelScope.launch {
            delay(DIGIT_COMMIT_MS)
            commitChannelInput()
        }
    }

    fun commitChannelInput() {
        digitJob?.cancel()
        val number = _state.value.channelInput?.toIntOrNull() ?: run { _state.update { it.copy(channelInput = null) }; return }
        val target = _state.value.channels?.firstOrNull { it.number == number }
        if (target == null) {
            viewModelScope.launch {
                // Fora da categoria atual: volta para Todos e procura lá.
                val all = catalog.channelsOnce(ChannelFilter.All)
                val found = all.firstOrNull { it.number == number }
                if (found != null) {
                    selectRail(RAIL_ALL)
                    _state.update { it.copy(channelInput = null, jumpToChannelId = found.id) }
                } else {
                    _state.update { it.copy(channelInput = null, toastKey = "player.channel_not_found") }
                    delay(TOAST_MS)
                    _state.update { if (it.toastKey == "player.channel_not_found") it.copy(toastKey = null) else it }
                }
            }
        } else {
            _state.update { it.copy(channelInput = null, jumpToChannelId = target.id) }
        }
    }

    fun onJumpConsumed() = _state.update { it.copy(jumpToChannelId = null) }

    private var returnFromPlayer = false

    /** Marca que o próximo retorno à tela vem do player. */
    fun markReturnFocus() { returnFromPlayer = true }

    /** Ao voltar do player: foca o canal que estava em tela cheia (último do histórico). */
    fun focusLastPlayed() {
        if (!returnFromPlayer) return
        returnFromPlayer = false
        viewModelScope.launch {
            val last = library.recentChannels(1).firstOrNull()?.firstOrNull() ?: return@launch
            val inList = _state.value.channels?.any { it.id == last.id } == true
            if (!inList) selectRail(RAIL_ALL)
            _state.update { it.copy(jumpToChannelId = last.id) }
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
        const val DIGIT_COMMIT_MS = 1_500L
        const val TOAST_MS = 2_500L
    }
}
