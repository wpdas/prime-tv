package com.wpdas.primetv.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wpdas.primetv.core.di.ApplicationScope
import com.wpdas.primetv.domain.football.FootballClassifier
import com.wpdas.primetv.domain.model.AppSettings
import com.wpdas.primetv.domain.model.BufferProfile
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.SyncInterval
import com.wpdas.primetv.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope scope: CoroutineScope,
) : SettingsRepository {

    private val store get() = context.appDataStore

    override val settings: StateFlow<AppSettings> = store.data
        .map { prefs ->
            AppSettings(
                syncIntervals = ContentSection.entries.associateWith { section ->
                    prefs[intervalKey(section)]?.let { v -> SyncInterval.entries.firstOrNull { it.name == v } } ?: SyncInterval.defaultFor(section)
                },
                unmeteredOnly = prefs[KEY_UNMETERED] ?: false,
                footballKeywords = prefs[KEY_FOOTBALL]?.split('\n')?.filter { it.isNotBlank() } ?: FootballClassifier.DEFAULT_KEYWORDS,
                preferSoftwareDecoder = prefs[KEY_SOFTWARE_DECODER] ?: false,
                bufferProfile = prefs[KEY_BUFFER]?.let { v -> BufferProfile.entries.firstOrNull { it.name == v } } ?: BufferProfile.NORMAL,
                language = prefs[KEY_LANGUAGE] ?: "pt-BR",
                overscanPercent = prefs[KEY_OVERSCAN] ?: 0,
                previewSound = prefs[KEY_PREVIEW_SOUND] ?: true,
                resumeLive = prefs[KEY_RESUME_LIVE] ?: true,
                matchDisplayMode = prefs[KEY_MATCH_DISPLAY_MODE] ?: false,
            )
        }
        // Primeiro valor lido de forma síncrona: arquivo minúsculo, evita "piscar" com padrões errados.
        .stateIn(scope, SharingStarted.Eagerly, runBlocking { readOnce() })

    private suspend fun readOnce(): AppSettings = runCatching {
        val prefs = store.data.first()
        AppSettings(
            syncIntervals = ContentSection.entries.associateWith { section ->
                prefs[intervalKey(section)]?.let { v -> SyncInterval.entries.firstOrNull { it.name == v } } ?: SyncInterval.defaultFor(section)
            },
            unmeteredOnly = prefs[KEY_UNMETERED] ?: false,
            footballKeywords = prefs[KEY_FOOTBALL]?.split('\n')?.filter { it.isNotBlank() } ?: FootballClassifier.DEFAULT_KEYWORDS,
            preferSoftwareDecoder = prefs[KEY_SOFTWARE_DECODER] ?: false,
            bufferProfile = prefs[KEY_BUFFER]?.let { v -> BufferProfile.entries.firstOrNull { it.name == v } } ?: BufferProfile.NORMAL,
            language = prefs[KEY_LANGUAGE] ?: "pt-BR",
            overscanPercent = prefs[KEY_OVERSCAN] ?: 0,
            previewSound = prefs[KEY_PREVIEW_SOUND] ?: true,
            resumeLive = prefs[KEY_RESUME_LIVE] ?: true,
            matchDisplayMode = prefs[KEY_MATCH_DISPLAY_MODE] ?: false,
        )
    }.getOrDefault(AppSettings(footballKeywords = FootballClassifier.DEFAULT_KEYWORDS))

    override suspend fun setSyncInterval(section: ContentSection, interval: SyncInterval) {
        store.edit { it[intervalKey(section)] = interval.name }
    }

    override suspend fun setUnmeteredOnly(enabled: Boolean) {
        store.edit { it[KEY_UNMETERED] = enabled }
    }

    override suspend fun setFootballKeywords(keywords: List<String>) {
        store.edit { it[KEY_FOOTBALL] = keywords.map { k -> k.trim() }.filter { k -> k.isNotEmpty() }.distinct().joinToString("\n") }
    }

    override suspend fun setPreferSoftwareDecoder(enabled: Boolean) {
        store.edit { it[KEY_SOFTWARE_DECODER] = enabled }
    }

    override suspend fun setBufferProfile(profile: BufferProfile) {
        store.edit { it[KEY_BUFFER] = profile.name }
    }

    override suspend fun setLanguage(language: String) {
        store.edit { it[KEY_LANGUAGE] = language }
    }

    override suspend fun setOverscanPercent(percent: Int) {
        store.edit { it[KEY_OVERSCAN] = percent.coerceIn(0, 10) }
    }

    override suspend fun setPreviewSound(enabled: Boolean) {
        store.edit { it[KEY_PREVIEW_SOUND] = enabled }
    }

    override suspend fun setMatchDisplayMode(enabled: Boolean) {
        store.edit { it[KEY_MATCH_DISPLAY_MODE] = enabled }
    }

    override suspend fun setResumeLive(enabled: Boolean) {
        store.edit { it[KEY_RESUME_LIVE] = enabled }
    }

    private companion object {
        fun intervalKey(section: ContentSection) = stringPreferencesKey("sync_interval_${section.name.lowercase()}")
        val KEY_UNMETERED = booleanPreferencesKey("sync_unmetered_only")
        val KEY_FOOTBALL = stringPreferencesKey("football_keywords")
        val KEY_SOFTWARE_DECODER = booleanPreferencesKey("player_software_decoder")
        val KEY_BUFFER = stringPreferencesKey("player_buffer")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_OVERSCAN = androidx.datastore.preferences.core.intPreferencesKey("overscan_percent")
        val KEY_PREVIEW_SOUND = booleanPreferencesKey("preview_sound")
        val KEY_RESUME_LIVE = booleanPreferencesKey("player_resume_live")
        val KEY_MATCH_DISPLAY_MODE = booleanPreferencesKey("player_match_display_mode")
    }
}
