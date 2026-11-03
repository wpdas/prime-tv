package com.wpdas.primetv.ui.screens.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.wpdas.primetv.ui.navigation.SetupRoute
import androidx.lifecycle.viewModelScope
import com.wpdas.primetv.BuildConfig
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.PlaylistError
import com.wpdas.primetv.domain.model.PlaylistException
import com.wpdas.primetv.domain.model.SourceType
import com.wpdas.primetv.domain.repository.PlaylistRepository
import com.wpdas.primetv.domain.sync.CatalogSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    private val catalogSync: CatalogSync,
    private val updates: com.wpdas.primetv.domain.update.UpdateRepository,
) : ViewModel() {

    /** O diálogo de versão nova já foi aberto nesta visita à tela. */
    var updatePromptShown = false

    private val prefill: String? = runCatching { savedStateHandle.toRoute<SetupRoute>().prefill }.getOrNull()?.takeIf { it.isNotBlank() }

    private val _state = MutableStateFlow(SetupUiState(url = prefill ?: BuildConfig.IPTV_TEST_URL).withGuess())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    init {
        catalogSync.status.onEach { status -> _state.update { it.copy(sync = status) } }.launchIn(viewModelScope)
        // Link recebido pelo celular: conecta sem exigir mais um OK.
        if (prefill != null) connect()
        // Quem fica parado aqui (lista ainda não configurada) também recebe versão nova: a atualização
        // pode trazer justamente o recurso que destrava a configuração (ex.: receber o link pelo celular).
        updates.pending.onEach { u -> _state.update { it.copy(updateVersion = u?.versionName) } }.launchIn(viewModelScope)
        viewModelScope.launch {
            while (true) {
                updates.checkIfDue()
                kotlinx.coroutines.delay(UPDATE_RECHECK_MS)
            }
        }
    }

    fun onUrlChange(url: String) {
        _state.update { it.copy(url = url, error = null, detected = null, hasEpg = null).withGuess() }
    }

    fun connect() {
        val url = _state.value.url.trim()
        if (url.isEmpty() || _state.value.isDetecting) return
        _state.update { it.copy(isDetecting = true, error = null) }
        viewModelScope.launch {
            try {
                val result = playlistRepository.detect(url)
                playlistRepository.save(result.source)
                _state.update { it.copy(isDetecting = false, detected = result.source.type, hasEpg = result.hasEpg, connected = true) }
                catalogSync.syncAll()
            } catch (e: PlaylistException) {
                _state.update { it.copy(isDetecting = false, error = e.error) }
            } catch (e: Exception) {
                _state.update { it.copy(isDetecting = false, error = PlaylistError.Unknown(e.javaClass.simpleName)) }
            }
        }
    }

    /** "Tentar de novo" depois de um erro em Ao vivo. */
    fun retryLive() = catalogSync.sync(ContentSection.LIVE)

    private fun SetupUiState.withGuess(): SetupUiState = copy(guess = guessType(url))

    private companion object {
        const val UPDATE_RECHECK_MS = 30 * 60 * 1000L

        fun guessType(url: String): SourceType? {
            val lower = url.lowercase()
            if (lower.isBlank()) return null
            if (lower.contains("username=") && lower.contains("password=")) return SourceType.XTREAM
            if (lower.contains(".m3u") || lower.contains("get.php") || lower.contains("playlist")) return SourceType.M3U
            return null
        }
    }
}
