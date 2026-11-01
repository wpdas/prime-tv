package com.wpdas.primetv.domain.model

/** Estado da sincronização de uma seção. */
sealed interface SyncState {
    data object Idle : SyncState
    data class Running(val count: Int) : SyncState
    data class Done(val count: Int, val finishedAt: Long) : SyncState
    /** Seção não disponível nesta lista/versão (ex.: lista M3U sem guia). */
    data class Skipped(val reasonKey: String) : SyncState
    data class Error(val error: PlaylistError, val at: Long) : SyncState
}
