package com.wpdas.primetv.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class KeyCount(val key: String?, val count: Int)
data class SearchHit(val refId: String, val type: String)
data class ChannelEpgRef(val id: String, val name: String, val epgChannelId: String?)
data class TitleRef(val id: String, val name: String, val categoryName: String?)

@Dao
interface CatalogDao {
    // --- categorias ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCategories(list: List<CategoryEntity>)
    @Query("DELETE FROM categories WHERE section = :section") suspend fun deleteCategories(section: String)
    @Query("SELECT * FROM categories WHERE section = :section ORDER BY sortOrder") fun observeCategories(section: String): Flow<List<CategoryEntity>>

    // --- canais ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertChannels(list: List<ChannelEntity>)
    @Query("DELETE FROM channels") suspend fun deleteChannels()
    @Query("SELECT COUNT(*) FROM channels") fun observeChannelCount(): Flow<Int>
    // Variantes de qualidade (UHD/FHD/HD/SD do mesmo canal): só a melhor aparece. A regra vale
    // dentro do escopo consultado (Todos, uma categoria, futebol), e grupos sem nenhuma tag de
    // qualidade não são tocados (todos os itens passam).
    @Query("SELECT * FROM channels c WHERE (c.id IN (SELECT c2.id FROM channels c2 WHERE c2.groupKey = c.groupKey ORDER BY c2.qualityRank, c2.number LIMIT 1) OR NOT EXISTS (SELECT 1 FROM channels c3 WHERE c3.groupKey = c.groupKey AND c3.qualityRank < 4)) ORDER BY number") fun observeAllChannels(): Flow<List<ChannelEntity>>
    @Query("SELECT * FROM channels c WHERE c.categoryId = :categoryId AND (c.id IN (SELECT c2.id FROM channels c2 WHERE c2.groupKey = c.groupKey AND c2.categoryId = c.categoryId ORDER BY c2.qualityRank, c2.number LIMIT 1) OR NOT EXISTS (SELECT 1 FROM channels c3 WHERE c3.groupKey = c.groupKey AND c3.categoryId = c.categoryId AND c3.qualityRank < 4)) ORDER BY number") fun observeCategoryChannels(categoryId: String): Flow<List<ChannelEntity>>
    @Query("SELECT * FROM channels c WHERE c.isFootball = 1 AND (c.id IN (SELECT c2.id FROM channels c2 WHERE c2.groupKey = c.groupKey AND c2.isFootball = 1 ORDER BY c2.qualityRank, c2.number LIMIT 1) OR NOT EXISTS (SELECT 1 FROM channels c3 WHERE c3.groupKey = c.groupKey AND c3.isFootball = 1 AND c3.qualityRank < 4)) ORDER BY number") fun observeFootballChannelsAll(): Flow<List<ChannelEntity>>
    @Query("SELECT * FROM channels c WHERE c.isFootball = 1 AND (c.id IN (SELECT c2.id FROM channels c2 WHERE c2.groupKey = c.groupKey AND c2.isFootball = 1 ORDER BY c2.qualityRank, c2.number LIMIT 1) OR NOT EXISTS (SELECT 1 FROM channels c3 WHERE c3.groupKey = c.groupKey AND c3.isFootball = 1 AND c3.qualityRank < 4)) ORDER BY number LIMIT :limit") fun observeFootballChannels(limit: Int): Flow<List<ChannelEntity>>
    @Query("SELECT COUNT(*) FROM channels c WHERE c.isFootball = 1 AND (c.id IN (SELECT c2.id FROM channels c2 WHERE c2.groupKey = c.groupKey AND c2.isFootball = 1 ORDER BY c2.qualityRank, c2.number LIMIT 1) OR NOT EXISTS (SELECT 1 FROM channels c3 WHERE c3.groupKey = c.groupKey AND c3.isFootball = 1 AND c3.qualityRank < 4))") fun observeFootballChannelCount(): Flow<Int>
    @Query("SELECT * FROM channels WHERE groupKey = :groupKey ORDER BY qualityRank, number") suspend fun channelVariants(groupKey: String): List<ChannelEntity>
    @Query("SELECT (SELECT COUNT(*) FROM movies WHERE isFootball = 1) + (SELECT COUNT(*) FROM series WHERE isFootball = 1)") fun observeFootballVodCount(): Flow<Int>
    @Query("SELECT * FROM channels WHERE id = :id") fun observeChannel(id: String): Flow<ChannelEntity?>
    @Query("SELECT * FROM channels WHERE id = :id") suspend fun channelById(id: String): ChannelEntity?
    @Query("SELECT * FROM channels WHERE id IN (:ids)") suspend fun channelsByIds(ids: List<String>): List<ChannelEntity>
    @Query("SELECT * FROM channels WHERE id IN (:ids)") fun observeChannelsByIds(ids: List<String>): Flow<List<ChannelEntity>>
    @Query("SELECT * FROM channels c WHERE c.id IN (SELECT c2.id FROM channels c2 WHERE c2.groupKey = c.groupKey ORDER BY c2.qualityRank, c2.number LIMIT 1) ORDER BY number") suspend fun allChannelsList(): List<ChannelEntity>
    @Query("SELECT * FROM channels c WHERE c.categoryId = :categoryId AND (c.id IN (SELECT c2.id FROM channels c2 WHERE c2.groupKey = c.groupKey AND c2.categoryId = c.categoryId ORDER BY c2.qualityRank, c2.number LIMIT 1) OR NOT EXISTS (SELECT 1 FROM channels c3 WHERE c3.groupKey = c.groupKey AND c3.categoryId = c.categoryId AND c3.qualityRank < 4)) ORDER BY number") suspend fun categoryChannelsList(categoryId: String): List<ChannelEntity>
    @Query("SELECT * FROM channels c WHERE c.isFootball = 1 AND (c.id IN (SELECT c2.id FROM channels c2 WHERE c2.groupKey = c.groupKey AND c2.isFootball = 1 ORDER BY c2.qualityRank, c2.number LIMIT 1) OR NOT EXISTS (SELECT 1 FROM channels c3 WHERE c3.groupKey = c.groupKey AND c3.isFootball = 1 AND c3.qualityRank < 4)) ORDER BY number") suspend fun footballChannelsList(): List<ChannelEntity>
    @Query("SELECT c.categoryId AS `key`, COUNT(*) AS count FROM channels c WHERE (c.id IN (SELECT c2.id FROM channels c2 WHERE c2.groupKey = c.groupKey AND c2.categoryId = c.categoryId ORDER BY c2.qualityRank, c2.number LIMIT 1) OR NOT EXISTS (SELECT 1 FROM channels c3 WHERE c3.groupKey = c.groupKey AND c3.categoryId = c.categoryId AND c3.qualityRank < 4)) GROUP BY c.categoryId") fun observeChannelCounts(): Flow<List<KeyCount>>
    @Query("SELECT id, name, epgChannelId FROM channels") suspend fun channelEpgRefs(): List<ChannelEpgRef>
    @Query("UPDATE channels SET epgKey = :key WHERE id = :id") suspend fun setEpgKey(id: String, key: String?)
    @Query("SELECT id, name, categoryName FROM channels") suspend fun channelRefs(): List<TitleRef>
    @Query("UPDATE channels SET isFootball = :value WHERE id = :id") suspend fun setChannelFootball(id: String, value: Boolean)
    @Query("SELECT id, title AS name, categoryName FROM movies") suspend fun movieRefs(): List<TitleRef>
    @Query("UPDATE movies SET isFootball = :value WHERE id = :id") suspend fun setMovieFootball(id: String, value: Boolean)
    @Query("SELECT id, title AS name, categoryName FROM series") suspend fun seriesRefs(): List<TitleRef>
    @Query("UPDATE series SET isFootball = :value WHERE id = :id") suspend fun setSeriesFootball(id: String, value: Boolean)

