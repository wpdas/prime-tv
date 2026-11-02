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
import com.wpdas.primetv.domain.football.FootballClassifierProvider
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
    @Binds abstract fun bindSettingsRepository(impl: com.wpdas.primetv.data.local.prefs.SettingsStore): com.wpdas.primetv.domain.repository.SettingsRepository
    @Binds abstract fun bindLinkReceiver(impl: com.wpdas.primetv.data.link.FakeLinkReceiver): com.wpdas.primetv.domain.link.LinkReceiver
    @Binds abstract fun bindUpdateRepository(impl: com.wpdas.primetv.data.update.UpdateRepositoryImpl): com.wpdas.primetv.domain.update.UpdateRepository
    @Binds abstract fun bindTeamCrestRepository(impl: com.wpdas.primetv.data.remote.crests.CrestRepositoryImpl): com.wpdas.primetv.domain.crests.TeamCrestRepository
    @Binds abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository
    @Binds abstract fun bindCatalogSync(impl: CatalogSyncer): CatalogSync
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PrimeDatabase =
        Room.databaseBuilder(context, PrimeDatabase::class.java, PrimeDatabase.NAME)
            .addMigrations(*com.wpdas.primetv.data.local.db.Migrations.ALL)
            // WAL explícito: no modo automático, aparelhos marcados como "low RAM" (boxes baratas) caem
            // em TRUNCATE, e aí toda leitura da UI espera a transação de importação terminar.
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            // Só bancos de builds de desenvolvimento anteriores à primeira release (v5) são descartados.
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4)
            .build()

    @Provides fun provideCatalogDao(db: PrimeDatabase): CatalogDao = db.catalogDao()
    @Provides fun provideSyncStateDao(db: PrimeDatabase): SyncStateDao = db.syncStateDao()
    @Provides fun provideEpgDao(db: PrimeDatabase): EpgDao = db.epgDao()
    @Provides fun provideLibraryDao(db: PrimeDatabase): LibraryDao = db.libraryDao()
    @Provides fun provideTmdbDao(db: PrimeDatabase): TmdbDao = db.tmdbDao()

    /** Classificador sempre com as palavras-chave atuais das Configurações. */
    @Provides @Singleton fun provideFootballClassifierProvider(settings: com.wpdas.primetv.domain.repository.SettingsRepository): FootballClassifierProvider =
        FootballClassifierProvider { FootballClassifier(settings.settings.value.footballKeywords) }
}
