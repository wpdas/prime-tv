package com.wpdas.primetv.ui.screens.setup

import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.PlaylistError
import com.wpdas.primetv.domain.model.SourceType
import com.wpdas.primetv.domain.model.SyncState

data class SetupUiState(
    val url: String = "",
    /** Palpite só pelo formato da URL, antes de validar no servidor. */
    val guess: SourceType? = null,
    /** Tipo confirmado pelo servidor. */
    val detected: SourceType? = null,
    val hasEpg: Boolean? = null,
    val isDetecting: Boolean = false,
    val error: PlaylistError? = null,
    /** Lista validada e salva; o painel de progresso aparece. */
    val connected: Boolean = false,
    val sync: Map<ContentSection, SyncState> = emptyMap(),
    /** Versão nova publicada no GitHub, se houver. */
    val updateVersion: String? = null,
) {
    val canConnect: Boolean get() = url.isNotBlank() && !isDetecting
    val canStart: Boolean get() = connected && sync[ContentSection.LIVE].let { it is SyncState.Done || it is SyncState.Skipped }
    val liveFailed: Boolean get() = sync[ContentSection.LIVE] is SyncState.Error
}
