package com.wpdas.primetv.data.di

import android.content.Context
import androidx.room.Room
import com.wpdas.primetv.data.local.db.CatalogDao
import com.wpdas.primetv.data.local.db.EpgDao
import com.wpdas.primetv.data.local.db.LibraryDao
import com.wpdas.primetv.data.local.db.TmdbDao
import com.wpdas.primetv.data.local.db.PrimeDatabase
import com.wpdas.primetv.data.local.db.SyncStateDao
import com.wpdas.primetv.data.repository.CatalogRepositoryImpl
import com.wpdas.primetv.data.repository.HomeRepositoryImpl
import com.wpdas.primetv.data.repository.LibraryRepositoryImpl
import com.wpdas.primetv.data.repository.PlaylistRepositoryImpl
import com.wpdas.primetv.data.repository.TmdbRepositoryImpl
import com.wpdas.primetv.data.sync.CatalogSyncer
import com.wpdas.primetv.domain.football.FootballClassifier
import com.wpdas.primetv.domain.repository.CatalogRepository
import com.wpdas.primetv.domain.repository.HomeRepository
import com.wpdas.primetv.domain.repository.LibraryRepository
import com.wpdas.primetv.domain.repository.PlaylistRepository
import com.wpdas.primetv.domain.repository.RecommendationRepository
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
    @Binds abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository
    @Binds abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository
    @Binds abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository
    @Binds abstract fun bindRecommendationRepository(impl: TmdbRepositoryImpl): RecommendationRepository
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
    @Provides fun provideEpgDao(db: PrimeDatabase): EpgDao = db.epgDao()
    @Provides fun provideLibraryDao(db: PrimeDatabase): LibraryDao = db.libraryDao()
    @Provides fun provideTmdbDao(db: PrimeDatabase): TmdbDao = db.tmdbDao()

    /** Palavras-chave padrão; as configuráveis (DataStore) entram na fase 6. */
    @Provides @Singleton fun provideFootballClassifier(): FootballClassifier = FootballClassifier()
}
