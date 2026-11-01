package com.wpdas.primetv.data.remote

import com.wpdas.primetv.core.di.IoDispatcher
import com.wpdas.primetv.data.remote.m3u.M3uItem
import com.wpdas.primetv.data.remote.m3u.M3uParser
import com.wpdas.primetv.data.remote.xtream.XtreamClient
import com.wpdas.primetv.domain.model.DetectionResult
import com.wpdas.primetv.domain.model.PlaylistError
import com.wpdas.primetv.domain.model.PlaylistException
import com.wpdas.primetv.domain.model.PlaylistSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * Descobre o tipo da lista:
 * 1. URL com `username` e `password` -> testa `player_api.php`; se autenticar, é Xtream.
 * 2. Senão, baixa o começo do arquivo e confere se começa com `#EXTM3U` (M3U).
 */
class PlaylistDetector @Inject constructor(
    private val http: OkHttpClient,
    private val xtream: XtreamClient,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun detect(rawUrl: String): DetectionResult = withContext(io) {
        val url = normalizeUrl(rawUrl) ?: throw PlaylistException(PlaylistError.InvalidUrl)
        val username = url.queryParameter("username")
        val password = url.queryParameter("password")

        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            val base = baseOf(url)
            val auth = try {
                xtream.authenticate(base, username, password)
            } catch (e: PlaylistException) {
                // Servidor fora do ar ou sem internet: não adianta tentar M3U no mesmo host.
                if (e.error == PlaylistError.NoInternet || e.error == PlaylistError.ServerUnreachable) throw e
                null
            }
            if (auth?.userInfo != null) {
                if (auth.userInfo.isAuthenticated) {
                    return@withContext DetectionResult(
                        source = PlaylistSource.Xtream(url.toString(), base, username, password),
                        hasEpg = true,
                    )
                }
                throw PlaylistException(PlaylistError.InvalidCredentials)
            }
        }
        detectM3u(url)
    }

    private fun detectM3u(url: HttpUrl): DetectionResult {
        val request = Request.Builder().url(url).get().build()
        val response = try {
            http.newCall(request).execute()
        } catch (e: UnknownHostException) {
            throw PlaylistException(PlaylistError.NoInternet, e)
        } catch (e: SocketTimeoutException) {
            throw PlaylistException(PlaylistError.ServerUnreachable, e)
        } catch (e: IOException) {
            throw PlaylistException(PlaylistError.ServerUnreachable, e)
        }
        response.use { resp ->
            if (resp.code == 401 || resp.code == 403) throw PlaylistException(PlaylistError.InvalidCredentials)
            if (!resp.isSuccessful) throw PlaylistException(PlaylistError.ServerUnreachable)
            val source = resp.body.source()
            // Lê só o começo: o suficiente para o cabeçalho e a primeira entrada.
            source.request(HEAD_BYTES)
            val head = source.buffer.copy().readUtf8(minOf(source.buffer.size, HEAD_BYTES))
            val trimmed = head.trimStart('﻿', ' ', '\r', '\n', '\t')
            if (!trimmed.startsWith("#EXTM3U", ignoreCase = true)) throw PlaylistException(PlaylistError.InvalidPlaylist)
            val header = M3uParser.parse(trimmed.reader().buffered())
                .filterIsInstance<M3uItem.Header>()
                .firstOrNull()?.header
            return DetectionResult(
                source = PlaylistSource.M3u(originalUrl = url.toString(), url = url.toString(), epgUrl = header?.epgUrl),
                hasEpg = header?.epgUrl != null,
            )
        }
    }

    companion object {
        private const val HEAD_BYTES = 4096L

        /** Aceita URL sem esquema ("servidor:8080/get.php?...") e espaços nas pontas. */
        fun normalizeUrl(raw: String): HttpUrl? {
            val text = raw.trim()
            if (text.isEmpty()) return null
            val withScheme = if (text.contains("://")) text else "http://$text"
            return withScheme.toHttpUrlOrNull()
        }

        fun baseOf(url: HttpUrl): String {
            val defaultPort = HttpUrl.defaultPort(url.scheme)
            val port = if (url.port != defaultPort) ":${url.port}" else ""
            return "${url.scheme}://${url.host}$port"
        }
    }
}
