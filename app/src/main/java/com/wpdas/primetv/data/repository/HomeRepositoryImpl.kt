package com.wpdas.primetv.data.repository

import com.wpdas.primetv.domain.model.CategoryRow
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.ContinueWatchingItem
import com.wpdas.primetv.core.util.TextNormalizer
import com.wpdas.primetv.domain.catalog.TabKeywords
import com.wpdas.primetv.domain.repository.FeaturedKind
import com.wpdas.primetv.domain.football.LeagueNames
import com.wpdas.primetv.domain.football.MatchSchedule
import com.wpdas.primetv.domain.football.MatchStatus
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
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

    override fun featured(tab: HomeTab): Flow<List<MediaItem>> = when (tab) {
        HomeTab.KIDS, HomeTab.ANIME, HomeTab.NOVELAS -> tabFeatured(tab)
        else -> generalFeatured()
    }

    /**
     * Destaque de Kids/Anime/Novelas: em Anime, primeiro os fixos que a lista tiver (Dragon Ball e Naruto/Boruto mais
     * recentes, One Piece); depois os populares do TMDB no contexto que existem na lista; o resto são títulos
     * das categorias da aba, sorteados com semente do dia (mudam a cada dia, não a cada abertura).
     */
    private fun tabFeatured(tab: HomeTab): Flow<List<MediaItem>> {
        val anime = tab == HomeTab.ANIME
        val (kind, badge) = when (tab) {
            HomeTab.ANIME -> FeaturedKind.ANIME to "home.badge_anime"
            HomeTab.NOVELAS -> FeaturedKind.NOVELAS to "home.badge_novelas"
            else -> FeaturedKind.KIDS to "home.badge_kids"
        }
        val recommended = if (recommendations.isAvailable) {
            recommendations.featured(FEATURED_TMDB, kind).onStart { emit(emptyList()) }
        } else {
            flowOf(emptyList())
        }
        val pinned = if (anime) flow { emit(pinnedAnime()) } else flowOf(emptyList())
        return combine(pinned, recommended, tabPool(tab), myList) { fixed, tmdb, pool, ids ->
            val random = java.util.Random(java.time.LocalDate.now().toEpochDay())
            val picks = (fixed + tmdb + pool.shuffled(random)).distinctBy { it.id }.take(FEATURED_TAB)
            picks.map { item ->
                val enriched = if (item.backdropUrl == null && recommendations.isAvailable) enrichForHero(item) else item
                enriched.copy(badgeKey = item.badgeKey ?: badge, inMyList = item.id in ids)
            }
        }
    }

    /** Arte de fundo e sinopse do TMDB para um item da lista (cache de 7 dias); sem correspondência, fica como está. */
    private suspend fun enrichForHero(item: MediaItem): MediaItem {
        val info = runCatching { recommendations.enrich(item) }.getOrNull() ?: return item
        return item.copy(
            backdropUrl = info.backdropUrl ?: item.backdropUrl,
            posterUrl = item.posterUrl ?: info.posterUrl,
            synopsis = item.synopsis ?: info.overview,
            rating = item.rating ?: info.rating,
            genres = item.genres.ifEmpty { info.genres.take(3) },
            year = item.year ?: info.year,
            tmdbId = info.tmdbId,
        )
    }

    /**
     * Fixos da aba Anime: um por grupo (Dragon Ball, Naruto, One Piece), na primeira busca do grupo que a lista
     * atender; série antes de filme, título mais parecido com a busca primeiro, depois o mais recente.
     */
    private suspend fun pinnedAnime(): List<MediaItem> = PINNED_ANIME.mapNotNull { group ->
        group.firstNotNullOfOrNull { query ->
            val results = runCatching { catalog.search(query, PINNED_SEARCH_LIMIT) }.getOrNull() ?: return@firstNotNullOfOrNull null
            val key = TextNormalizer.normalize(query)
            (results.series.map { it.toMediaItem() } + results.movies.map { it.toMediaItem() })
                .filter { TextNormalizer.normalize(it.title).contains(key) }
                .sortedWith(
                    compareByDescending<MediaItem> { it.type == MediaType.SERIES }
                        .thenBy { TextNormalizer.normalize(it.title).length }
                        .thenByDescending { it.year ?: 0 },
                )
                .firstOrNull()
        }
    }.distinctBy { it.id }

    /** Filmes e séries das categorias da aba, para o sorteio do destaque. */
    private fun tabPool(tab: HomeTab): Flow<List<MediaItem>> = combine(
        catalog.categories(ContentSection.MOVIES),
        catalog.categories(ContentSection.SERIES),
    ) { movies, series -> movies.filter { TabKeywords.tabOf(it.name) == tab } to series.filter { TabKeywords.tabOf(it.name) == tab } }
        .flatMapLatest { (movieCats, seriesCats) ->
            val flows = seriesCats.map { cat -> catalog.series(cat.id, FEATURED_POOL).map { list -> list.map { it.toMediaItem() } } } +
                movieCats.map { cat -> catalog.movies(cat.id, FEATURED_POOL).map { list -> list.map { it.toMediaItem() } } }
            if (flows.isEmpty()) flowOf(emptyList()) else combine(flows) { lists -> lists.flatMap { it }.filter { it.posterUrl != null } }
        }

    /** Recomendações do TMDB que existem na lista; sem chave ou sem correspondência, itens recentes. */
    private fun generalFeatured(): Flow<List<MediaItem>> {
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

    /** Jogos do dia (ao vivo primeiro, depois os próximos). Lista sem canais de evento: canais de futebol com o que está no ar. */
    override fun footballNow(): Flow<List<FootballEvent>> =
        combine(catalog.matchSchedule(), minuteTicker()) { schedule, now -> schedule to now }.flatMapLatest { (schedule, now) ->
            val upcoming = schedule.filter { it.status(now) != MatchStatus.FINISHED }
            if (schedule.isNotEmpty()) {
                flowOf(upcoming.take(FOOTBALL_NOW).map { e ->
                    FootballEvent(
                        id = e.key, channelId = e.channelId, channelName = e.channelName, title = "${e.home} x ${e.away}",
                        isLive = e.status(now) == MatchStatus.LIVE, start = e.start,
                        league = LeagueNames.fromHint(e.leagueHint), home = e.home, away = e.away,
                    )
                })
            } else {
                catalog.footballChannels(FOOTBALL_NOW).map { channels ->
                    val airing = runCatching { catalog.nowPlaying(ChannelFilter.Football, now) }.getOrDefault(emptyMap())
                    channels.filterNot { MatchSchedule.isPlaceholder(it) }.map { ch ->
                        FootballEvent(id = ch.id, channelName = ch.name, title = ch.epgKey?.let { airing[it]?.title } ?: ch.categoryName ?: ch.name)
                    }
                }
            }
        }

    /** Tique por minuto para o status (ao vivo/encerrado) acompanhar o relógio. */
    private fun minuteTicker(): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000L - System.currentTimeMillis() % 60_000L)
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
        // A aba Futebol abre a tela de Jogos; estas linhas são o trilho "Vídeos" dela.
        HomeTab.FOOTBALL -> combine(
            catalog.footballMovies(ROW_ITEMS).map { list -> CategoryRow("football-vod", "Jogos e programas", list.map { it.toMediaItem() }) },
            catalog.footballSeries(ROW_ITEMS).map { list -> CategoryRow("football-series", "Séries de futebol", list.map { it.toMediaItem() }) },
        ) { a, b -> listOf(a, b).filter { it.items.isNotEmpty() } }
        HomeTab.KIDS -> keywordRows(HomeTab.KIDS)
        HomeTab.ANIME -> keywordRows(HomeTab.ANIME)
        HomeTab.NOVELAS -> keywordRows(HomeTab.NOVELAS)
        HomeTab.MY_LIST -> myListRows()
        HomeTab.LIVE -> flowOf(emptyList())
    }.combine(myList) { rows, ids -> rows.map { row -> row.copy(items = row.items.map { it.copy(inMyList = it.id in ids) }) } }

    override fun availableTabs(): Flow<List<HomeTab>> = combine(
        catalog.categories(ContentSection.LIVE),
        catalog.categories(ContentSection.MOVIES),
        catalog.categories(ContentSection.SERIES),
    ) { live, movies, series ->
        val present = (live + movies + series).mapNotNullTo(HashSet()) { TabKeywords.tabOf(it.name) }
        HomeTab.entries.filter { it !in HomeTab.OPTIONAL || it in present }
    }

    /**
     * Kids/Anime: uma linha por categoria cujo nome bate com as palavras da aba (filmes, séries, canais),
     * mais, em Kids, as séries que a lista marca com gênero "Kids" fora dessas categorias.
     */
    private fun keywordRows(tab: HomeTab): Flow<List<CategoryRow>> = combine(
        catalog.categories(ContentSection.MOVIES),
        catalog.categories(ContentSection.SERIES),
        catalog.categories(ContentSection.LIVE),
    ) { movies, series, live -> Triple(movies, series, live) }.flatMapLatest { (movies, series, live) ->
        val movieCats = movies.filter { TabKeywords.tabOf(it.name) == tab }
        val seriesCats = series.filter { TabKeywords.tabOf(it.name) == tab }
        val liveCats = live.filter { TabKeywords.tabOf(it.name) == tab }
        val flows = buildList {
            movieCats.forEach { cat -> add(catalog.movies(cat.id, ROW_ITEMS).map { list -> CategoryRow(cat.id, cat.name, list.map { it.toMediaItem() }, ContentSection.MOVIES, cat.id) }) }
            seriesCats.forEach { cat -> add(catalog.series(cat.id, ROW_ITEMS).map { list -> CategoryRow(cat.id, cat.name, list.map { it.toMediaItem() }, ContentSection.SERIES, cat.id) }) }
            if (tab == HomeTab.KIDS) {
                add(catalog.seriesByGenre(KIDS_GENRE, seriesCats.map { it.id }, ROW_ITEMS).map { list -> CategoryRow("kids-genre", "Mais para crianças", list.map { it.toMediaItem() }) })
            }
            liveCats.forEach { cat -> add(catalog.channels(ChannelFilter.Category(cat.id)).map { list -> CategoryRow(cat.id, cat.name, list.take(ROW_ITEMS).map { it.toMediaItem() }, ContentSection.LIVE, cat.id) }) }
        }
        if (flows.isEmpty()) flowOf(emptyList()) else combine(flows) { rows -> rows.filter { it.items.isNotEmpty() } }
    }

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
        private const val FEATURED_TAB = 8
        private const val FEATURED_POOL = 40
        /** Fixos da aba Anime, por grupo: a primeira busca do grupo que a lista atender. */
        private val PINNED_ANIME = listOf(
            listOf("dragon ball super", "dragon ball daima", "dragon ball"),
            listOf("boruto", "naruto shippuden", "naruto"),
            listOf("one piece"),
        )
        private const val PINNED_SEARCH_LIMIT = 80
        private const val FEATURED_SERIES = 2
        private const val FOOTBALL_NOW = 10
        private const val ROW_ITEMS = 24
        private const val CONTINUE_ITEMS = 12
        /** Gênero que a lista (Xtream) informa para séries infantis. */
        private const val KIDS_GENRE = "Kids"
        /** Id virtual usado pela grade para listar só canais de futebol. */
        const val FOOTBALL_CATEGORY = "football"
    }
}
