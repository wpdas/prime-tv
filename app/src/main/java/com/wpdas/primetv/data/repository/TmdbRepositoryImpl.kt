package com.wpdas.primetv.data.repository

import com.wpdas.primetv.core.di.IoDispatcher
import com.wpdas.primetv.data.local.db.MovieEntity
import com.wpdas.primetv.data.local.db.PrimeDatabase
import com.wpdas.primetv.data.local.db.SeriesEntity
import com.wpdas.primetv.data.local.db.TmdbCacheEntity
import com.wpdas.primetv.data.local.db.TmdbDetailsEntity
import com.wpdas.primetv.data.local.db.TmdbMatchEntity
import com.wpdas.primetv.data.remote.tmdb.TmdbApi
import com.wpdas.primetv.data.remote.tmdb.TmdbApiKey
import com.wpdas.primetv.data.remote.tmdb.TmdbDetails
import com.wpdas.primetv.data.remote.tmdb.TmdbListItem
import com.wpdas.primetv.data.remote.tmdb.TmdbMatcher
import com.wpdas.primetv.data.remote.tmdb.TmdbPage
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.model.MediaType
import com.wpdas.primetv.domain.model.TmdbInfo
import com.wpdas.primetv.domain.repository.FeaturedKind
import com.wpdas.primetv.domain.repository.RecommendationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TMDB com cache no Room: listas por ~12 h, detalhes e correspondências por ~7 dias.
 * Nada do TMDB aparece se o título não existir na lista do usuário.
 */
