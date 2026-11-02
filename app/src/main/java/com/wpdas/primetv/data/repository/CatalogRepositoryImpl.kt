package com.wpdas.primetv.data.repository

import androidx.room.withTransaction
import com.wpdas.primetv.core.di.IoDispatcher
import com.wpdas.primetv.core.util.TextNormalizer
import com.wpdas.primetv.data.local.db.EpisodeEntity
import com.wpdas.primetv.data.local.db.PrimeDatabase
import com.wpdas.primetv.data.local.prefs.PlaylistStore
import com.wpdas.primetv.data.remote.xtream.XtreamClient
import com.wpdas.primetv.data.remote.xtream.blankToNull
import com.wpdas.primetv.data.remote.xtream.toIntLenient
import com.wpdas.primetv.data.sync.SearchType
import com.wpdas.primetv.domain.model.Category
import com.wpdas.primetv.domain.model.Channel
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.EpgProgram
import com.wpdas.primetv.domain.model.Episode
import com.wpdas.primetv.domain.model.Movie
import com.wpdas.primetv.domain.model.NowNext
import com.wpdas.primetv.domain.model.PlaylistSource
import com.wpdas.primetv.domain.model.Series
import com.wpdas.primetv.domain.repository.CatalogRepository
import com.wpdas.primetv.domain.repository.CategoryCount
import com.wpdas.primetv.domain.repository.ChannelFilter
import com.wpdas.primetv.domain.repository.SearchResults
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepositoryImpl @Inject constructor(
    private val db: PrimeDatabase,
    private val playlistStore: PlaylistStore,
    private val xtream: XtreamClient,
    @IoDispatcher private val io: CoroutineDispatcher,
) : CatalogRepository {

    private val dao get() = db.catalogDao()
    private val epg get() = db.epgDao()

    override fun categories(section: ContentSection): Flow<List<Category>> =
        dao.observeCategories(section.name).map { list -> list.map { it.toDomain() } }

    override fun categoryCounts(section: ContentSection): Flow<List<CategoryCount>> = when (section) {
        ContentSection.LIVE -> dao.observeChannelCounts()
        ContentSection.MOVIES -> dao.observeMovieCounts()
        ContentSection.SERIES -> dao.observeSeriesCounts()
        ContentSection.EPG -> flowOf(emptyList())
    }.map { list -> list.map { CategoryCount(it.key, it.count) } }

    override fun channels(filter: ChannelFilter): Flow<List<Channel>> = when (filter) {
        ChannelFilter.All -> dao.observeAllChannels().map { list -> list.map { it.toDomain() } }
        ChannelFilter.Football -> dao.observeFootballChannelsAll().map { list -> list.map { it.toDomain() } }
        is ChannelFilter.Category -> dao.observeCategoryChannels(filter.id).map { list -> list.map { it.toDomain() } }
        is ChannelFilter.Favorites -> channelsByIds(filter.ids)
    }

    override fun channel(id: String): Flow<Channel?> = dao.observeChannel(id).map { it?.toDomain() }
    override fun footballChannels(limit: Int): Flow<List<Channel>> = dao.observeFootballChannels(limit).map { l -> l.map { it.toDomain() } }
    override fun footballChannelCount(): Flow<Int> = dao.observeFootballChannelCount()
    override fun footballVodCount(): Flow<Int> = dao.observeFootballVodCount()

    override fun movies(categoryId: String?, limit: Int): Flow<List<Movie>> =
        (if (categoryId == null) dao.observeAllMovies(limit) else dao.observeCategoryMovies(categoryId, limit)).map { l -> l.map { it.toDomain() } }
    override fun recentMovies(limit: Int): Flow<List<Movie>> = dao.observeAllMovies(limit).map { l -> l.map { it.toDomain() } }
    override fun footballMovies(limit: Int): Flow<List<Movie>> = dao.observeFootballMovies(limit).map { l -> l.map { it.toDomain() } }
    override fun movie(id: String): Flow<Movie?> = dao.observeMovie(id).map { it?.toDomain() }

    override fun series(categoryId: String?, limit: Int): Flow<List<Series>> = dao.observeSeries(categoryId, limit).map { l -> l.map { it.toDomain() } }
    override fun footballSeries(limit: Int): Flow<List<Series>> = dao.observeFootballSeries(limit).map { l -> l.map { it.toDomain() } }
    override fun seriesById(id: String): Flow<Series?> = dao.observeSeriesById(id).map { it?.toDomain() }
    override fun episodes(seriesId: String): Flow<List<Episode>> = dao.observeEpisodes(seriesId).map { l -> l.map { it.toDomain() } }

    override suspend fun ensureEpisodes(seriesId: String) = withContext(io) {
        val series = dao.seriesById(seriesId) ?: return@withContext
        if (series.episodesLoaded) return@withContext
        val source = playlistStore.source.first() as? PlaylistSource.Xtream ?: return@withContext
        val remoteId = seriesId.removePrefix("xs-")
        val info = xtream.seriesInfo(source, remoteId)
        val episodes = xtream.parseEpisodes(info).mapNotNull { dto ->
            val id = dto.id.blankToNull() ?: return@mapNotNull null
            val season = dto.season.toIntLenient() ?: 1
            val number = dto.episodeNum.toIntLenient() ?: 0
            EpisodeEntity(
                id = "xe-$id",
                seriesId = seriesId,
                season = season,
                number = number,
                title = dto.title.blankToNull() ?: "Episódio $number",
                streamUrl = XtreamClient.episodeStreamUrl(source, id, dto.containerExtension.blankToNull()),
                containerExt = dto.containerExtension.blankToNull(),
                durationSec = dto.info?.durationSecs.toIntLenient() ?: parseDuration(dto.info?.duration),
                posterUrl = dto.info?.movieImage.blankToNull(),
                plot = dto.info?.plot.blankToNull(),
            )
        }
        db.withTransaction {
            dao.deleteEpisodesOf(seriesId)
            episodes.chunked(PrimeDatabase.BATCH_SIZE).forEach { dao.insertEpisodes(it) }
            dao.setEpisodesLoaded(seriesId, true)
        }
    }

    override suspend fun nowPlaying(filter: ChannelFilter, now: Long): Map<String, EpgProgram> = withContext(io) {
        val rows = when (filter) {
            ChannelFilter.All -> epg.nowPlaying(null, now)
            ChannelFilter.Football -> epg.nowPlayingFootball(now)
            is ChannelFilter.Category -> epg.nowPlaying(filter.id, now)
            is ChannelFilter.Favorites -> epg.nowPlaying(null, now)
        }
        // Guias com sobreposição: fica o programa que começou por último.
        val best = HashMap<String, EpgProgram>()
        for (row in rows) {
            val current = best[row.channelKey]
            if (current == null || row.start > current.start) best[row.channelKey] = row.toDomain()
        }
        best
    }

    override fun nowNext(channel: Channel, now: Long, upcoming: Int): Flow<NowNext> {
        val key = channel.epgKey ?: return flowOf(NowNext(null, emptyList()))
        return epg.observeUpcoming(key, now, upcoming + 1).map { list ->
            val programs = list.map { it.toDomain() }
            val current = programs.filter { it.start <= now && it.end > now }.maxByOrNull { it.start }
            NowNext(current, programs.filter { it.start > now }.sortedBy { it.start }.take(upcoming))
        }
    }

    override fun moviesByIds(ids: List<String>): Flow<List<Movie>> =
        if (ids.isEmpty()) flowOf(emptyList()) else dao.observeMoviesByIds(ids).map { l -> l.map { it.toDomain() }.sortedBy { ids.indexOf(it.id) } }
    override fun seriesByIds(ids: List<String>): Flow<List<Series>> =
        if (ids.isEmpty()) flowOf(emptyList()) else dao.observeSeriesByIds(ids).map { l -> l.map { it.toDomain() }.sortedBy { ids.indexOf(it.id) } }
    override fun channelsByIds(ids: List<String>): Flow<List<Channel>> =
        if (ids.isEmpty()) flowOf(emptyList()) else dao.observeChannelsByIds(ids).map { l -> l.map { it.toDomain() }.sortedBy { ids.indexOf(it.id) } }
    override suspend fun movieOnce(id: String): Movie? = withContext(io) { dao.movieById(id)?.toDomain() }
    override suspend fun seriesOnce(id: String): Series? = withContext(io) { dao.seriesById(id)?.toDomain() }
    override suspend fun channelOnce(id: String): Channel? = withContext(io) { dao.channelById(id)?.toDomain() }
    override suspend fun episodesOnce(seriesId: String): List<Episode> = withContext(io) { dao.episodesOf(seriesId).map { it.toDomain() } }
    override suspend fun episodeOnce(id: String): Episode? = withContext(io) { dao.episodeById(id)?.toDomain() }
    override suspend fun channelsOnce(filter: ChannelFilter): List<Channel> = withContext(io) {
        when (filter) {
            ChannelFilter.All -> dao.allChannelsList()
            ChannelFilter.Football -> dao.footballChannelsList()
            is ChannelFilter.Category -> dao.categoryChannelsList(filter.id)
            is ChannelFilter.Favorites -> dao.channelsByIds(filter.ids).sortedBy { filter.ids.indexOf(it.id) }
        }.map { it.toDomain() }
    }

    override suspend fun channelVariants(groupKey: String): List<Channel> = withContext(io) { dao.channelVariants(groupKey).map { it.toDomain() } }

    override suspend fun search(query: String, limit: Int): SearchResults = withContext(io) {
        val match = buildMatchQuery(query) ?: return@withContext SearchResults(emptyList(), emptyList(), emptyList())
        val hits = dao.search(match, limit)
        val byType = hits.groupBy({ it.type }, { it.refId })
        val channels = byType[SearchType.CHANNEL]?.let { dao.channelsByIds(it) }.orEmpty().map { it.toDomain() }
        val movies = byType[SearchType.MOVIE]?.let { dao.moviesByIds(it) }.orEmpty().map { it.toDomain() }
        val series = byType[SearchType.SERIES]?.let { dao.seriesByIds(it) }.orEmpty().map { it.toDomain() }
        // Mantém a ordem de relevância do FTS (ordem do índice) dentro de cada tipo.
        val order = hits.withIndex().associate { it.value.refId to it.index }
        // Mesma regra das listas: só colapsa grupos que têm alguma variante com tag de qualidade.
        val bestChannels = channels.groupBy { it.groupKey.ifEmpty { it.id } }.values.flatMap { group ->
            if (group.any { it.qualityRank < com.wpdas.primetv.core.util.Quality.RANK_UNKNOWN }) listOf(group.minByOrNull { it.qualityRank }!!) else group
        }
        SearchResults(
            channels = bestChannels.sortedBy { order[it.id] },
            movies = movies.sortedBy { order[it.id] },
            series = series.sortedBy { order[it.id] },
        )
    }

    companion object {
        /** "Glob sp" -> "glob* sp*" (cada termo por prefixo). null se não sobrar nada pesquisável. */
        fun buildMatchQuery(query: String): String? {
            val tokens = TextNormalizer.searchKey(query).split(' ').filter { it.isNotBlank() }
            if (tokens.isEmpty()) return null
            return tokens.joinToString(" ") { "$it*" }
        }

        /** "00:45:00" -> 2700. */
        fun parseDuration(text: String?): Int? {
            val parts = text?.trim()?.split(':')?.mapNotNull { it.toIntOrNull() } ?: return null
            return when (parts.size) {
                3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                2 -> parts[0] * 60 + parts[1]
                else -> null
            }
        }
    }
}
