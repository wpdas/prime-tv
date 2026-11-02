package com.wpdas.primetv.data.sync

import android.content.Context
import androidx.room.withTransaction
import com.wpdas.primetv.core.util.Hashing
import com.wpdas.primetv.core.util.Quality
import com.wpdas.primetv.core.util.TextNormalizer
import com.wpdas.primetv.data.local.db.CategoryEntity
import com.wpdas.primetv.data.local.db.ChannelEntity
import com.wpdas.primetv.data.local.db.EpisodeEntity
import com.wpdas.primetv.data.local.db.MovieEntity
import com.wpdas.primetv.data.local.db.PrimeDatabase
import com.wpdas.primetv.data.local.db.SearchIndexEntity
import com.wpdas.primetv.data.local.db.SeriesEntity
import com.wpdas.primetv.data.remote.m3u.M3uClassifier
import com.wpdas.primetv.data.remote.m3u.M3uEntry
import com.wpdas.primetv.data.remote.m3u.M3uItem
import com.wpdas.primetv.data.remote.m3u.M3uKind
import com.wpdas.primetv.data.remote.m3u.M3uParser
import com.wpdas.primetv.data.remote.m3u.SeriesPattern
import com.wpdas.primetv.domain.football.FootballClassifierProvider
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.PlaylistError
import com.wpdas.primetv.domain.model.PlaylistException
import com.wpdas.primetv.domain.model.PlaylistSource
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * Importa de uma lista M3U. O arquivo é baixado uma vez para o cache e depois lido em
 * streaming, uma passada por seção (Ao vivo, Filmes, Séries). Cada seção é lida inteira e
 * gravada numa transação curta, para a UI nunca ver a tabela vazia nem gravações do usuário
 * ficarem presas atrás da leitura.
 */
