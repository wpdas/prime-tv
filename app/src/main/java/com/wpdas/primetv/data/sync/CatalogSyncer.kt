package com.wpdas.primetv.data.sync

import com.wpdas.primetv.core.di.ApplicationScope
import com.wpdas.primetv.core.di.IoDispatcher
import com.wpdas.primetv.data.local.db.SyncStateDao
import com.wpdas.primetv.data.local.db.SyncStateEntity
import com.wpdas.primetv.data.local.prefs.PlaylistStore
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.PlaylistError
import com.wpdas.primetv.domain.model.PlaylistException
import com.wpdas.primetv.domain.model.PlaylistSource
import com.wpdas.primetv.domain.model.SyncState
import com.wpdas.primetv.domain.sync.CatalogSync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orquestra a importação seção por seção no escopo do app (continua com a UI fechada,
 * enquanto o processo viver). A ordem fixa garante que Ao vivo libere primeiro.
 */
@Singleton
class CatalogSyncer @Inject constructor(
    private val playlistStore: PlaylistStore,
    private val xtreamImporter: XtreamImporter,
    private val m3uImporter: M3uImporter,
    private val syncStateDao: SyncStateDao,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) : CatalogSync {

    private val _status = MutableStateFlow(ContentSection.entries.associateWith<ContentSection, SyncState> { SyncState.Idle })
    override val status: StateFlow<Map<ContentSection, SyncState>> = _status.asStateFlow()

    private val mutex = Mutex()
    private var job: Job? = null

    init {
        scope.launch(io) { restorePersistedState() }
    }

    override fun syncAll() = launchSections(ORDER)

    override fun sync(section: ContentSection) = launchSections(listOf(section))

    override fun cancel() {
        job?.cancel()
        _status.update { map -> map.mapValues { (_, s) -> if (s is SyncState.Running) SyncState.Idle else s } }
    }

    private fun launchSections(sections: List<ContentSection>) {
        if (job?.isActive == true) return
        job = scope.launch(io) {
            mutex.withLock {
                val source = playlistStore.source.first() ?: return@withLock
                for (section in sections) runSection(source, section)
            }
        }
    }

    private suspend fun runSection(source: PlaylistSource, section: ContentSection) {
        set(section, SyncState.Running(0))
        val importer = when (source) {
            is PlaylistSource.Xtream -> xtreamImporter
            is PlaylistSource.M3u -> m3uImporter
        }
        val result: SyncState = try {
            when (val r = importer.import(source, section) { n -> set(section, SyncState.Running(n)) }) {
                is ImportResult.Imported -> SyncState.Done(r.count, System.currentTimeMillis())
                is ImportResult.Skipped -> SyncState.Skipped(r.reasonKey)
            }
        } catch (e: CancellationException) {
            set(section, SyncState.Idle)
            throw e
        } catch (e: PlaylistException) {
            SyncState.Error(e.error, System.currentTimeMillis())
        } catch (e: Exception) {
            SyncState.Error(PlaylistError.Unknown(e.javaClass.simpleName), System.currentTimeMillis())
        }
        set(section, result)
        persist(section, result)
    }

    private fun set(section: ContentSection, state: SyncState) {
        _status.update { it + (section to state) }
    }

    private suspend fun persist(section: ContentSection, state: SyncState) {
        val entity = when (state) {
            is SyncState.Done -> SyncStateEntity(section.name, "done", state.count, state.finishedAt, null)
            is SyncState.Skipped -> SyncStateEntity(section.name, "skipped", 0, System.currentTimeMillis(), state.reasonKey)
            is SyncState.Error -> SyncStateEntity(section.name, "error", 0, state.at, state.error.messageKey)
            else -> return
        }
        syncStateDao.upsert(entity)
    }

    private suspend fun restorePersistedState() {
        val saved = syncStateDao.all()
        if (saved.isEmpty()) return
        _status.update { current ->
            current + saved.mapNotNull { e ->
                val section = ContentSection.entries.firstOrNull { it.name == e.section } ?: return@mapNotNull null
                if (current[section] is SyncState.Running) return@mapNotNull null
                section to when (e.state) {
                    "done" -> SyncState.Done(e.count, e.finishedAt)
                    "skipped" -> SyncState.Skipped(e.detail ?: "sync.skipped_epg_pending")
                    else -> SyncState.Error(PlaylistError.Unknown(e.detail ?: ""), e.finishedAt)
                }
            }
        }
    }

    private companion object {
        val ORDER = listOf(ContentSection.LIVE, ContentSection.MOVIES, ContentSection.SERIES, ContentSection.EPG)
    }
}
