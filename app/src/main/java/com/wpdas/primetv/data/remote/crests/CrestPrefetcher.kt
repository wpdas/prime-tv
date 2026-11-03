package com.wpdas.primetv.data.remote.crests

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.wpdas.primetv.core.di.ApplicationScope
import com.wpdas.primetv.core.di.IoDispatcher
import com.wpdas.primetv.domain.crests.TeamCrestRepository
import com.wpdas.primetv.domain.football.MatchEvent
import com.wpdas.primetv.domain.repository.CatalogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ao abrir o app (e sempre que o guia de jogos mudar), busca aos poucos os escudos dos times, a
 * competição real de cada partida e o escudo dela, e guarda: URLs no banco (30 dias) e imagens no cache em disco do Coil. Assim a
 * tela de Jogos abre pronta em vez de preencher aos poucos. Respeita o espaçamento do [TeamCrestRepository].
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Singleton
class CrestPrefetcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalog: CatalogRepository,
    private val crests: TeamCrestRepository,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        catalog.matchSchedule()
            .map { events -> events.map { it.home to it.away } to events }
            .distinctUntilChanged { old, new -> old.first == new.first }
            .debounce(PREFETCH_DELAY_MS)
            .mapLatest { (_, events) -> prefetch(events) }
            .flowOn(io)
            .launchIn(scope)
    }

    private suspend fun prefetch(events: List<MatchEvent>) {
        val loader = SingletonImageLoader.get(context)
        val leaguesDone = HashSet<String>()
        for (event in events) {
            for (team in listOf(event.home, event.away)) {
                runCatching { crests.teamInfo(team) }.getOrNull()?.crestUrl?.let { warm(loader, it) }
            }
            // Competição real da partida (a lista só informa às vezes) e o escudo dela.
            if (event.leagueHint != null) continue
            val league = runCatching { crests.matchLeague(event.home, event.away, event.start) }.getOrNull() ?: continue
            val badge = league.badgeUrl ?: league.leagueId?.takeIf { leaguesDone.add(it) }?.let { id -> runCatching { crests.leagueBadge(id) }.getOrNull() }
            badge?.let { warm(loader, it) }
        }
    }

    /** Baixa a imagem para o cache em disco do Coil, sem exibir. */
    private fun warm(loader: coil3.ImageLoader, url: String) {
        loader.enqueue(ImageRequest.Builder(context).data(url).build())
    }

    private companion object {
        /** Espera a lista assentar depois de uma importação antes de começar. */
        const val PREFETCH_DELAY_MS = 3_000L
    }
}