@Singleton
class TmdbRepositoryImpl @Inject constructor(
    private val api: TmdbApi,
    private val db: PrimeDatabase,
    private val json: Json,
    @TmdbApiKey private val apiKey: String,
    @IoDispatcher private val io: CoroutineDispatcher,
) : RecommendationRepository {

    private val tmdb get() = db.tmdbDao()
    private val catalog get() = db.catalogDao()
    private val genreLock = Mutex()
    private var genreNames: Map<Int, String>? = null

    override val isAvailable: Boolean get() = apiKey.isNotBlank()

    override fun featured(limit: Int, kind: FeaturedKind): Flow<List<MediaItem>> = flow {
        if (!isAvailable) {
            emit(emptyList())
            return@flow
        }
        val sources = when (kind) {
            FeaturedKind.GENERAL -> listOf(
                "trending" to "home.badge_trending",
                "popular_movies" to "home.badge_popular",
                "now_playing" to "home.badge_now_playing",
                "popular_tv" to "home.badge_popular",
            )
            FeaturedKind.KIDS -> listOf("kids_tv" to "home.badge_kids", "kids_movies" to "home.badge_kids")
            FeaturedKind.ANIME -> listOf("anime_tv" to "home.badge_anime", "anime_movies" to "home.badge_anime")
            FeaturedKind.NOVELAS -> listOf("novelas_tv" to "home.badge_novelas")
        }
        val genres = genres()
        val out = ArrayList<MediaItem>()
        val seen = HashSet<String>()
        for ((key, badge) in sources) {
            val page = cachedPage(key) ?: continue
            for (remote in page.results) {
                if (out.size >= limit) break
                val type = when (remote.mediaType ?: if (key.endsWith("_tv")) "tv" else "movie") {
                    "movie" -> MediaType.MOVIE
                    "tv" -> MediaType.SERIES
                    else -> continue
                }
                val local = findLocal(remote, type) ?: continue
                if (!seen.add(local.id)) continue
                out += local.copy(
                    backdropUrl = TmdbApi.backdrop(remote.backdropPath) ?: local.backdropUrl,
                    posterUrl = local.posterUrl ?: TmdbApi.poster(remote.posterPath),
                    synopsis = remote.overview?.takeIf { it.isNotBlank() } ?: local.synopsis,
                    rating = remote.voteAverage?.takeIf { it > 0 } ?: local.rating,
                    genres = remote.genreIds.mapNotNull { genres[it] }.take(3).ifEmpty { local.genres },
                    year = remote.year ?: local.year,
                    badgeKey = badge,
                    tmdbId = remote.id,
                )
                // Lembra a correspondência para os Detalhes não precisarem buscar.
                tmdb.putMatch(TmdbMatchEntity(local.id, remote.id, type.key, System.currentTimeMillis()))
            }
            if (out.size >= limit) break
        }
        emit(out)
    }.flowOn(io)

    override suspend fun enrich(item: MediaItem): TmdbInfo? = withContext(io) {
        if (!isAvailable || item.type == MediaType.CHANNEL) return@withContext null
        val tmdbId = item.tmdbId ?: resolveMatch(item) ?: return@withContext null
        details(tmdbId, item.type)
    }

    // ------------------------------------------------------------------ correspondência

    private suspend fun resolveMatch(item: MediaItem): Int? {
        val cached = tmdb.match(item.id)
        if (cached != null && System.currentTimeMillis() - cached.matchedAt < MATCH_TTL_MS) return cached.tmdbId
        val found = runCatching {
            val results = if (item.type == MediaType.MOVIE) api.searchMovie(item.title, item.year).results
            else api.searchTv(item.title, item.year).results
            val best = TmdbMatcher.pickRemote(item.title, item.year, results)
                ?: if (item.year != null) {
                    // Ano da lista pode estar errado: tenta sem ano.
                    val loose = if (item.type == MediaType.MOVIE) api.searchMovie(item.title).results else api.searchTv(item.title).results
                    TmdbMatcher.pickRemote(item.title, null, loose)
                } else null
            best?.id
        }.getOrNull()
        tmdb.putMatch(TmdbMatchEntity(item.id, found, item.type.key, System.currentTimeMillis()))
        return found
    }

    private suspend fun findLocal(remote: TmdbListItem, type: MediaType): MediaItem? {
        val keys = TmdbMatcher.keysFor(remote.displayTitle, remote.original)
        if (keys.isEmpty()) return null
        return when (type) {
            MediaType.MOVIE -> {
                val rows = catalog.moviesByNormalizedTitles(keys) + looseRows(keys) { catalog.moviesByNormalizedTitles(it) }
                val pick = TmdbMatcher.pick(rows.distinctBy { it.id }.map { TmdbMatcher.Candidate(it.id, it.normalizedTitle, it.year) }, remote.year) ?: return null
                rows.first { it.id == pick.id }.toDomain().toMediaItem()
            }
            MediaType.SERIES -> {
                val rows = catalog.seriesByNormalizedTitles(keys) + looseRows(keys) { catalog.seriesByNormalizedTitles(it) }
                val pick = TmdbMatcher.pick(rows.distinctBy { it.id }.map { TmdbMatcher.Candidate(it.id, it.normalizedTitle, it.year) }, remote.year) ?: return null
                rows.first { it.id == pick.id }.toDomain().toMediaItem()
            }
            MediaType.CHANNEL -> null
        }
    }

    /** Segunda tentativa: títulos da lista com numeração extra ("Apocalipse Z 2 Novo Mundo"). */
    private suspend fun <T> looseRows(keys: List<String>, query: suspend (List<String>) -> List<T>): List<T> {
        val extra = keys.flatMap { k -> listOf("$k 2", "$k 3", "$k 1") }.distinct()
        return query(extra)
    }

    // ------------------------------------------------------------------ detalhes e cache

    private suspend fun details(tmdbId: Int, type: MediaType): TmdbInfo? {
        val cached = tmdb.details(tmdbId, type.key)
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < DETAILS_TTL_MS) return cached.toDomain(type)
        val remote: TmdbDetails = runCatching {
            if (type == MediaType.MOVIE) api.movie(tmdbId) else api.tv(tmdbId)
        }.getOrNull() ?: return cached?.toDomain(type)
        val entity = TmdbDetailsEntity(
            tmdbId = tmdbId,
            type = type.key,
            title = remote.displayTitle,
            overview = remote.overview?.takeIf { it.isNotBlank() },
            backdropPath = remote.backdropPath,
            posterPath = remote.posterPath,
            genres = remote.genres.joinToString("|") { it.name }.ifEmpty { null },
            rating = remote.voteAverage?.takeIf { it > 0 },
            voteCount = remote.voteCount,
            year = remote.year,
            seasons = remote.numberOfSeasons,
            episodes = remote.numberOfEpisodes,
            trailerKey = remote.trailerKey,
            fetchedAt = System.currentTimeMillis(),
        )
        tmdb.putDetails(entity)
        return entity.toDomain(type)
    }

    private suspend fun cachedPage(key: String): TmdbPage? {
        val cached = tmdb.cache(key)
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < LIST_TTL_MS) {
            return runCatching { json.decodeFromString(TmdbPage.serializer(), cached.json) }.getOrNull()
        }
        val fresh = runCatching {
            when (key) {
                "trending" -> api.trending()
                "popular_movies" -> api.popularMovies()
                "now_playing" -> api.nowPlaying()
                "kids_tv" -> api.discoverTv(genres = "10762")
                "kids_movies" -> api.discoverMovie(genres = "16|10751")
                "anime_tv" -> api.discoverTv(genres = "16", country = "JP")
                "anime_movies" -> api.discoverMovie(genres = "16", country = "JP")
                "novelas_tv" -> api.discoverTv(genres = "10766", country = "BR", minVotes = 5)
                else -> api.popularTv()
            }
        }.getOrNull()
        if (fresh != null) {
            tmdb.putCache(TmdbCacheEntity(key, json.encodeToString(TmdbPage.serializer(), fresh), System.currentTimeMillis()))
            return fresh
        }
        // Sem rede: usa o que houver, mesmo vencido.
        return cached?.let { runCatching { json.decodeFromString(TmdbPage.serializer(), it.json) }.getOrNull() }
    }

    private suspend fun genres(): Map<Int, String> = genreLock.withLock {
        genreNames?.let { return it }
        val cached = tmdb.cache("genres")
        val map: Map<Int, String> = if (cached != null && System.currentTimeMillis() - cached.fetchedAt < DETAILS_TTL_MS) {
            runCatching { json.decodeFromString(GenreMap.serializer(), cached.json).map }.getOrDefault(emptyMap())
        } else {
            val fetched = runCatching {
                (api.movieGenres().genres + api.tvGenres().genres).associate { it.id to it.name }
            }.getOrNull()
            if (fetched != null) {
                tmdb.putCache(TmdbCacheEntity("genres", json.encodeToString(GenreMap.serializer(), GenreMap(fetched)), System.currentTimeMillis()))
                fetched
            } else {
                cached?.let { runCatching { json.decodeFromString(GenreMap.serializer(), it.json).map }.getOrNull() }.orEmpty()
            }
        }
        genreNames = map
        map
    }

    @kotlinx.serialization.Serializable
    private data class GenreMap(val map: Map<Int, String>)

    private fun TmdbDetailsEntity.toDomain(type: MediaType) = TmdbInfo(
        tmdbId = tmdbId,
        type = type,
        title = title,
        overview = overview,
        backdropUrl = TmdbApi.backdrop(backdropPath),
        posterUrl = TmdbApi.poster(posterPath),
        genres = genres?.split('|')?.filter { it.isNotBlank() }.orEmpty(),
        rating = rating,
        voteCount = voteCount,
        year = year,
        seasons = seasons,
        episodes = episodes,
        trailerYoutubeKey = trailerKey,
    )

    @Suppress("unused")
    private fun MovieEntity.key() = normalizedTitle
    @Suppress("unused")
    private fun SeriesEntity.key() = normalizedTitle

    private companion object {
        const val LIST_TTL_MS = 12 * 60 * 60 * 1000L
        const val DETAILS_TTL_MS = 7 * 24 * 60 * 60 * 1000L
        const val MATCH_TTL_MS = 7 * 24 * 60 * 60 * 1000L
    }
}
