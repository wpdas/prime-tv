package com.wpdas.primetv.domain.repository

import com.wpdas.primetv.domain.model.Channel
import com.wpdas.primetv.domain.model.ContinueWatchingItem
import com.wpdas.primetv.domain.model.Episode
import com.wpdas.primetv.domain.model.MediaType
import com.wpdas.primetv.domain.model.SeriesResume
import com.wpdas.primetv.domain.model.WatchProgress
import kotlinx.coroutines.flow.Flow

/** Favoritos e histórico do usuário, persistidos localmente. */
interface LibraryRepository {
    fun favoriteIds(): Flow<Set<String>>
    fun favoriteIds(type: MediaType): Flow<List<String>>
    suspend fun isFavorite(itemId: String): Boolean
    /** Devolve o novo estado (true = favoritado). */
    suspend fun toggleFavorite(itemId: String, type: MediaType): Boolean

    suspend fun progress(itemId: String): WatchProgress?
    fun seriesProgress(seriesId: String): Flow<Map<String, WatchProgress>>
    suspend fun savePosition(itemId: String, type: MediaType, seriesId: String?, positionMs: Long, durationMs: Long)
    suspend fun markFinished(itemId: String, type: MediaType, seriesId: String?, durationMs: Long)
    suspend fun recordChannel(channelId: String)

    /** Episódio onde retomar a série: o último em andamento, ou o seguinte ao último concluído. */
    suspend fun resumePoint(seriesId: String, episodes: List<Episode>): SeriesResume?
    fun continueWatching(limit: Int): Flow<List<ContinueWatchingItem>>
    fun recentChannels(limit: Int): Flow<List<Channel>>
}
