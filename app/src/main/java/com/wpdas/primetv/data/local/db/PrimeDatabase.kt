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
    ],
    version = 1,
    exportSchema = true,
)
abstract class PrimeDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        const val NAME = "primetv.db"
        /** Tamanho dos lotes de inserção durante a importação. */
        const val BATCH_SIZE = 500
    }
}
