package com.wpdas.primetv.ui.screens.setup

import androidx.lifecycle.ViewModel
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
    private val playlistRepository: PlaylistRepository,
    private val catalogSync: CatalogSync,
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState(url = BuildConfig.IPTV_TEST_URL).withGuess())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    init {
        catalogSync.status.onEach { status -> _state.update { it.copy(sync = status) } }.launchIn(viewModelScope)
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
        fun guessType(url: String): SourceType? {
            val lower = url.lowercase()
            if (lower.isBlank()) return null
            if (lower.contains("username=") && lower.contains("password=")) return SourceType.XTREAM
            if (lower.contains(".m3u") || lower.contains("get.php") || lower.contains("playlist")) return SourceType.M3U
            return null
        }
    }
}