    // --- filmes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMovies(list: List<MovieEntity>)
    @Query("DELETE FROM movies") suspend fun deleteMovies()
    @Query("SELECT COUNT(*) FROM movies") fun observeMovieCount(): Flow<Int>
    @Query("SELECT * FROM movies m WHERE (m.id IN (SELECT m2.id FROM movies m2 WHERE m2.normalizedTitle = m.normalizedTitle AND IFNULL(m2.year, 0) = IFNULL(m.year, 0) ORDER BY m2.qualityRank, m2.addedAt DESC LIMIT 1) OR NOT EXISTS (SELECT 1 FROM movies m3 WHERE m3.normalizedTitle = m.normalizedTitle AND IFNULL(m3.year, 0) = IFNULL(m.year, 0) AND m3.qualityRank < 4)) ORDER BY addedAt DESC, title LIMIT :limit") fun observeAllMovies(limit: Int): Flow<List<MovieEntity>>
    @Query("SELECT * FROM movies m WHERE m.categoryId = :categoryId AND (m.id IN (SELECT m2.id FROM movies m2 WHERE m2.normalizedTitle = m.normalizedTitle AND IFNULL(m2.year, 0) = IFNULL(m.year, 0) AND m2.categoryId = m.categoryId ORDER BY m2.qualityRank, m2.addedAt DESC LIMIT 1) OR NOT EXISTS (SELECT 1 FROM movies m3 WHERE m3.normalizedTitle = m.normalizedTitle AND IFNULL(m3.year, 0) = IFNULL(m.year, 0) AND m3.categoryId = m.categoryId AND m3.qualityRank < 4)) ORDER BY addedAt DESC, title LIMIT :limit") fun observeCategoryMovies(categoryId: String, limit: Int): Flow<List<MovieEntity>>
    @Query("SELECT * FROM movies m WHERE m.isFootball = 1 AND (m.id IN (SELECT m2.id FROM movies m2 WHERE m2.normalizedTitle = m.normalizedTitle AND IFNULL(m2.year, 0) = IFNULL(m.year, 0) AND m2.isFootball = 1 ORDER BY m2.qualityRank, m2.addedAt DESC LIMIT 1) OR NOT EXISTS (SELECT 1 FROM movies m3 WHERE m3.normalizedTitle = m.normalizedTitle AND IFNULL(m3.year, 0) = IFNULL(m.year, 0) AND m3.isFootball = 1 AND m3.qualityRank < 4)) ORDER BY addedAt DESC LIMIT :limit") fun observeFootballMovies(limit: Int): Flow<List<MovieEntity>>
    @Query("SELECT * FROM movies WHERE id = :id") fun observeMovie(id: String): Flow<MovieEntity?>
    @Query("SELECT * FROM movies WHERE id = :id") suspend fun movieById(id: String): MovieEntity?
    @Query("SELECT * FROM movies WHERE normalizedTitle = :normalizedTitle AND IFNULL(year, 0) = IFNULL(:year, 0) ORDER BY qualityRank, addedAt DESC") suspend fun movieVariants(normalizedTitle: String, year: Int?): List<MovieEntity>
    @Query("SELECT * FROM movies WHERE id IN (:ids)") suspend fun moviesByIds(ids: List<String>): List<MovieEntity>
    @Query("SELECT * FROM movies WHERE normalizedTitle IN (:keys) LIMIT 10") suspend fun moviesByNormalizedTitles(keys: List<String>): List<MovieEntity>
    @Query("SELECT * FROM movies WHERE id IN (:ids)") fun observeMoviesByIds(ids: List<String>): Flow<List<MovieEntity>>
    @Query("SELECT m.categoryId AS `key`, COUNT(*) AS count FROM movies m WHERE (m.id IN (SELECT m2.id FROM movies m2 WHERE m2.normalizedTitle = m.normalizedTitle AND IFNULL(m2.year, 0) = IFNULL(m.year, 0) AND m2.categoryId = m.categoryId ORDER BY m2.qualityRank, m2.addedAt DESC LIMIT 1) OR NOT EXISTS (SELECT 1 FROM movies m3 WHERE m3.normalizedTitle = m.normalizedTitle AND IFNULL(m3.year, 0) = IFNULL(m.year, 0) AND m3.categoryId = m.categoryId AND m3.qualityRank < 4)) GROUP BY m.categoryId") fun observeMovieCounts(): Flow<List<KeyCount>>

