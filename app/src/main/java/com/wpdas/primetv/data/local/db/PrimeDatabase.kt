package com.wpdas.primetv.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CategoryEntity::class,
        ChannelEntity::class,
        MovieEntity::class,
        SeriesEntity::class,
        EpisodeEntity::class,
        SearchIndexEntity::class,
        SyncStateEntity::class,
        EpgProgramEntity::class,
        EpgChannelEntity::class,
        FavoriteEntity::class,
        WatchHistoryEntity::class,
        TmdbCacheEntity::class,
        TmdbDetailsEntity::class,
        TmdbMatchEntity::class,
        TeamCrestEntity::class,
        LeagueBadgeEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
abstract class PrimeDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun epgDao(): EpgDao
    abstract fun libraryDao(): LibraryDao
    abstract fun tmdbDao(): TmdbDao

    companion object {
        const val NAME = "primetv.db"
        /** Tamanho dos lotes de inserção durante a importação. */
        const val BATCH_SIZE = 500
    }
}
