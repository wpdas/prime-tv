package com.wpdas.primetv.domain.repository

import com.wpdas.primetv.domain.model.AppSettings
import com.wpdas.primetv.domain.model.BufferProfile
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.SyncInterval
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    /** Sempre com o valor atual (carregado no arranque), para leituras síncronas como no player. */
    val settings: StateFlow<AppSettings>

    suspend fun setSyncInterval(section: ContentSection, interval: SyncInterval)
    suspend fun setUnmeteredOnly(enabled: Boolean)
    suspend fun setFootballKeywords(keywords: List<String>)
    suspend fun setPreferSoftwareDecoder(enabled: Boolean)
    suspend fun setBufferProfile(profile: BufferProfile)
    suspend fun setLanguage(language: String)
    suspend fun setOverscanPercent(percent: Int)
    suspend fun setPreviewSound(enabled: Boolean)
}
