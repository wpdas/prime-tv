package com.wpdas.primetv.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpdas.primetv.domain.model.HomeTab
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.repository.HomeRepository
import com.wpdas.primetv.ui.navigation.HomeRoute
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HomeRepository,
) : ViewModel() {

    private val initialTab: HomeTab = runCatching { savedStateHandle.toRoute<HomeRoute>().tab }
        .getOrNull()?.let { name -> HomeTab.entries.firstOrNull { it.name == name } } ?: HomeTab.HOME

    private val _state = MutableStateFlow(HomeUiState(selectedTab = initialTab))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val tab = MutableStateFlow(initialTab)
    private val focusedCard = MutableStateFlow<MediaItem?>(null)

    init {
        repository.featured().onEach { list ->
            _state.update { it.copy(featured = list, featuredIndex = it.featuredIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))) }
        }.launchIn(viewModelScope)
        repository.continueWatching().onEach { list -> _state.update { it.copy(continueWatching = list) } }.launchIn(viewModelScope)
        repository.footballNow().onEach { list -> _state.update { it.copy(football = list) } }.launchIn(viewModelScope)
        tab.flatMapLatest { repository.categoryRows(it) }
            .onEach { rows -> _state.update { it.copy(rows = rows) } }
            .launchIn(viewModelScope)
        repository.backgroundUpdate().onEach { key -> _state.update { it.copy(updatingSectionKey = key) } }.launchIn(viewModelScope)
        // Aviso de versão nova por alguns segundos, uma vez por abertura da Início.
        // A checagem silenciosa pode terminar depois da Início abrir: observa continuamente.
        repository.pendingUpdateVersion().onEach { version -> _state.update { it.copy(updateVersion = version) } }.launchIn(viewModelScope)

        // Ao focar um card, o destaque troca para ele com debounce de ~400 ms.
        focusedCard.debounce(HERO_FOCUS_DEBOUNCE_MS)
            .onEach { item -> _state.update { it.copy(pinnedHero = item) } }
            .launchIn(viewModelScope)

        // Rotação automática do destaque enquanto nenhum card está fixado.
        viewModelScope.launch {
            while (true) {
                delay(HERO_ROTATION_MS)
                _state.update { s ->
                    if (s.pinnedHero != null || s.featured.size < 2) s
                    else s.copy(featuredIndex = (s.featuredIndex + 1) % s.featured.size)
                }
            }
        }
    }

    fun onTabSelected(newTab: HomeTab) {
        tab.value = newTab
        focusedCard.value = null
        _state.update { it.copy(selectedTab = newTab, pinnedHero = null) }
    }

    /** Chamado pelos cards quando ganham foco. */
    fun onCardFocused(item: MediaItem) {
        focusedCard.value = item
        lastFocusedItemId = item.id
    }

    /** Chamado quando o foco volta para a barra superior: solta o destaque e retoma a rotação. */
    fun onTopBarFocused() {
        focusedCard.value = null
    }

    /** Linha que estava com foco ao sair da Início (para voltar exatamente lá). */
    var lastFocusedRow: String? = null
    var lastFocusedItemId: String? = null
    var initialFocusDone: Boolean = false
    /** O diálogo de atualização já foi aberto automaticamente nesta sessão. */
    var updatePromptShown: Boolean = false

    fun toggleMyList(item: MediaItem) {
        viewModelScope.launch { repository.toggleMyList(item.id) }
        _state.update { s -> s.copy(pinnedHero = s.pinnedHero?.takeIf { it.id == item.id }?.copy(inMyList = !item.inMyList) ?: s.pinnedHero) }
    }

    private companion object {
        const val HERO_FOCUS_DEBOUNCE_MS = 400L
        const val HERO_ROTATION_MS = 8_000L
    }
}