    // --- séries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSeries(list: List<SeriesEntity>)
    @Query("DELETE FROM series") suspend fun deleteSeries()
    @Query("SELECT COUNT(*) FROM series") fun observeSeriesCount(): Flow<Int>
    @Query("SELECT * FROM series WHERE (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY title LIMIT :limit") fun observeSeries(categoryId: String?, limit: Int): Flow<List<SeriesEntity>>
    @Query("SELECT * FROM series WHERE isFootball = 1 ORDER BY title LIMIT :limit") fun observeFootballSeries(limit: Int): Flow<List<SeriesEntity>>
    @Query("SELECT * FROM series WHERE id = :id") fun observeSeriesById(id: String): Flow<SeriesEntity?>
    @Query("SELECT * FROM series WHERE id = :id") suspend fun seriesById(id: String): SeriesEntity?
    @Query("SELECT * FROM series WHERE id IN (:ids)") suspend fun seriesByIds(ids: List<String>): List<SeriesEntity>
    @Query("SELECT * FROM series WHERE normalizedTitle IN (:keys) LIMIT 10") suspend fun seriesByNormalizedTitles(keys: List<String>): List<SeriesEntity>
    @Query("SELECT * FROM series WHERE id IN (:ids)") fun observeSeriesByIds(ids: List<String>): Flow<List<SeriesEntity>>
    @Query("UPDATE series SET episodesLoaded = :loaded WHERE id = :id") suspend fun setEpisodesLoaded(id: String, loaded: Boolean)
    @Query("SELECT categoryId AS `key`, COUNT(*) AS count FROM series GROUP BY categoryId") fun observeSeriesCounts(): Flow<List<KeyCount>>