class M3uImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: PrimeDatabase,
    private val http: OkHttpClient,
    private val epgImporter: EpgImporter,
    private val footballProvider: FootballClassifierProvider,
) : PlaylistImporter {

    private val dao get() = db.catalogDao()
    private val football get() = footballProvider.get()

    override suspend fun import(source: PlaylistSource, section: ContentSection, onProgress: (Int) -> Unit): ImportResult {
        val src = source as PlaylistSource.M3u
        if (section == ContentSection.EPG) return epgImporter.import(src, onProgress)
        val file = ensureDownloaded(src)
        return ImportResult.Imported(
            when (section) {
                ContentSection.LIVE -> importLive(file, onProgress)
                ContentSection.MOVIES -> importMovies(file, onProgress)
                ContentSection.SERIES -> importSeries(file, onProgress)
                ContentSection.EPG -> 0
            },
        )
    }

    /** Baixa a lista para o cache se não houver cópia recente (mesma sincronização). */
    private fun ensureDownloaded(src: PlaylistSource.M3u): File {
        val file = File(context.cacheDir, CACHE_FILE)
        val fresh = file.exists() && file.length() > 0 && System.currentTimeMillis() - file.lastModified() < CACHE_TTL_MS
        if (fresh) return file
        val tmp = File(context.cacheDir, "$CACHE_FILE.part")
        try {
            http.newCall(Request.Builder().url(src.url).get().build()).execute().use { resp ->
                if (resp.code == 401 || resp.code == 403) throw PlaylistException(PlaylistError.InvalidCredentials)
                if (!resp.isSuccessful) throw PlaylistException(PlaylistError.ServerUnreachable)
                resp.body.byteStream().use { input -> tmp.outputStream().use { out -> input.copyTo(out, 64 * 1024) } }
            }
        } catch (e: UnknownHostException) {
            throw PlaylistException(PlaylistError.NoInternet, e)
        } catch (e: SocketTimeoutException) {
            throw PlaylistException(PlaylistError.ServerUnreachable, e)
        } catch (e: IOException) {
            throw PlaylistException(PlaylistError.ServerUnreachable, e)
        }
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
        return file
    }

    private fun entries(file: File): Sequence<M3uEntry> = sequence {
        file.bufferedReader().use { reader ->
            for (item in M3uParser.parse(reader)) {
                if (item is M3uItem.Entry) yield(item.entry)
            }
        }
    }

    private suspend fun importLive(file: File, onProgress: (Int) -> Unit): Int {
        val categories = CategoryCollector(ContentSection.LIVE)
        val collected = Collector<ChannelEntity>(onProgress)
        var index = 0
        entries(file).forEach { entry ->
            if (M3uClassifier.classify(entry) != M3uKind.LIVE) return@forEach
            index++
            val cat = categories.of(entry.group)
            collected.add(
                ChannelEntity(
                    id = "ml-" + Hashing.shortId(entry.url),
                    name = entry.name.trim(),
                    number = entry.channelNumber ?: index,
                    logoUrl = entry.logo,
                    categoryId = cat?.id,
                    categoryName = cat?.name,
                    streamUrl = entry.url,
                    epgChannelId = entry.tvgId,
                    epgKey = entry.tvgId,
                    isAdult = false,
                    isFootball = football.matches(entry.name, cat?.name),
                    searchKey = TextNormalizer.searchKey(entry.name),
                    groupKey = Quality.groupKey(entry.name),
                    qualityRank = Quality.rank(entry.name),
                ),
            )
        }
        db.withTransaction {
            dao.deleteCategories(ContentSection.LIVE.name)
            dao.deleteChannels()
            dao.deleteSearch(SearchType.CHANNEL)
            collected.items.chunked(PrimeDatabase.BATCH_SIZE).forEach { items ->
                dao.insertChannels(items)
                dao.insertSearch(items.map { SearchIndexEntity(title = it.searchKey, type = SearchType.CHANNEL, refId = it.id) })
            }
            dao.insertCategories(categories.all())
        }
        return collected.items.size
    }

    private suspend fun importMovies(file: File, onProgress: (Int) -> Unit): Int {
        val categories = CategoryCollector(ContentSection.MOVIES)
        val collected = Collector<MovieEntity>(onProgress)
        entries(file).forEach { entry ->
            if (M3uClassifier.classify(entry) != M3uKind.MOVIE) return@forEach
            val cat = categories.of(entry.group)
            val ext = entry.url.substringBefore('?').substringAfterLast('/').substringAfterLast('.', "").ifEmpty { null }
            collected.add(
                MovieEntity(
                    id = "mm-" + Hashing.shortId(entry.url),
                    title = entry.name.trim(),
                    posterUrl = entry.logo,
                    categoryId = cat?.id,
                    categoryName = cat?.name,
                    streamUrl = entry.url,
                    containerExt = ext,
                    year = TextNormalizer.extractYear(entry.name),
                    rating = null,
                    tmdbId = null,
                    addedAt = null,
                    isAdult = false,
                    isFootball = football.matches(entry.name, cat?.name),
                    normalizedTitle = TextNormalizer.normalize(entry.name),
                    qualityRank = Quality.rank(entry.name),
                    groupKey = Quality.groupKey(entry.name),
                ),
            )
        }
        db.withTransaction {
            dao.deleteCategories(ContentSection.MOVIES.name)
            dao.deleteMovies()
            dao.deleteSearch(SearchType.MOVIE)
            collected.items.chunked(PrimeDatabase.BATCH_SIZE).forEach { items ->
                dao.insertMovies(items)
                dao.insertSearch(items.map { SearchIndexEntity(title = TextNormalizer.searchKey(it.title), type = SearchType.MOVIE, refId = it.id) })
            }
            dao.insertCategories(categories.all())
        }
        return collected.items.size
    }

    private suspend fun importSeries(file: File, onProgress: (Int) -> Unit): Int {
        val categories = CategoryCollector(ContentSection.SERIES)
        val seriesById = LinkedHashMap<String, SeriesEntity>()
        val episodeCounter = HashMap<String, Int>()
        val episodes = ArrayList<EpisodeEntity>()
        entries(file).forEach { entry ->
            if (M3uClassifier.classify(entry) != M3uKind.SERIES_EPISODE) return@forEach
            val match = SeriesPattern.match(entry.name)
            val seriesTitle = (match?.seriesTitle ?: entry.name).trim()
            val seriesId = "ms-" + Hashing.shortId(TextNormalizer.normalize(seriesTitle))
            val cat = categories.of(entry.group)
            val series = seriesById.getOrPut(seriesId) {
                SeriesEntity(
                    id = seriesId,
                    title = seriesTitle,
                    posterUrl = entry.logo,
                    backdropUrl = null,
                    categoryId = cat?.id,
                    categoryName = cat?.name,
                    plot = null,
                    genre = null,
                    year = TextNormalizer.extractYear(seriesTitle),
                    rating = null,
                    tmdbId = null,
                    episodesLoaded = true,
                    isFootball = football.matches(seriesTitle, cat?.name),
                    normalizedTitle = TextNormalizer.normalize(seriesTitle),
                ).also { onProgress(seriesById.size + 1) }
            }
            if (series.posterUrl == null && entry.logo != null) seriesById[seriesId] = series.copy(posterUrl = entry.logo)
            val number = match?.episode ?: episodeCounter.merge(seriesId, 1, Int::plus)!!
            val ext = entry.url.substringBefore('?').substringAfterLast('/').substringAfterLast('.', "").ifEmpty { null }
            episodes += EpisodeEntity(
                id = "me-" + Hashing.shortId(entry.url),
                seriesId = seriesId,
                season = match?.season ?: 1,
                number = number,
                title = entry.name.trim(),
                streamUrl = entry.url,
                containerExt = ext,
                durationSec = null,
                posterUrl = entry.logo,
                plot = null,
            )
        }
        db.withTransaction {
            dao.deleteCategories(ContentSection.SERIES.name)
            dao.deleteSeries()
            dao.deleteEpisodes()
            dao.deleteSearch(SearchType.SERIES)
            episodes.chunked(PrimeDatabase.BATCH_SIZE).forEach { dao.insertEpisodes(it) }
            seriesById.values.chunked(PrimeDatabase.BATCH_SIZE).forEach { chunk ->
                dao.insertSeries(chunk)
                dao.insertSearch(chunk.map { SearchIndexEntity(title = TextNormalizer.searchKey(it.title), type = SearchType.SERIES, refId = it.id) })
            }
            dao.insertCategories(categories.all())
        }
        return seriesById.size
    }

    /** Categorias vêm do `group-title`; ids estáveis por nome normalizado, ordem de aparição. */
    private class CategoryCollector(private val section: ContentSection) {
        private val map = LinkedHashMap<String, CategoryEntity>()

        fun of(group: String?): CategoryEntity? {
            val name = group?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val key = TextNormalizer.searchKey(name)
            return map.getOrPut(key) {
                CategoryEntity(
                    id = "${section.name.lowercase()}:" + Hashing.shortId(key),
                    section = section.name,
                    name = name,
                    sortOrder = map.size,
                )
            }
        }

        fun all(): List<CategoryEntity> = map.values.toList()
    }

    private companion object {
        const val CACHE_FILE = "playlist.m3u"
        const val CACHE_TTL_MS = 10 * 60 * 1000L
    }
}
