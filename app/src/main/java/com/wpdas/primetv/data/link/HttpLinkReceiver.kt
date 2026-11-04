package com.wpdas.primetv.data.link

import com.wpdas.primetv.BuildConfig
import com.wpdas.primetv.core.di.IoDispatcher
import com.wpdas.primetv.core.util.SslErrors
import com.wpdas.primetv.domain.link.LinkEvent
import com.wpdas.primetv.domain.link.LinkException
import com.wpdas.primetv.domain.link.LinkReceiver
import com.wpdas.primetv.domain.link.LinkSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Receptor real: cria a sessão no site, consulta a cada [POLL_MS] enquanto a tela estiver aberta
 * e confirma (DELETE) assim que a URL chega, para o servidor apagá-la. Falha de rede e limite de
 * tentativas não derrubam a espera: avisam com [LinkEvent.Offline] e seguem tentando.
 *
 * Nada daqui vai para o log: a URL tem usuário e senha e o token é o segredo da sessão.
 */
@Singleton
class HttpLinkReceiver @Inject constructor(
    private val api: LinkApi,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LinkReceiver {

    override suspend fun newSession(): LinkSession = withContext(io) {
        val created = try {
            api.createSession()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            throw LinkException(serverMessage(e), "receive.error_server", "HTTP ${e.code()}", e)
        } catch (e: UnknownHostException) {
            throw LinkException(null, "receive.error_dns", e.javaClass.simpleName, e)
        } catch (e: IOException) {
            if (SslErrors.isTls(e)) throw LinkException(null, "receive.error_ssl", SslErrors.describe(e), e)
            throw LinkException(null, "receive.error_server", e.javaClass.simpleName, e)
        } catch (e: RuntimeException) {
            throw LinkException(null, "receive.error_server", e.javaClass.simpleName, e)
        }
        LinkSession(
            code = created.code,
            expiresAt = created.expiresAt,
            url = LinkSites.qrUrl(created.siteUrl ?: BuildConfig.LINK_SITE_URL, created.code),
            deviceToken = created.deviceToken,
        )
    }

    override fun events(session: LinkSession): Flow<LinkEvent> = flow {
        emit(LinkEvent.Waiting)
        var offline = false
        while (true) {
            val result = runCatching { api.poll(session.code, bearer(session)) }
            val error = result.exceptionOrNull()
            if (error is CancellationException) throw error
            val status = result.getOrNull()
            when {
                status != null -> {
                    if (offline) {
                        offline = false
                        emit(LinkEvent.Waiting)
                    }
                    when (status.status) {
                        LinkStatusResponse.DELIVERED -> {
                            val url = status.url
                            if (url != null) {
                                emit(LinkEvent.Received(url))
                                // Confirma mesmo que quem escuta cancele logo depois: a URL some do servidor.
                                withContext(NonCancellable) { release(session) }
                            } else {
                                // Já confirmada antes (ou a URL foi apagada por prazo): não dá mais para ler.
                                emit(LinkEvent.Expired)
                            }
                            return@flow
                        }
                        LinkStatusResponse.EXPIRED -> {
                            emit(LinkEvent.Expired)
                            return@flow
                        }
                        else -> Unit
                    }
                    delay(POLL_MS)
                }
                // Token recusado: a sessão não serve mais; só um código novo resolve.
                error is HttpException && error.code() == 401 -> {
                    emit(LinkEvent.Expired)
                    return@flow
                }
                // Limite de tentativas: espera mais antes de perguntar de novo, sem alarmar.
                error is HttpException && error.code() == 429 -> delay(RATE_LIMIT_MS)
                else -> {
                    if (!offline) {
                        offline = true
                        emit(LinkEvent.Offline)
                    }
                    delay(POLL_MS)
                }
            }
        }
    }.flowOn(io)

    override suspend fun release(session: LinkSession) {
        if (session.deviceToken.isEmpty()) return
        withContext(io) { runCatching { api.release(session.code, bearer(session)) } }
    }

    private fun bearer(session: LinkSession) = "Bearer ${session.deviceToken}"

    private fun serverMessage(e: HttpException): String? = runCatching {
        val body = e.response()?.errorBody()?.string().orEmpty()
        json.decodeFromString(LinkErrorResponse.serializer(), body).message?.takeIf { it.isNotBlank() }
    }.getOrNull()

    companion object {
        const val POLL_MS = 3_000L
        const val RATE_LIMIT_MS = 10_000L
    }
}
