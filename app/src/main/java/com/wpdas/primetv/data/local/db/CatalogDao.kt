package com.wpdas.primetv.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class KeyCount(val key: String?, val count: Int)
data class SearchHit(val refId: String, val type: String)
data class ChannelEpgRef(val id: String, val name: String, val epgChannelId: String?)

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
    @Query("SELECT * FROM channels WHERE (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY number") fun observeChannels(categoryId: String?): Flow<List<ChannelEntity>>
    @Query("SELECT * FROM channels WHERE isFootball = 1 ORDER BY number") fun observeFootballChannelsAll(): Flow<List<ChannelEntity>>
    @Query("SELECT * FROM channels WHERE isFootball = 1 ORDER BY number LIMIT :limit") fun observeFootballChannels(limit: Int): Flow<List<ChannelEntity>>
    @Query("SELECT COUNT(*) FROM channels WHERE isFootball = 1") fun observeFootballChannelCount(): Flow<Int>
    @Query("SELECT * FROM channels WHERE id = :id") fun observeChannel(id: String): Flow<ChannelEntity?>
    @Query("SELECT * FROM channels WHERE id IN (:ids)") suspend fun channelsByIds(ids: List<String>): List<ChannelEntity>
    @Query("SELECT categoryId AS `key`, COUNT(*) AS count FROM channels GROUP BY categoryId") fun observeChannelCounts(): Flow<List<KeyCount>>
    @Query("SELECT id, name, epgChannelId FROM channels") suspend fun channelEpgRefs(): List<ChannelEpgRef>
    @Query("UPDATE channels SET epgKey = :key WHERE id = :id") suspend fun setEpgKey(id: String, key: String?)

    // --- filmes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMovies(list: List<MovieEntity>)
    @Query("DELETE FROM movies") suspend fun deleteMovies()
    @Query("SELECT COUNT(*) FROM movies") fun observeMovieCount(): Flow<Int>
    @Query("SELECT * FROM movies WHERE (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY addedAt DESC, title LIMIT :limit") fun observeMovies(categoryId: String?, limit: Int): Flow<List<MovieEntity>>
    @Query("SELECT * FROM movies WHERE isFootball = 1 ORDER BY addedAt DESC LIMIT :limit") fun observeFootballMovies(limit: Int): Flow<List<MovieEntity>>
    @Query("SELECT * FROM movies WHERE id = :id") fun observeMovie(id: String): Flow<MovieEntity?>
    @Query("SELECT * FROM movies WHERE id IN (:ids)") suspend fun moviesByIds(ids: List<String>): List<MovieEntity>
    @Query("SELECT categoryId AS `key`, COUNT(*) AS count FROM movies GROUP BY categoryId") fun observeMovieCounts(): Flow<List<KeyCount>>

    // --- séries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSeries(list: List<SeriesEntity>)
    @Query("DELETE FROM series") suspend fun deleteSeries()
    @Query("SELECT COUNT(*) FROM series") fun observeSeriesCount(): Flow<Int>
    @Query("SELECT * FROM series WHERE (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY title LIMIT :limit") fun observeSeries(categoryId: String?, limit: Int): Flow<List<SeriesEntity>>
    @Query("SELECT * FROM series WHERE isFootball = 1 ORDER BY title LIMIT :limit") fun observeFootballSeries(limit: Int): Flow<List<SeriesEntity>>
    @Query("SELECT * FROM series WHERE id = :id") fun observeSeriesById(id: String): Flow<SeriesEntity?>
    @Query("SELECT * FROM series WHERE id = :id") suspend fun seriesById(id: String): SeriesEntity?
    @Query("SELECT * FROM series WHERE id IN (:ids)") suspend fun seriesByIds(ids: List<String>): List<SeriesEntity>
    @Query("UPDATE series SET episodesLoaded = :loaded WHERE id = :id") suspend fun setEpisodesLoaded(id: String, loaded: Boolean)
    @Query("SELECT categoryId AS `key`, COUNT(*) AS count FROM series GROUP BY categoryId") fun observeSeriesCounts(): Flow<List<KeyCount>>

    // --- episódios ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEpisodes(list: List<EpisodeEntity>)
    @Query("DELETE FROM episodes") suspend fun deleteEpisodes()
    @Query("DELETE FROM episodes WHERE seriesId = :seriesId") suspend fun deleteEpisodesOf(seriesId: String)
    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY season, number") fun observeEpisodes(seriesId: String): Flow<List<EpisodeEntity>>

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
