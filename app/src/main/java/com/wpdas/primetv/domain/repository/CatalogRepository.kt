package com.wpdas.primetv.domain.repository

import com.wpdas.primetv.domain.model.Category
import com.wpdas.primetv.domain.model.Channel
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.EpgProgram
import com.wpdas.primetv.domain.model.Episode
import com.wpdas.primetv.domain.model.Movie
import com.wpdas.primetv.domain.model.NowNext
import com.wpdas.primetv.domain.model.Series
import kotlinx.coroutines.flow.Flow

/** Filtro da lista de canais da tela Ao vivo. */
sealed interface ChannelFilter {
    data object All : ChannelFilter
    data object Football : ChannelFilter
    data class Category(val id: String) : ChannelFilter
    data class Favorites(val ids: List<String>) : ChannelFilter
}

data class SearchResults(
    val channels: List<Channel>,
    val movies: List<Movie>,
    val series: List<Series>,
) {
    val isEmpty: Boolean get() = channels.isEmpty() && movies.isEmpty() && series.isEmpty()
    val total: Int get() = channels.size + movies.size + series.size
}

data class CategoryCount(val categoryId: String?, val count: Int)

/** Leitura do catálogo importado. Tudo via Flow para a UI reagir a sincronizações. */
interface CatalogRepository {
    fun categories(section: ContentSection): Flow<List<Category>>
    fun categoryCounts(section: ContentSection): Flow<List<CategoryCount>>

    fun channels(filter: ChannelFilter): Flow<List<Channel>>
    fun channel(id: String): Flow<Channel?>
    fun footballChannels(limit: Int): Flow<List<Channel>>
    fun footballChannelCount(): Flow<Int>
    /** Filmes + séries marcados como futebol. */
    fun footballVodCount(): Flow<Int>

    fun movies(categoryId: String?, limit: Int): Flow<List<Movie>>
    fun recentMovies(limit: Int): Flow<List<Movie>>
    fun footballMovies(limit: Int): Flow<List<Movie>>
    fun movie(id: String): Flow<Movie?>

    fun series(categoryId: String?, limit: Int): Flow<List<Series>>
    fun footballSeries(limit: Int): Flow<List<Series>>
    fun seriesById(id: String): Flow<Series?>
    fun episodes(seriesId: String): Flow<List<Episode>>
    /** Busca os episódios no servidor se ainda não estiverem no banco (Xtream). */
    suspend fun ensureEpisodes(seriesId: String)

    /** Programas no ar agora para os canais do filtro, indexados por [Channel.epgKey]. */
    suspend fun nowPlaying(filter: ChannelFilter, now: Long): Map<String, EpgProgram>
    fun nowNext(channel: Channel, now: Long, upcoming: Int): Flow<NowNext>

    fun moviesByIds(ids: List<String>): Flow<List<Movie>>
    fun seriesByIds(ids: List<String>): Flow<List<Series>>
    fun channelsByIds(ids: List<String>): Flow<List<Channel>>
    suspend fun movieOnce(id: String): Movie?
    suspend fun seriesOnce(id: String): Series?
    suspend fun channelOnce(id: String): Channel?
    suspend fun episodesOnce(seriesId: String): List<Episode>
    suspend fun episodeOnce(id: String): Episode?
    suspend fun channelsOnce(filter: ChannelFilter): List<Channel>
    /** Todas as variantes de qualidade de um canal, da melhor para a pior. */
    suspend fun channelVariants(groupKey: String): List<Channel>

    /** Busca por prefixo no índice FTS. Deve responder em menos de 100 ms. */
    suspend fun search(query: String, limit: Int): SearchResults
}
