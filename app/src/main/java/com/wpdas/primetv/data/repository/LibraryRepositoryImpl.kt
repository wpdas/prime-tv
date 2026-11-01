package com.wpdas.primetv.data.repository

import com.wpdas.primetv.core.di.IoDispatcher
import com.wpdas.primetv.data.local.db.FavoriteEntity
import com.wpdas.primetv.data.local.db.PrimeDatabase
import com.wpdas.primetv.data.local.db.WatchHistoryEntity
import com.wpdas.primetv.domain.model.Channel
import com.wpdas.primetv.domain.model.ContinueWatchingItem
import com.wpdas.primetv.domain.model.Episode
import com.wpdas.primetv.domain.model.MediaType
import com.wpdas.primetv.domain.model.ResumeLogic
import com.wpdas.primetv.domain.model.SeriesResume
import com.wpdas.primetv.domain.model.WatchProgress
import com.wpdas.primetv.domain.repository.LibraryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val db: PrimeDatabase,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LibraryRepository {

    private val dao get() = db.libraryDao()
    private val catalog get() = db.catalogDao()

    override fun favoriteIds(): Flow<Set<String>> = dao.observeFavorites().map { list -> list.mapTo(HashSet()) { it.itemId } }

    override fun favoriteIds(type: MediaType): Flow<List<String>> =
        dao.observeFavorites().map { list -> list.filter { it.type == type.key }.map { it.itemId } }

    override suspend fun isFavorite(itemId: String): Boolean = dao.isFavorite(itemId)

    override suspend fun toggleFavorite(itemId: String, type: MediaType): Boolean = withContext(io) {
        if (dao.isFavorite(itemId)) {
            dao.deleteFavorite(itemId)
            false
        } else {
            dao.insertFavorite(FavoriteEntity(itemId, type.key, System.currentTimeMillis()))
            true
        }
    }

    override suspend fun progress(itemId: String): WatchProgress? = dao.history(itemId)?.toDomain()

    override fun seriesProgress(seriesId: String): Flow<Map<String, WatchProgress>> =
        dao.observeSeriesHistory(seriesId).map { list -> list.associate { it.itemId to it.toDomain() } }

    override suspend fun savePosition(itemId: String, type: MediaType, seriesId: String?, positionMs: Long, durationMs: Long) {
        val finished = ResumeLogic.isFinished(positionMs, durationMs)
        dao.upsertHistory(WatchHistoryEntity(itemId, type.key, seriesId, positionMs, durationMs, System.currentTimeMillis(), finished))
    }

    override suspend fun markFinished(itemId: String, type: MediaType, seriesId: String?, durationMs: Long) {
        dao.upsertHistory(WatchHistoryEntity(itemId, type.key, seriesId, durationMs, durationMs, System.currentTimeMillis(), finished = true))
    }

    override suspend fun recordChannel(channelId: String) {
        dao.upsertHistory(WatchHistoryEntity(channelId, MediaType.CHANNEL.key, null, 0, 0, System.currentTimeMillis(), finished = false))
    }

    override suspend fun resumePoint(seriesId: String, episodes: List<Episode>): SeriesResume? = withContext(io) {
        val latest = dao.latestOfSeries(seriesId) ?: return@withContext null
        ResumeLogic.resume(latest.toDomain(), episodes)
    }

    /**
     * Últimos itens em andamento: filmes com posição salva e, por série, o episódio onde
     * retomar. Séries terminadas (último episódio concluído) somem da linha.
     */
    override fun continueWatching(limit: Int): Flow<List<ContinueWatchingItem>> =
        dao.observeVodHistory(limit * 3).map { rows ->
            val result = ArrayList<ContinueWatchingItem>()
            val seenSeries = HashSet<String>()
            for (row in rows) {
                if (result.size >= limit) break
                when (row.type) {
                    MediaType.MOVIE.key -> {
                        if (row.finished || row.positionMs <= 0) continue
                        val movie = catalog.movieById(row.itemId) ?: continue
                        result += ContinueWatchingItem(
                            item = movie.toDomain().toMediaItem(),
                            episodeLabel = null,
                            remainingMinutes = remainingMinutes(row),
                            progress = row.toDomain().fraction,
                        )
                    }
                    MediaType.SERIES.key -> {
                        val seriesId = row.seriesId ?: continue
                        if (!seenSeries.add(seriesId)) continue
                        val series = catalog.seriesById(seriesId) ?: continue
                        val episodes = catalog.episodesOf(seriesId).map { it.toDomain() }
                        if (episodes.none { it.id == row.itemId }) continue
                        val resume = ResumeLogic.resume(row.toDomain(), episodes) ?: continue
                        val durationMs = if (resume.episode.id == row.itemId) row.durationMs else (resume.episode.durationSec ?: 0) * 1000L
                        val remaining = if (durationMs > 0) ((durationMs - resume.positionMs) / 60_000L).toInt().coerceAtLeast(1) else 0
                        result += ContinueWatchingItem(
                            item = series.toDomain().toMediaItem(),
                            episodeLabel = "T${resume.episode.season}:E${resume.episode.number}",
                            remainingMinutes = remaining,
                            progress = if (durationMs > 0) (resume.positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                            episodeId = resume.episode.id,
                        )
                    }
                }
            }
            result.toList()
        }.flowOn(io)

    override fun recentChannels(limit: Int): Flow<List<Channel>> =
        dao.observeRecentChannels(limit).map { rows ->
            val byId = catalog.channelsByIds(rows.map { it.itemId }).associateBy { it.id }
            rows.mapNotNull { byId[it.itemId]?.toDomain() }
        }.flowOn(io)

    private fun remainingMinutes(row: WatchHistoryEntity): Int =
        if (row.durationMs > 0) ((row.durationMs - row.positionMs) / 60_000L).toInt().coerceAtLeast(1) else 0

    private fun WatchHistoryEntity.toDomain() = WatchProgress(
        itemId = itemId,
        type = MediaType.entries.firstOrNull { it.key == type } ?: MediaType.MOVIE,
        seriesId = seriesId,
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = updatedAt,
        finished = finished,
    )
}
