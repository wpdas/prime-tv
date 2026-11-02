package com.wpdas.primetv.data.sync

import androidx.room.withTransaction
import com.wpdas.primetv.core.util.Quality
import com.wpdas.primetv.core.util.TextNormalizer
import com.wpdas.primetv.data.local.db.CategoryEntity
import com.wpdas.primetv.data.local.db.ChannelEntity
import com.wpdas.primetv.data.local.db.MovieEntity
import com.wpdas.primetv.data.local.db.PrimeDatabase
import com.wpdas.primetv.data.local.db.SearchIndexEntity
import com.wpdas.primetv.data.local.db.SeriesEntity
import com.wpdas.primetv.data.remote.xtream.XtreamCategoryDto
import com.wpdas.primetv.data.remote.xtream.XtreamClient
import com.wpdas.primetv.data.remote.xtream.blankToNull
import com.wpdas.primetv.data.remote.xtream.contentOrNull
import com.wpdas.primetv.data.remote.xtream.toBoolLenient
import com.wpdas.primetv.data.remote.xtream.toDoubleLenient
import com.wpdas.primetv.data.remote.xtream.toIntLenient
import com.wpdas.primetv.data.remote.xtream.toLongLenient
import com.wpdas.primetv.data.remote.xtream.yearLenient
import com.wpdas.primetv.domain.football.FootballClassifierProvider
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.PlaylistSource
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

/**
 * Importa do Xtream. Cada seção roda numa única transação: apaga a anterior e insere em
 * lotes enquanto o JSON é lido em streaming. Se falhar no meio, o rollback preserva os
 * dados antigos. Episódios de série são buscados sob demanda na tela de Detalhes.
 */
