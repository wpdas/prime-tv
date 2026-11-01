package com.wpdas.primetv.domain.sync

import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

/** Sincronização do catálogo com a lista do usuário, seção por seção, fora da UI. */
interface CatalogSync {
    val status: StateFlow<Map<ContentSection, SyncState>>

    /** Sincroniza todas as seções em sequência (Ao vivo primeiro). Ignora se já estiver rodando. */
    fun syncAll()

    /** Sincroniza só uma seção. */
    fun sync(section: ContentSection)

    /** Sincroniza o que estiver vencido segundo os intervalos configurados (chamado ao abrir o app). */
    fun syncStale()

    fun cancel()
}
