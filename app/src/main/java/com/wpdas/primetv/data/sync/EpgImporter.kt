package com.wpdas.primetv.data.sync

import androidx.room.withTransaction
import com.wpdas.primetv.core.util.TextNormalizer
import com.wpdas.primetv.data.local.db.EpgChannelEntity
import com.wpdas.primetv.data.local.db.EpgProgramEntity
import com.wpdas.primetv.data.local.db.PrimeDatabase
import com.wpdas.primetv.data.remote.epg.XmlTvItem
import com.wpdas.primetv.data.remote.epg.XmlTvParser
import com.wpdas.primetv.data.remote.xtream.XtreamClient
import com.wpdas.primetv.domain.model.PlaylistError
import com.wpdas.primetv.domain.model.PlaylistException
import com.wpdas.primetv.domain.model.PlaylistSource
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.zip.GZIPInputStream
import javax.inject.Inject

/**
 * Importa o guia XMLTV (Xtream: `xmltv.php`; M3U: `url-tvg`). Guarda só a janela útil
 * (poucas horas para trás, alguns dias para a frente) e, no fim, resolve a chave de guia
 * dos canais sem `tvg-id` casando o nome com os `display-name` do XMLTV.
 */
class EpgImporter @Inject constructor(
    private val db: PrimeDatabase,
    private val http: OkHttpClient,
) {
    suspend fun import(source: PlaylistSource, onProgress: (Int) -> Unit): ImportResult {
        val url = when (source) {
            is PlaylistSource.Xtream -> XtreamClient.epgUrl(source)
            is PlaylistSource.M3u -> source.epgUrl ?: return ImportResult.Skipped("sync.skipped_no_epg")
        }
        val now = System.currentTimeMillis()
        val windowStart = now - PAST_WINDOW_MS
        val windowEnd = now + FUTURE_WINDOW_MS
        var count = 0
        val epgDao = db.epgDao()
        val catalog = db.catalogDao()

        openStream(url).use { input ->
            db.withTransaction {
                epgDao.deletePrograms()
                epgDao.deleteChannels()
                val programs = Batch<EpgProgramEntity>(PrimeDatabase.BATCH_SIZE) { chunk ->
                    epgDao.insertPrograms(chunk)
                    count += chunk.size
                    onProgress(count)
                }
                val channels = Batch<EpgChannelEntity>(PrimeDatabase.BATCH_SIZE) { epgDao.insertChannels(it) }
                XmlTvParser.parse(input).forEach { item ->
                    when (item) {
                        is XmlTvItem.Channel -> channels.add(
                            EpgChannelEntity(
                                xmltvId = item.channel.id,
                                displayKey = TextNormalizer.normalize(item.channel.displayName),
                                iconUrl = item.channel.icon,
                            ),
                        )
                        is XmlTvItem.Programme -> {
                            val p = item.programme
                            if (p.end < windowStart || p.start > windowEnd) return@forEach
                            programs.add(EpgProgramEntity(channelKey = p.channel, start = p.start, end = p.end, title = p.title, description = p.description))
                        }
                    }
                }
                channels.flush()
                programs.flush()
                resolveChannelKeys()
            }
        }
        return ImportResult.Imported(count)
    }

    /** epgKey = epgChannelId quando existe no XMLTV; senão, casa pelo nome normalizado. */
    private suspend fun resolveChannelKeys() {
        val epgDao = db.epgDao()
        val catalog = db.catalogDao()
        val xmltv = epgDao.allChannels()
        if (xmltv.isEmpty()) return
        val ids = xmltv.mapTo(HashSet()) { it.xmltvId }
        val byName = HashMap<String, String>(xmltv.size)
        for (c in xmltv) byName.putIfAbsent(c.displayKey, c.xmltvId)
        for (ref in catalog.channelEpgRefs()) {
            val direct = ref.epgChannelId?.takeIf { it in ids }
            val key = direct ?: byName[TextNormalizer.normalize(ref.name)] ?: byName[TextNormalizer.searchKey(ref.name)]
            catalog.setEpgKey(ref.id, key)
        }
    }

    private fun openStream(url: String): InputStream {
        val response = try {
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
            throw PlaylistException(if (response.code == 401 || response.code == 403) PlaylistError.InvalidCredentials else PlaylistError.ServerUnreachable)
        }
        return maybeGunzip(response.body.byteStream())
    }

    /** Alguns servidores entregam `.xml.gz` sem Content-Encoding; detecta pelo cabeçalho mágico. */
    private fun maybeGunzip(raw: InputStream): InputStream {
        val pushback = PushbackInputStream(BufferedInputStream(raw, 64 * 1024), 2)
        val head = ByteArray(2)
        val read = pushback.read(head)
        if (read > 0) pushback.unread(head, 0, read)
        val isGzip = read == 2 && head[0] == 0x1f.toByte() && head[1] == 0x8b.toByte()
        return if (isGzip) GZIPInputStream(pushback) else pushback
    }

    private companion object {
        const val PAST_WINDOW_MS = 6 * 60 * 60 * 1000L
        const val FUTURE_WINDOW_MS = 3 * 24 * 60 * 60 * 1000L
    }
}
