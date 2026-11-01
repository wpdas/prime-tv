package com.wpdas.primetv.data.repository

import com.wpdas.primetv.data.local.db.PrimeDatabase
import com.wpdas.primetv.data.local.prefs.PlaylistStore
import com.wpdas.primetv.data.remote.PlaylistDetector
import com.wpdas.primetv.domain.model.DetectionResult
import com.wpdas.primetv.domain.model.PlaylistSource
import com.wpdas.primetv.domain.repository.PlaylistRepository
import com.wpdas.primetv.domain.sync.CatalogSync
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val store: PlaylistStore,
    private val detector: PlaylistDetector,
    private val db: PrimeDatabase,
    private val sync: CatalogSync,
) : PlaylistRepository {

    override val source: Flow<PlaylistSource?> = store.source

    override suspend fun detect(url: String): DetectionResult = detector.detect(url)

    override suspend fun save(source: PlaylistSource) {
        sync.cancel()
        store.save(source)
    }

    /** Trocar de lista: apaga a URL e todo o catálogo importado. */
    override suspend fun clear() {
        sync.cancel()
        store.clear()
        db.clearAllTables()
    }
}
