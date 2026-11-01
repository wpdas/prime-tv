package com.wpdas.primetv.data.repository

import com.wpdas.primetv.domain.model.CategoryRow
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.ContinueWatchingItem
import com.wpdas.primetv.domain.model.FootballEvent
import com.wpdas.primetv.domain.model.HomeTab
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.model.SyncState
import com.wpdas.primetv.domain.repository.CatalogRepository
import com.wpdas.primetv.domain.repository.ChannelFilter
import com.wpdas.primetv.domain.repository.HomeRepository
import com.wpdas.primetv.domain.sync.CatalogSync
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
) : HomeRepository {

    private val myList = MutableStateFlow<Set<String>>(emptySet())

    override fun featured(): Flow<List<MediaItem>> =
        combine(catalog.recentMovies(FEATURED_MOVIES), catalog.series(null, FEATURED_SERIES), myList) { movies, series, ids ->
            val items = movies.map { it.toMediaItem().copy(badgeKey = "home.badge_recent") } +
                series.map { it.toMediaItem().copy(badgeKey = "home.badge_series") }
            items.map { it.copy(inMyList = it.id in ids) }
        }

    override fun continueWatching(): Flow<List<ContinueWatchingItem>> = flowOf(emptyList())

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
        HomeTab.MY_LIST -> flowOf(emptyList())
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

    override suspend fun toggleMyList(itemId: String) {
        myList.update { if (itemId in it) it - itemId else it + itemId }
    }

    companion object {
        private const val FEATURED_MOVIES = 4
        private const val FEATURED_SERIES = 2
        private const val FOOTBALL_NOW = 10
        private const val ROW_ITEMS = 24
        /** Id virtual usado pela grade para listar só canais de futebol. */
        const val FOOTBALL_CATEGORY = "football"
    }
}
