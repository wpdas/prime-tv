package com.wpdas.primetv.data.fake

import com.wpdas.primetv.domain.model.CategoryRow
import com.wpdas.primetv.domain.model.ContinueWatchingItem
import com.wpdas.primetv.domain.model.FootballEvent
import com.wpdas.primetv.domain.model.HomeTab
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.model.MediaType
import com.wpdas.primetv.domain.repository.HomeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Dados inventados para navegar na tela de Início antes de existir catálogo real (fase 1). */
@Singleton
class FakeHomeRepository @Inject constructor() : HomeRepository {

    private val myList = MutableStateFlow(setOf("farol"))

    private val farol = MediaItem(
        id = "farol", title = "O Último Farol", type = MediaType.SERIES, placeholderColor = 0xFF2B2440,
        year = 2024, genres = listOf("Suspense", "Drama"), rating = 7.8, seasons = 2, episodes = 16,
        synopsis = "Uma faroleira isolada no litoral sul recebe sinais de rádio de um navio desaparecido há trinta anos.",
        badgeKey = "home.badge_trending",
    )
    private val rota = MediaItem(
        id = "rota116", title = "Rota 116", type = MediaType.MOVIE, placeholderColor = 0xFF1F2E3A,
        year = 2023, genres = listOf("Ação", "Policial"), rating = 7.1,
        synopsis = "Um caminhoneiro aposentado aceita uma última viagem noturna e descobre que a carga não é o que dizia a nota.",
        badgeKey = "home.badge_popular",
    )
    private val casa = MediaItem(
        id = "casa", title = "Casa de Vidro", type = MediaType.SERIES, placeholderColor = 0xFF3A2A24,
        year = 2022, genres = listOf("Drama"), rating = 8.2, seasons = 3, episodes = 30,
        synopsis = "Três gerações de uma família de arquitetos dividem a mesma casa e nenhum segredo fica escondido por muito tempo.",
        badgeKey = "home.badge_trending",
    )
    private val sertao = MediaItem(
        id = "sertao", title = "Sertão Profundo", type = MediaType.SERIES, placeholderColor = 0xFF24332A,
        year = 2025, genres = listOf("Aventura", "Drama"), rating = 7.5, seasons = 1, episodes = 8,
        synopsis = "Uma geóloga volta à cidade natal para investigar o sumiço de um rio e encontra uma comunidade que prefere não lembrar.",
        badgeKey = "home.badge_now_playing",
    )
    private val lisboa = MediaItem(
        id = "lisboa", title = "Noite em Lisboa", type = MediaType.MOVIE, placeholderColor = 0xFF33243A,
        year = 2024, genres = listOf("Romance", "Comédia"), rating = 6.9,
        synopsis = "Dois estranhos perdem o último voo e passam a madrugada percorrendo a cidade atrás de um casaco emprestado.",
        badgeKey = "home.badge_popular",
    )

    private val palette = longArrayOf(0xFF2B2440, 0xFF1F2E3A, 0xFF3A2A24, 0xFF24332A, 0xFF33243A, 0xFF1C2A3A, 0xFF2E2350, 0xFF14261C)

    private fun fakeItems(prefix: String, type: MediaType, titles: List<String>): List<MediaItem> =
        titles.mapIndexed { i, title ->
            MediaItem(
                id = "$prefix-$i", title = title, type = type, placeholderColor = palette[i % palette.size],
                year = 2018 + (i % 8), genres = listOf("Drama"), rating = 6.0 + (i % 30) / 10.0,
                seasons = if (type == MediaType.SERIES) 1 + i % 4 else null,
                episodes = if (type == MediaType.SERIES) 8 * (1 + i % 4) else null,
                synopsis = "Sinopse de exemplo para $title. Os dados reais vêm da sua lista e do TMDB.",
            )
        }

