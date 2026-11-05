package com.wpdas.primetv.data.remote.crests

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.wpdas.primetv.core.di.IoDispatcher
import com.wpdas.primetv.domain.crests.MatchArtWarmer
import com.wpdas.primetv.domain.crests.TeamCrestRepository
import com.wpdas.primetv.domain.football.MatchEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enquanto a tela de Futebol está aberta, busca aos poucos os escudos dos times, a competição real
 * de cada partida e o escudo dela, e guarda: URLs no banco (30 dias) e imagens no cache em disco do
 * Coil. Antes rodava desde a abertura do app, observando o guia o tempo todo (inclusive durante a
 * importação); agora só gasta rede e CPU quando alguém olha os jogos. Respeita o espaçamento do
 * [TeamCrestRepository].
 */
@Singleton
class CrestPrefetcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crests: TeamCrestRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : MatchArtWarmer {

    override suspend fun warm(events: List<MatchEvent>) = withContext(io) {
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
}
