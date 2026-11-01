package com.wpdas.primetv.data.remote.xtream

import com.wpdas.primetv.domain.model.PlaylistError
import com.wpdas.primetv.domain.model.PlaylistException
import com.wpdas.primetv.domain.model.PlaylistSource
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.decodeToSequence
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cliente da API Xtream Codes (`player_api.php`). Listas grandes são decodificadas em
 * streaming ([decodeToSequence]) para não carregar dezenas de MB de JSON na memória.
 * Nenhum método registra URL, usuário ou senha em log.
 */
@OptIn(ExperimentalSerializationApi::class)
@Singleton
class XtreamClient @Inject constructor(
    private val http: OkHttpClient,
    private val json: Json,
) {
    fun authenticate(baseUrl: String, username: String, password: String): XtreamAuthResponse =
        open(apiUrl(baseUrl, username, password, null)).use { stream ->
            runCatching { json.decodeFromStream(XtreamAuthResponse.serializer(), stream) }
                .getOrElse { throw PlaylistException(PlaylistError.InvalidPlaylist, it) }
        }

    fun liveCategories(src: PlaylistSource.Xtream): List<XtreamCategoryDto> = list(src, "get_live_categories", XtreamCategoryDto.serializer())
    fun vodCategories(src: PlaylistSource.Xtream): List<XtreamCategoryDto> = list(src, "get_vod_categories", XtreamCategoryDto.serializer())
    fun seriesCategories(src: PlaylistSource.Xtream): List<XtreamCategoryDto> = list(src, "get_series_categories", XtreamCategoryDto.serializer())

    suspend fun <R> liveStreams(src: PlaylistSource.Xtream, block: suspend (Sequence<XtreamLiveStreamDto>) -> R): R =
        stream(src, "get_live_streams", XtreamLiveStreamDto.serializer(), block)

    suspend fun <R> vodStreams(src: PlaylistSource.Xtream, block: suspend (Sequence<XtreamVodStreamDto>) -> R): R =
        stream(src, "get_vod_streams", XtreamVodStreamDto.serializer(), block)

    suspend fun <R> series(src: PlaylistSource.Xtream, block: suspend (Sequence<XtreamSeriesDto>) -> R): R =
        stream(src, "get_series", XtreamSeriesDto.serializer(), block)

    fun seriesInfo(src: PlaylistSource.Xtream, seriesId: String): XtreamSeriesInfoResponse =
        open(apiUrl(src, "get_series_info", "series_id" to seriesId)).use { stream ->
            decodeOrThrow(XtreamSeriesInfoResponse.serializer(), stream)
        }

    fun vodInfo(src: PlaylistSource.Xtream, vodId: String): XtreamVodInfoResponse =
        open(apiUrl(src, "get_vod_info", "vod_id" to vodId)).use { stream ->
            decodeOrThrow(XtreamVodInfoResponse.serializer(), stream)
        }

    /** Episódios do `get_series_info`, aceitando o formato objeto-por-temporada ou lista. */
    fun parseEpisodes(info: XtreamSeriesInfoResponse): List<XtreamEpisodeDto> {
        val serializer = ListSerializer(XtreamEpisodeDto.serializer())
        return when (val eps = info.episodes) {
            is JsonObject -> eps.values.flatMap { runCatching { json.decodeFromJsonElement(serializer, it) }.getOrDefault(emptyList()) }
            is JsonArray -> runCatching { json.decodeFromJsonElement(serializer, eps) }.getOrDefault(emptyList())
            else -> emptyList()
        }
    }

    // ------------------------------------------------------------------------------------

    private fun <T> list(src: PlaylistSource.Xtream, action: String, serializer: KSerializer<T>): List<T> =
        open(apiUrl(src, action)).use { stream -> decodeOrThrow(ListSerializer(serializer), stream) }

    private suspend fun <T, R> stream(
        src: PlaylistSource.Xtream,
        action: String,
        serializer: DeserializationStrategy<T>,
        block: suspend (Sequence<T>) -> R,
    ): R = open(apiUrl(src, action)).use { stream ->
        try {
            block(json.decodeToSequence(stream, serializer, DecodeSequenceMode.ARRAY_WRAPPED))
        } catch (e: SerializationException) {
            throw PlaylistException(PlaylistError.InvalidPlaylist, e)
        } catch (e: IllegalArgumentException) {
            throw PlaylistException(PlaylistError.InvalidPlaylist, e)
        }
    }

    private fun <T> decodeOrThrow(serializer: DeserializationStrategy<T>, stream: InputStream): T = try {
        json.decodeFromStream(serializer, stream)
    } catch (e: SerializationException) {
        throw PlaylistException(PlaylistError.InvalidPlaylist, e)
    } catch (e: IllegalArgumentException) {
        throw PlaylistException(PlaylistError.InvalidPlaylist, e)
    }

    private fun open(url: HttpUrl): InputStream {
        val response: Response = try {
            http.newCall(Request.Builder().url(url).get().build()).execute()
        } catch (e: UnknownHostException) {
            throw PlaylistException(PlaylistError.NoInternet, e)
        } catch (e: SocketTimeoutException) {
            throw PlaylistException(PlaylistError.ServerUnreachable, e)
        } catch (e: IOException) {
            throw PlaylistException(PlaylistError.ServerUnreachable, e)
        }
        if (!response.isSuccessful) {
            response.close()
            throw PlaylistException(
                if (response.code == 401 || response.code == 403) PlaylistError.InvalidCredentials else PlaylistError.ServerUnreachable,
            )
        }
        return response.body.byteStream()
    }

    private fun apiUrl(src: PlaylistSource.Xtream, action: String, vararg extra: Pair<String, String>): HttpUrl =
        apiUrl(src.baseUrl, src.username, src.password, action, *extra)

    private fun apiUrl(baseUrl: String, username: String, password: String, action: String?, vararg extra: Pair<String, String>): HttpUrl =
        baseUrl.toHttpUrl().newBuilder()
            .addPathSegment("player_api.php")
            .addQueryParameter("username", username)
            .addQueryParameter("password", password)
            .apply {
                if (action != null) addQueryParameter("action", action)
                extra.forEach { (k, v) -> addQueryParameter(k, v) }
            }
            .build()

    companion object {
        fun liveStreamUrl(src: PlaylistSource.Xtream, streamId: String, ext: String = "ts") =
            "${src.baseUrl}/live/${src.username}/${src.password}/$streamId.$ext"

        fun movieStreamUrl(src: PlaylistSource.Xtream, streamId: String, ext: String?) =
            "${src.baseUrl}/movie/${src.username}/${src.password}/$streamId.${ext?.ifBlank { null } ?: "mp4"}"

        fun episodeStreamUrl(src: PlaylistSource.Xtream, episodeId: String, ext: String?) =
            "${src.baseUrl}/series/${src.username}/${src.password}/$episodeId.${ext?.ifBlank { null } ?: "mp4"}"

        fun epgUrl(src: PlaylistSource.Xtream) =
            "${src.baseUrl}/xmltv.php?username=${src.username}&password=${src.password}"
    }
}
