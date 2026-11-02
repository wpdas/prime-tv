package com.wpdas.primetv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpdas.primetv.core.i18n.I18n
import com.wpdas.primetv.domain.model.AppSettings
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.PlaylistSource
import com.wpdas.primetv.domain.model.SyncState
import com.wpdas.primetv.domain.repository.CatalogRepository
import com.wpdas.primetv.domain.repository.PlaylistRepository
import com.wpdas.primetv.domain.repository.SettingsRepository
import com.wpdas.primetv.domain.sync.CatalogSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SettingsSection(val labelKey: String) {
    PLAYLIST("settings.playlist"),
    UPDATES("settings.updates"),
    FOOTBALL("settings.football"),
    PLAYER("settings.player"),
    LANGUAGE("settings.language"),
    ABOUT("settings.about"),
}

data class SettingsUiState(
    val section: SettingsSection = SettingsSection.PLAYLIST,
    val source: PlaylistSource? = null,
    val settings: AppSettings = AppSettings(),
    val sync: Map<ContentSection, SyncState> = emptyMap(),
    val footballChannels: Int = 0,
    val footballVod: Int = 0,
    val languages: List<String> = emptyList(),
    val addingKeyword: Boolean = false,
    val keywordInput: String = "",
) {
    val isSyncing: Boolean get() = sync.values.any { it is SyncState.Running }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val playlistRepository: PlaylistRepository,
    private val catalogSync: CatalogSync,
    private val i18n: I18n,
    catalog: CatalogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(settings = settingsRepository.settings.value, languages = i18n.availableLanguages()))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        settingsRepository.settings.onEach { s -> _state.update { it.copy(settings = s) } }.launchIn(viewModelScope)
        playlistRepository.source.onEach { src -> _state.update { it.copy(source = src) } }.launchIn(viewModelScope)
        catalogSync.status.onEach { st -> _state.update { it.copy(sync = st) } }.launchIn(viewModelScope)
        combine(catalog.footballChannelCount(), catalog.footballVodCount()) { c, v -> c to v }
            .onEach { (c, v) -> _state.update { it.copy(footballChannels = c, footballVod = v) } }
            .launchIn(viewModelScope)
    }

    fun select(section: SettingsSection) = _state.update { it.copy(section = section) }

    fun cycleInterval(section: ContentSection) {
        val next = _state.value.settings.interval(section).next()
        viewModelScope.launch { settingsRepository.setSyncInterval(section, next) }
    }

    fun toggleUnmetered() {
        viewModelScope.launch { settingsRepository.setUnmeteredOnly(!_state.value.settings.unmeteredOnly) }
    }

    fun syncAllNow() = catalogSync.syncAll()

    fun removeKeyword(keyword: String) {
        viewModelScope.launch { settingsRepository.setFootballKeywords(_state.value.settings.footballKeywords - keyword) }
    }

    fun startAddKeyword() = _state.update { it.copy(addingKeyword = true, keywordInput = "") }
    fun cancelAddKeyword() = _state.update { it.copy(addingKeyword = false, keywordInput = "") }
    fun onKeywordInput(text: String) = _state.update { it.copy(keywordInput = text) }

    fun confirmAddKeyword() {
        val word = _state.value.keywordInput.trim().lowercase()
        if (word.isEmpty()) return
        viewModelScope.launch {
            settingsRepository.setFootballKeywords(_state.value.settings.footballKeywords + word)
            _state.update { it.copy(addingKeyword = false, keywordInput = "") }
        }
    }

    fun toggleSoftwareDecoder() {
        viewModelScope.launch { settingsRepository.setPreferSoftwareDecoder(!_state.value.settings.preferSoftwareDecoder) }
    }

    fun cycleBuffer() {
        viewModelScope.launch { settingsRepository.setBufferProfile(_state.value.settings.bufferProfile.next()) }
    }

    fun cycleOverscan() {
        val options = AppSettings.OVERSCAN_OPTIONS
        val next = options[(options.indexOf(_state.value.settings.overscanPercent).coerceAtLeast(0) + 1) % options.size]
        viewModelScope.launch { settingsRepository.setOverscanPercent(next) }
    }

    fun togglePreviewSound() {
        viewModelScope.launch { settingsRepository.setPreviewSound(!_state.value.settings.previewSound) }
    }

    fun selectLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
            i18n.load(language)
        }
    }
}