    // --- episódios ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEpisodes(list: List<EpisodeEntity>)
    @Query("DELETE FROM episodes") suspend fun deleteEpisodes()
    @Query("DELETE FROM episodes WHERE seriesId = :seriesId") suspend fun deleteEpisodesOf(seriesId: String)
    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY season, number") fun observeEpisodes(seriesId: String): Flow<List<EpisodeEntity>>
    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY season, number") suspend fun episodesOf(seriesId: String): List<EpisodeEntity>
    @Query("SELECT * FROM episodes WHERE id = :id") suspend fun episodeById(id: String): EpisodeEntity?

    // --- busca ---
    @Insert suspend fun insertSearch(list: List<SearchIndexEntity>)
    @Query("DELETE FROM search_index WHERE type = :type") suspend fun deleteSearch(type: String)
    @Query("SELECT refId, type FROM search_index WHERE search_index MATCH :query LIMIT :limit") suspend fun search(query: String, limit: Int): List<SearchHit>
}

@Dao
interface SyncStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(state: SyncStateEntity)
    @Query("SELECT * FROM sync_state") suspend fun all(): List<SyncStateEntity>
    @Query("DELETE FROM sync_state") suspend fun clear()
}

@Dao
interface EpgDao {
    @Insert suspend fun insertPrograms(list: List<EpgProgramEntity>)
    @Query("DELETE FROM epg_programs") suspend fun deletePrograms()
    @Query("SELECT COUNT(*) FROM epg_programs") suspend fun countPrograms(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertChannels(list: List<EpgChannelEntity>)
    @Query("DELETE FROM epg_channels") suspend fun deleteChannels()
    @Query("SELECT * FROM epg_channels") suspend fun allChannels(): List<EpgChannelEntity>

    @Query("SELECT * FROM epg_programs WHERE channelKey = :key AND `end` > :from ORDER BY start LIMIT :limit")
    fun observeUpcoming(key: String, from: Long, limit: Int): Flow<List<EpgProgramEntity>>

    @Query(
        "SELECT p.* FROM epg_programs p INNER JOIN channels c ON c.epgKey = p.channelKey " +
            "WHERE (:categoryId IS NULL OR c.categoryId = :categoryId) AND p.start <= :now AND p.`end` > :now",
    )
    suspend fun nowPlaying(categoryId: String?, now: Long): List<EpgProgramEntity>

    @Query(
        "SELECT p.* FROM epg_programs p INNER JOIN channels c ON c.epgKey = p.channelKey " +
            "WHERE c.isFootball = 1 AND p.start <= :now AND p.`end` > :now",
    )
    suspend fun nowPlayingFootball(now: Long): List<EpgProgramEntity>
}

@Dao
interface LibraryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFavorite(favorite: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE itemId = :itemId") suspend fun deleteFavorite(itemId: String)
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC") fun observeFavorites(): Flow<List<FavoriteEntity>>
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE itemId = :itemId)") suspend fun isFavorite(itemId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertHistory(entry: WatchHistoryEntity)
    @Query("SELECT * FROM watch_history WHERE itemId = :itemId") suspend fun history(itemId: String): WatchHistoryEntity?
    @Query("SELECT * FROM watch_history WHERE type != 'channel' ORDER BY updatedAt DESC LIMIT :limit") fun observeVodHistory(limit: Int): Flow<List<WatchHistoryEntity>>
    @Query("SELECT * FROM watch_history WHERE seriesId = :seriesId") fun observeSeriesHistory(seriesId: String): Flow<List<WatchHistoryEntity>>
    @Query("SELECT * FROM watch_history WHERE seriesId = :seriesId ORDER BY updatedAt DESC LIMIT 1") suspend fun latestOfSeries(seriesId: String): WatchHistoryEntity?
    @Query("SELECT * FROM watch_history WHERE type = 'channel' ORDER BY updatedAt DESC LIMIT :limit") fun observeRecentChannels(limit: Int): Flow<List<WatchHistoryEntity>>
    @Query("DELETE FROM watch_history WHERE itemId = :itemId") suspend fun deleteHistory(itemId: String)
}

@Dao
interface TmdbDao {
    @Query("SELECT * FROM tmdb_cache WHERE `key` = :key") suspend fun cache(key: String): TmdbCacheEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCache(entry: TmdbCacheEntity)
    @Query("SELECT * FROM tmdb_details WHERE tmdbId = :tmdbId AND type = :type") suspend fun details(tmdbId: Int, type: String): TmdbDetailsEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDetails(entry: TmdbDetailsEntity)
    @Query("SELECT * FROM tmdb_match WHERE itemId = :itemId") suspend fun match(itemId: String): TmdbMatchEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMatch(entry: TmdbMatchEntity)
    @Query("DELETE FROM tmdb_cache WHERE fetchedAt < :before") suspend fun pruneCache(before: Long)
    @Query("SELECT * FROM team_crests WHERE teamKey = :key") suspend fun crest(key: String): TeamCrestEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCrest(entry: TeamCrestEntity)
}
