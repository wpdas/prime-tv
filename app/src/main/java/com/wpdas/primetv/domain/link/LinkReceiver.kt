package com.wpdas.primetv.domain.link

import kotlinx.coroutines.flow.Flow

/** Sessão de "Receber link pelo celular": o app mostra [code] e espera o site entregar a URL. */
data class LinkSession(val code: String, val expiresAt: Long, val url: String)

sealed interface LinkEvent {
    data object Waiting : LinkEvent
    data class Received(val link: String) : LinkEvent
    data object Expired : LinkEvent
}

/**
 * Abstrai o canal de entrega (WebSocket ou polling, fase 2 do roadmap). A implementação
 * fake desta fase só serve para exercitar a interface.
 */
interface LinkReceiver {
    fun newSession(): LinkSession
    fun events(session: LinkSession): Flow<LinkEvent>
}
