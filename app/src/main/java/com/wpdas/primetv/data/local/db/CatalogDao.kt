package com.wpdas.primetv.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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
    @Query("SELECT * FROM channels ORDER BY number LIMIT :limit") suspend fun channels(limit: Int): List<ChannelEntity>

    // --- filmes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMovies(list: List<MovieEntity>)
    @Query("DELETE FROM movies") suspend fun deleteMovies()
    @Query("SELECT COUNT(*) FROM movies") fun observeMovieCount(): Flow<Int>

    // --- séries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSeries(list: List<SeriesEntity>)
    @Query("DELETE FROM series") suspend fun deleteSeries()
    @Query("SELECT COUNT(*) FROM series") fun observeSeriesCount(): Flow<Int>

    // --- episódios ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEpisodes(list: List<EpisodeEntity>)
    @Query("DELETE FROM episodes") suspend fun deleteEpisodes()
    @Query("DELETE FROM episodes WHERE seriesId = :seriesId") suspend fun deleteEpisodesOf(seriesId: String)

    // --- busca ---
    @Insert suspend fun insertSearch(list: List<SearchIndexEntity>)
    @Query("DELETE FROM search_index WHERE type = :type") suspend fun deleteSearch(type: String)
}

@Dao
interface SyncStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(state: SyncStateEntity)
    @Query("SELECT * FROM sync_state") suspend fun all(): List<SyncStateEntity>
    @Query("DELETE FROM sync_state") suspend fun clear()
}
