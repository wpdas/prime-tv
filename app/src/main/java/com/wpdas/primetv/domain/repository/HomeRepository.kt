package com.wpdas.primetv.domain.repository

import com.wpdas.primetv.domain.model.CategoryRow
import com.wpdas.primetv.domain.model.ContinueWatchingItem
import com.wpdas.primetv.domain.model.FootballEvent
import com.wpdas.primetv.domain.model.HomeTab
import com.wpdas.primetv.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

/** Dados da tela de Início. A implementação real (Room + TMDB) chega nas fases 3 e 5. */
interface HomeRepository {
    fun featured(): Flow<List<MediaItem>>
    fun continueWatching(): Flow<List<ContinueWatchingItem>>
    fun footballNow(): Flow<List<FootballEvent>>
    fun categoryRows(tab: HomeTab): Flow<List<CategoryRow>>
    /** Nome da seção sendo atualizada em segundo plano, ou null. */
    fun backgroundUpdate(): Flow<String?>
    suspend fun toggleMyList(itemId: String)
}
