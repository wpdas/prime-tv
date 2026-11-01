package com.wpdas.primetv.data.repository

import com.wpdas.primetv.domain.model.CategoryRow
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.ContinueWatchingItem
import com.wpdas.primetv.domain.model.FootballEvent
import com.wpdas.primetv.domain.model.HomeTab
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.model.MediaType
import com.wpdas.primetv.domain.model.SyncState
import com.wpdas.primetv.domain.repository.CatalogRepository
import com.wpdas.primetv.domain.repository.ChannelFilter
import com.wpdas.primetv.domain.repository.HomeRepository
import com.wpdas.primetv.domain.repository.LibraryRepository
import com.wpdas.primetv.domain.repository.RecommendationRepository
import com.wpdas.primetv.domain.update.UpdateRepository
import kotlinx.coroutines.flow.onStart
import com.wpdas.primetv.domain.sync.CatalogSync
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Início com dados reais do Room. Destaque: itens recentes da lista (o TMDB entra na
 * fase 5). Continuar assistindo e Minha lista persistidos chegam na fase 4.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val catalog: CatalogRepository,
    private val catalogSync: CatalogSync,
    private val library: LibraryRepository,
    private val recommendations: RecommendationRepository,
    private val updates: UpdateRepository,
) : HomeRepository {

    private val myList: Flow<Set<String>> get() = library.favoriteIds()

    /** Recomendações do TMDB que existem na lista; sem chave ou sem correspondência, itens recentes. */
    override fun featured(): Flow<List<MediaItem>> {
        val fallback = combine(catalog.recentMovies(FEATURED_MOVIES), catalog.series(null, FEATURED_SERIES)) { movies, series ->
            movies.map { it.toMediaItem().copy(badgeKey = "home.badge_recent") } +
                series.map { it.toMediaItem().copy(badgeKey = "home.badge_series") }
        }
        val recommended = if (recommendations.isAvailable) {
            recommendations.featured(FEATURED_TMDB).onStart { emit(emptyList()) }
        } else {
            flowOf(emptyList())
        }
        return combine(recommended, fallback, myList) { tmdb, recent, ids ->
            (tmdb.ifEmpty { recent }).map { it.copy(inMyList = it.id in ids) }
        }
    }

    override fun continueWatching(): Flow<List<ContinueWatchingItem>> = library.continueWatching(CONTINUE_ITEMS)

    override fun footballNow(): Flow<List<FootballEvent>> = catalog.footballChannels(FOOTBALL_NOW).map { channels ->
        val now = System.currentTimeMillis()
        val airing = runCatching { catalog.nowPlaying(ChannelFilter.Football, now) }.getOrDefault(emptyMap())
        channels.map { ch ->
            FootballEvent(
                id = ch.id,
                channelName = ch.name,
                title = ch.epgKey?.let { airing[it]?.title } ?: ch.categoryName ?: ch.name,
            )
        }
    }

    override fun categoryRows(tab: HomeTab): Flow<List<CategoryRow>> = when (tab) {
        HomeTab.HOME -> combine(
            catalog.recentMovies(ROW_ITEMS).map { listOf(CategoryRow("recent-movies", "Adicionados recentemente", it.map { m -> m.toMediaItem() }, ContentSection.MOVIES, null)) },
            sectionRows(ContentSection.MOVIES, maxRows = 4),
            sectionRows(ContentSection.SERIES, maxRows = 3),
        ) { recent, movies, series -> (recent + movies + series).filter { it.items.isNotEmpty() } }
        HomeTab.MOVIES -> sectionRows(ContentSection.MOVIES, maxRows = Int.MAX_VALUE)
        HomeTab.SERIES -> sectionRows(ContentSection.SERIES, maxRows = Int.MAX_VALUE)
        HomeTab.FOOTBALL -> combine(
            catalog.footballChannels(ROW_ITEMS).map { list -> CategoryRow("football-live", "Canais de futebol", list.map { it.toMediaItem() }, ContentSection.LIVE, FOOTBALL_CATEGORY) },
            catalog.footballMovies(ROW_ITEMS).map { list -> CategoryRow("football-vod", "Jogos e programas", list.map { it.toMediaItem() }) },
            catalog.footballSeries(ROW_ITEMS).map { list -> CategoryRow("football-series", "Séries de futebol", list.map { it.toMediaItem() }) },
        ) { a, b, c -> listOf(a, b, c).filter { it.items.isNotEmpty() } }
        HomeTab.MY_LIST -> myListRows()
        HomeTab.LIVE -> flowOf(emptyList())
    }.combine(myList) { rows, ids -> rows.map { row -> row.copy(items = row.items.map { it.copy(inMyList = it.id in ids) }) } }

    /** Uma linha por categoria da seção, limitada a [maxRows] categorias e [ROW_ITEMS] itens. */
    private fun sectionRows(section: ContentSection, maxRows: Int): Flow<List<CategoryRow>> =
        catalog.categories(section).flatMapLatest { categories ->
            val chosen = categories.take(maxRows)
            if (chosen.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val flows = chosen.map { cat ->
                when (section) {
                    ContentSection.MOVIES -> catalog.movies(cat.id, ROW_ITEMS).map { list -> CategoryRow(cat.id, cat.name, list.map { it.toMediaItem() }, section, cat.id) }
                    else -> catalog.series(cat.id, ROW_ITEMS).map { list -> CategoryRow(cat.id, cat.name, list.map { it.toMediaItem() }, section, cat.id) }
                }
            }
            combine(flows) { rows -> rows.filter { it.items.isNotEmpty() } }
        }

    override fun backgroundUpdate(): Flow<String?> = catalogSync.status.map { status ->
        status.entries.firstOrNull { it.value is SyncState.Running }?.key?.labelKey
    }

    /** Minha lista: uma linha por tipo, na ordem em que foram favoritados. */
    private fun myListRows(): Flow<List<CategoryRow>> = library.favoriteIds().flatMapLatest { ids ->
        if (ids.isEmpty()) return@flatMapLatest flowOf(emptyList())
        combine(
            catalog.moviesByIds(ids.filter { it.startsWith("xm-") || it.startsWith("mm-") }),
            catalog.seriesByIds(ids.filter { it.startsWith("xs-") || it.startsWith("ms-") }),
            catalog.channelsByIds(ids.filter { it.startsWith("xl-") || it.startsWith("ml-") }),
        ) { movies, series, channels ->
            listOf(
                CategoryRow("mylist-movies", "Filmes", movies.map { it.toMediaItem(true) }),
                CategoryRow("mylist-series", "Séries", series.map { it.toMediaItem(true) }),
                CategoryRow("mylist-channels", "Canais", channels.map { it.toMediaItem(true) }),
            ).filter { it.items.isNotEmpty() }
        }
    }

    override fun pendingUpdateVersion(): Flow<String?> = updates.pending.map { it?.versionName }

    override suspend fun toggleMyList(itemId: String) {
        val type = when {
            itemId.startsWith("xs-") || itemId.startsWith("ms-") -> MediaType.SERIES
            itemId.startsWith("xl-") || itemId.startsWith("ml-") -> MediaType.CHANNEL
            else -> MediaType.MOVIE
        }
        library.toggleFavorite(itemId, type)
    }

    companion object {
        private const val FEATURED_MOVIES = 4
        private const val FEATURED_TMDB = 8
        private const val FEATURED_SERIES = 2
        private const val FOOTBALL_NOW = 10
        private const val ROW_ITEMS = 24
        private const val CONTINUE_ITEMS = 12
        /** Id virtual usado pela grade para listar só canais de futebol. */
        const val FOOTBALL_CATEGORY = "football"
    }
}
