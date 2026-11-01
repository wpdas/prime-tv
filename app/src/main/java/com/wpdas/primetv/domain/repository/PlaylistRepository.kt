package com.wpdas.primetv.domain.repository

import com.wpdas.primetv.domain.model.DetectionResult
import com.wpdas.primetv.domain.model.PlaylistSource
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    /** Lista configurada, ou null antes da configuração inicial. */
    val source: Flow<PlaylistSource?>

    /** Descobre se a URL é Xtream ou M3U, validando no servidor. Lança [com.wpdas.primetv.domain.model.PlaylistException]. */
    suspend fun detect(url: String): DetectionResult

    suspend fun save(source: PlaylistSource)
    suspend fun clear()
}
