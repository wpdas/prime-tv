package com.wpdas.primetv.data.di

import android.content.Context
import androidx.room.Room
import com.wpdas.primetv.data.fake.FakeHomeRepository
import com.wpdas.primetv.data.local.db.CatalogDao
import com.wpdas.primetv.data.local.db.PrimeDatabase
import com.wpdas.primetv.data.local.db.SyncStateDao
import com.wpdas.primetv.data.repository.PlaylistRepositoryImpl
import com.wpdas.primetv.data.sync.CatalogSyncer
import com.wpdas.primetv.domain.repository.HomeRepository
import com.wpdas.primetv.domain.repository.PlaylistRepository
import com.wpdas.primetv.domain.sync.CatalogSync
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    /** Fase 1: dados fake na Início. Substituído pela implementação com Room na fase 3. */
    @Binds abstract fun bindHomeRepository(impl: FakeHomeRepository): HomeRepository
    @Binds abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository
    @Binds abstract fun bindCatalogSync(impl: CatalogSyncer): CatalogSync
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PrimeDatabase =
        Room.databaseBuilder(context, PrimeDatabase::class.java, PrimeDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideCatalogDao(db: PrimeDatabase): CatalogDao = db.catalogDao()
    @Provides fun provideSyncStateDao(db: PrimeDatabase): SyncStateDao = db.syncStateDao()
}