    private val movieRows = listOf(
        CategoryRow("lancamentos", "Lançamentos", fakeItems("lanc", MediaType.MOVIE,
            listOf("Maré Alta", "O Som do Metrô", "Último Turno", "Vidas Paralelas", "Fronteira Seca", "Café às Cinco", "Abismo", "Cidade Baixa"))),
        CategoryRow("acao", "Ação", fakeItems("acao", MediaType.MOVIE,
            listOf("Contra o Relógio", "Zona Morta", "Resgate 9", "Operação Vento", "Ponto Cego", "Linha Dura", "Carga Pesada"))),
        CategoryRow("comedia", "Comédia", fakeItems("com", MediaType.MOVIE,
            listOf("Sogro por um Dia", "Meu Vizinho Alien", "Férias Trocadas", "Churrasco de Domingo", "O Estagiário Eterno", "Plano B"))),
    )
    private val seriesRows = listOf(
        CategoryRow("em-alta", "Em alta", listOf(farol, casa, sertao) + fakeItems("alta", MediaType.SERIES,
            listOf("Bairro Alto", "Guarita", "A Herdeira", "Sinal Fechado", "Doca 7"))),
        CategoryRow("documentarios", "Documentários", fakeItems("doc", MediaType.SERIES,
            listOf("Amazônia Viva", "Copa de 70", "Mestres do Samba", "Rota do Cacau", "Vale do Silício BR", "Noites de Rádio"))),
    )

    override fun featured(): Flow<List<MediaItem>> =
        myList.map { ids -> listOf(farol, rota, casa, sertao).map { it.copy(inMyList = it.id in ids) } }

    override fun continueWatching(): Flow<List<ContinueWatchingItem>> = flow {
        emit(
            listOf(
                ContinueWatchingItem(farol, "T1:E4", 22, 0.62f),
                ContinueWatchingItem(rota, null, 48, 0.35f),
                ContinueWatchingItem(casa, "T3:E9", 12, 0.80f),
                ContinueWatchingItem(sertao, "T1:E2", 31, 0.20f),
                ContinueWatchingItem(lisboa, null, 65, 0.15f),
            )
        )
    }

    override fun footballNow(): Flow<List<FootballEvent>> = flow {
        emit(
            listOf(
                FootballEvent("f1", "Premiere 1", "Flamengo × Palmeiras"),
                FootballEvent("f2", "ESPN 2", "Real Madrid × Barcelona"),
                FootballEvent("f3", "SporTV", "Grêmio × Inter"),
                FootballEvent("f4", "Premiere 3", "Santos × Bahia"),
                FootballEvent("f5", "TNT", "Liverpool × Arsenal"),
            )
        )
    }

    override fun categoryRows(tab: HomeTab): Flow<List<CategoryRow>> = myList.map { ids ->
        val rows = when (tab) {
            HomeTab.HOME -> seriesRows.take(1) + movieRows.take(2)
            HomeTab.MOVIES -> movieRows
            HomeTab.SERIES -> seriesRows
            HomeTab.MY_LIST -> listOf(CategoryRow("minha-lista", "Minha lista",
                (listOf(farol, rota, casa, sertao, lisboa) + movieRows.flatMap { it.items }).filter { it.id in ids }))
            HomeTab.FOOTBALL -> listOf(CategoryRow("fut-vod", "Jogos completos", fakeItems("futvod", MediaType.MOVIE,
                listOf("Final 2024 · Fla × Pal", "Clássico dos Milhões", "Gre-Nal 440", "Libertadores 2023"))))
            HomeTab.LIVE -> emptyList()
        }
        rows.map { row -> row.copy(items = row.items.map { it.copy(inMyList = it.id in ids) }) }
    }

    override fun backgroundUpdate(): Flow<String?> = flow {
        emit(null)
        delay(3_000)
        emit("section.movies")
        delay(5_000)
        emit(null)
    }

    override suspend fun toggleMyList(itemId: String) {
        myList.update { if (itemId in it) it - itemId else it + itemId }
    }
}
