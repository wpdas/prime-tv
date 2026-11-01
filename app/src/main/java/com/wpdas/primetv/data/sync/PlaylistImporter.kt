package com.wpdas.primetv.data.sync

import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.PlaylistSource

sealed interface ImportResult {
    data class Imported(val count: Int) : ImportResult
    data class Skipped(val reasonKey: String) : ImportResult
}

/** Importa uma seção da lista para o Room. Implementações por tipo de lista. */
interface PlaylistImporter {
    suspend fun import(source: PlaylistSource, section: ContentSection, onProgress: (Int) -> Unit): ImportResult
}

internal object SearchType {
    const val CHANNEL = "channel"
    const val MOVIE = "movie"
    const val SERIES = "series"
}
