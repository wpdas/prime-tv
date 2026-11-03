package com.wpdas.primetv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpdas.primetv.data.local.prefs.LiveStateStore
import com.wpdas.primetv.domain.repository.CatalogRepository
import com.wpdas.primetv.domain.repository.PlaylistRepository
import com.wpdas.primetv.domain.repository.SettingsRepository
import com.wpdas.primetv.domain.sync.CatalogSync
import com.wpdas.primetv.ui.navigation.HomeRoute
import com.wpdas.primetv.ui.navigation.SetupRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Decide a tela inicial: Configuração inicial sem lista salva, Início com lista. */
@HiltViewModel
class MainViewModel @Inject constructor(
    playlistRepository: PlaylistRepository,
    catalogSync: CatalogSync,
    updates: com.wpdas.primetv.domain.update.UpdateRepository,
    settings: SettingsRepository,
    liveState: LiveStateStore,
    catalog: CatalogRepository,
) : ViewModel() {
    /** Tela inicial e, se o app foi fechado assistindo a um canal ao vivo, o canal a reabrir em tela cheia. */
    data class Start(val route: Any, val resumeChannelId: String?)

    /** Vinheta de abertura: uma vez por processo (não repete ao girar a tela ou voltar do player). */
    var showIntro by androidx.compose.runtime.mutableStateOf(true)

    /** A retomada do canal já foi disparada neste processo (não repete ao recompor). */
    var resumeConsumed by androidx.compose.runtime.mutableStateOf(false)

    init {
        // Checagem silenciosa de versão nova ao abrir (no máximo a cada 30 min); a Início abre o diálogo se houver.
        viewModelScope.launch { updates.checkIfDue() }
    }

    val start: StateFlow<Start?> = playlistRepository.source
        .take(1)
        .onEach { source -> if (source != null) catalogSync.syncStale() }
        .map<Any?, Start?> { source ->
            if (source == null) return@map Start(SetupRoute(), null)
            // Canal "em exibição" quando o app foi fechado, se a opção estiver ligada e ele ainda existir na lista.
            val resume = if (settings.settings.value.resumeLive) {
                liveState.readWatching()?.takeIf { id -> runCatching { catalog.channelOnce(id) }.getOrNull() != null }
            } else null
            Start(HomeRoute(), resume)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
