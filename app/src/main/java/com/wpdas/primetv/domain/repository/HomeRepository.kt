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
    /** Abas da barra superior na ordem: Kids e Anime só quando a lista tiver categorias correspondentes. */
    fun availableTabs(): Flow<List<HomeTab>>
    /** Nome da seção sendo atualizada em segundo plano, ou null. */
    fun backgroundUpdate(): Flow<String?>
    suspend fun toggleMyList(itemId: String)
    /** Versão nova encontrada pela checagem diária, ou null. */
    fun pendingUpdateVersion(): Flow<String?>
}