class XtreamImporter @Inject constructor(
    private val db: PrimeDatabase,
    private val client: XtreamClient,
    private val epgImporter: EpgImporter,
    private val footballProvider: FootballClassifierProvider,
) : PlaylistImporter {

    private val dao get() = db.catalogDao()
    private val football get() = footballProvider.get()

    override suspend fun import(source: PlaylistSource, section: ContentSection, onProgress: (Int) -> Unit): ImportResult {
        val src = source as PlaylistSource.Xtream
        return when (section) {
            ContentSection.LIVE -> ImportResult.Imported(importLive(src, onProgress))
            ContentSection.MOVIES -> ImportResult.Imported(importMovies(src, onProgress))
            ContentSection.SERIES -> ImportResult.Imported(importSeries(src, onProgress))
            ContentSection.EPG -> epgImporter.import(src, onProgress)
        }
    }

    private suspend fun importLive(src: PlaylistSource.Xtream, onProgress: (Int) -> Unit): Int {
        val categories = client.liveCategories(src).toEntities(ContentSection.LIVE)
        val names = categories.associate { it.id to it.name }
        var count = 0
        db.withTransaction {
            dao.deleteCategories(ContentSection.LIVE.name)
            dao.deleteChannels()
            dao.deleteSearch(SearchType.CHANNEL)
            dao.insertCategories(categories)
            val batch = Batch<ChannelEntity>(PrimeDatabase.BATCH_SIZE) { items ->
                dao.insertChannels(items)
                dao.insertSearch(items.map { SearchIndexEntity(title = it.searchKey, type = SearchType.CHANNEL, refId = it.id) })
                count += items.size
                onProgress(count)
            }
            client.liveStreams(src) { seq ->
                seq.forEachIndexed { index, dto ->
                    val id = dto.streamId.blankToNull() ?: return@forEachIndexed
                    val name = dto.name.blankToNull() ?: return@forEachIndexed
                    val catId = dto.categoryId.blankToNull()?.let { categoryId(ContentSection.LIVE, it) }
                    batch.add(
                        ChannelEntity(
                            id = "xl-$id",
                            name = name.trim(),
                            number = dto.num.toIntLenient() ?: (index + 1),
                            logoUrl = dto.streamIcon.blankToNull(),
                            categoryId = catId,
                            categoryName = catId?.let(names::get),
                            streamUrl = XtreamClient.liveStreamUrl(src, id),
                            epgChannelId = dto.epgChannelId.blankToNull(),
                            epgKey = dto.epgChannelId.blankToNull(),
                            isAdult = dto.isAdult.toBoolLenient(),
                            isFootball = football.matches(name, catId?.let(names::get)),
                            searchKey = TextNormalizer.searchKey(name),
                            groupKey = Quality.groupKey(name),
                            qualityRank = Quality.rank(name),
                        ),
                    )
                }
            }
            batch.flush()
        }
        return count
    }

    private suspend fun importMovies(src: PlaylistSource.Xtream, onProgress: (Int) -> Unit): Int {
        val categories = client.vodCategories(src).toEntities(ContentSection.MOVIES)
        val names = categories.associate { it.id to it.name }
        var count = 0
        db.withTransaction {
            dao.deleteCategories(ContentSection.MOVIES.name)
            dao.deleteMovies()
            dao.deleteSearch(SearchType.MOVIE)
            dao.insertCategories(categories)
            val batch = Batch<MovieEntity>(PrimeDatabase.BATCH_SIZE) { items ->
                dao.insertMovies(items)
                dao.insertSearch(items.map { SearchIndexEntity(title = TextNormalizer.searchKey(it.title), type = SearchType.MOVIE, refId = it.id) })
                count += items.size
                onProgress(count)
            }
            client.vodStreams(src) { seq ->
                seq.forEach { dto ->
                    val id = dto.streamId.blankToNull() ?: return@forEach
                    val name = dto.name.blankToNull() ?: return@forEach
                    val catId = dto.categoryId.blankToNull()?.let { categoryId(ContentSection.MOVIES, it) }
                    batch.add(
                        MovieEntity(
                            id = "xm-$id",
                            title = name.trim(),
                            posterUrl = dto.streamIcon.blankToNull(),
                            categoryId = catId,
                            categoryName = catId?.let(names::get),
                            streamUrl = XtreamClient.movieStreamUrl(src, id, dto.containerExtension.blankToNull()),
                            containerExt = dto.containerExtension.blankToNull(),
                            year = dto.year.yearLenient() ?: TextNormalizer.extractYear(name),
                            rating = dto.rating.toDoubleLenient()?.takeIf { it > 0 },
                            tmdbId = dto.tmdbId.toIntLenient()?.takeIf { it > 0 },
                            addedAt = dto.added.toLongLenient(),
                            isAdult = dto.isAdult.toBoolLenient(),
                            isFootball = football.matches(name, catId?.let(names::get)),
                            normalizedTitle = TextNormalizer.normalize(name),
                            qualityRank = Quality.rank(name),
                            groupKey = Quality.groupKey(name),
                        ),
                    )
                }
            }
            batch.flush()
        }
        return count
    }

    private suspend fun importSeries(src: PlaylistSource.Xtream, onProgress: (Int) -> Unit): Int {
        val categories = client.seriesCategories(src).toEntities(ContentSection.SERIES)
        val names = categories.associate { it.id to it.name }
        var count = 0
        db.withTransaction {
            dao.deleteCategories(ContentSection.SERIES.name)
            dao.deleteSeries()
            dao.deleteEpisodes()
            dao.deleteSearch(SearchType.SERIES)
            dao.insertCategories(categories)
            val batch = Batch<SeriesEntity>(PrimeDatabase.BATCH_SIZE) { items ->
                dao.insertSeries(items)
                dao.insertSearch(items.map { SearchIndexEntity(title = TextNormalizer.searchKey(it.title), type = SearchType.SERIES, refId = it.id) })
                count += items.size
                onProgress(count)
            }
            client.series(src) { seq ->
                seq.forEach { dto ->
                    val id = dto.seriesId.blankToNull() ?: return@forEach
                    val name = dto.name.blankToNull() ?: return@forEach
                    val catId = dto.categoryId.blankToNull()?.let { categoryId(ContentSection.SERIES, it) }
                    val backdrop = when (val b = dto.backdropPath) {
                        is JsonArray -> (b.firstOrNull() as? JsonPrimitive).contentOrNull()
                        is JsonPrimitive -> b.contentOrNull()
                        else -> null
                    }
                    batch.add(
                        SeriesEntity(
                            id = "xs-$id",
                            title = name.trim(),
                            posterUrl = dto.cover.blankToNull(),
                            backdropUrl = backdrop.blankToNull(),
                            categoryId = catId,
                            categoryName = catId?.let(names::get),
                            plot = dto.plot.blankToNull(),
                            genre = dto.genre.blankToNull(),
                            year = dto.releaseDate.yearLenient() ?: TextNormalizer.extractYear(name),
                            rating = dto.rating.toDoubleLenient()?.takeIf { it > 0 },
                            tmdbId = dto.tmdb.toIntLenient()?.takeIf { it > 0 },
                            episodesLoaded = false,
                            isFootball = football.matches(name, catId?.let(names::get)),
                            normalizedTitle = TextNormalizer.normalize(name),
                        ),
                    )
                }
            }
            batch.flush()
        }
        return count
    }

    private fun List<XtreamCategoryDto>.toEntities(section: ContentSection): List<CategoryEntity> =
        mapIndexedNotNull { index, dto ->
            val id = dto.categoryId.blankToNull() ?: return@mapIndexedNotNull null
            CategoryEntity(
                id = categoryId(section, id),
                section = section.name,
                name = dto.categoryName.blankToNull()?.trim() ?: id,
                sortOrder = index,
            )
        }

    private fun categoryId(section: ContentSection, raw: String) = "${section.name.lowercase()}:$raw"
}

/** Acumula itens e despeja em lotes no [sink]; chame [flush] no fim. */
internal class Batch<T>(private val size: Int, private val sink: suspend (List<T>) -> Unit) {
    private val items = ArrayList<T>(size)

    suspend fun add(item: T) {
        items += item
        if (items.size >= size) flush()
    }

    suspend fun flush() {
        if (items.isEmpty()) return
        val chunk = ArrayList(items)
        items.clear()
        sink(chunk)
    }
}
