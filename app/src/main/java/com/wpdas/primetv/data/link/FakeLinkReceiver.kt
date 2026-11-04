package com.wpdas.primetv.data.link

import com.wpdas.primetv.BuildConfig
import com.wpdas.primetv.domain.link.LinkEvent
import com.wpdas.primetv.domain.link.LinkReceiver
import com.wpdas.primetv.domain.link.LinkSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Receptor de teste, sem rede: gera o código localmente e fica "aguardando". Em build debug com
 * IPTV_TEST_URL, entrega essa URL depois de alguns segundos para dar para ver o estado
 * "Link recebido". Ligado por `FAKE_LINK_RECEIVER=true` (ver [LinkModule]).
 */
@Singleton
class FakeLinkReceiver @Inject constructor() : LinkReceiver {

    override suspend fun newSession(): LinkSession {
        val code = LinkCodes.generate()
        return LinkSession(code = code, expiresAt = System.currentTimeMillis() + SESSION_TTL_MS, url = LinkCodes.urlFor(code))
    }

    override fun events(session: LinkSession): Flow<LinkEvent> = flow {
        emit(LinkEvent.Waiting)
        val testUrl = if (BuildConfig.DEBUG) BuildConfig.IPTV_TEST_URL else ""
        if (testUrl.isNotBlank()) {
            delay(FAKE_DELIVERY_MS)
            emit(LinkEvent.Received(testUrl))
            return@flow
        }
        delay((session.expiresAt - System.currentTimeMillis()).coerceAtLeast(0))
        emit(LinkEvent.Expired)
    }

    override suspend fun release(session: LinkSession) = Unit

    private companion object {
        const val SESSION_TTL_MS = 10 * 60 * 1000L
        const val FAKE_DELIVERY_MS = 15_000L
    }
}

/** Códigos curtos sem caracteres ambíguos (0/O, 1/I/L); mesmo alfabeto do site. */
object LinkCodes {
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()

    fun generate(length: Int = 6): String = buildString { repeat(length) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    /** "K7P4QX" -> "K7P-4QX" para leitura na TV. */
    fun format(code: String): String = if (code.length == 6) "${code.take(3)}-${code.drop(3)}" else code

    fun urlFor(code: String): String = LinkSites.qrUrl(BuildConfig.LINK_SITE_URL, code)
}
