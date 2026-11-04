package com.wpdas.primetv.domain.link

import kotlinx.coroutines.flow.Flow

/**
 * Sessão de "Receber link pelo celular": o app mostra [code] e espera o site entregar a URL.
 * [url] é o endereço do QR (site com o código preenchido). [deviceToken] é o segredo que só a TV
 * conhece; autentica a consulta e a confirmação e nunca aparece na tela nem em log.
 */
data class LinkSession(val code: String, val expiresAt: Long, val url: String, val deviceToken: String = "")

sealed interface LinkEvent {
    data object Waiting : LinkEvent
    data class Received(val link: String) : LinkEvent
    data object Expired : LinkEvent
    /** Sem resposta do site (rede fora, limite de tentativas). A espera continua por conta própria. */
    data object Offline : LinkEvent
}

/** Falha ao criar a sessão. [userMessage] é o texto do servidor (pt-BR, com uma ação), se houver. */
class LinkException(val userMessage: String? = null, cause: Throwable? = null) : Exception(userMessage, cause)

/**
 * Abstrai o canal de entrega. A implementação real consulta o site por polling; a fake serve
 * para testes e para desenvolvimento sem rede.
 */
interface LinkReceiver {
    /** Cria uma sessão no servidor. Lança [LinkException] quando não dá para criar. */
    suspend fun newSession(): LinkSession

    /** Espera pelo link; termina depois de [LinkEvent.Received] ou [LinkEvent.Expired]. */
    fun events(session: LinkSession): Flow<LinkEvent>

    /**
     * Confirma o recebimento (o servidor apaga a URL) ou descarta a sessão ao sair da tela ou
     * gerar outro código. Nunca lança.
     */
    suspend fun release(session: LinkSession)
}
