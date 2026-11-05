package com.wpdas.primetv.domain.crests

import com.wpdas.primetv.domain.football.MatchEvent

/**
 * Busca e guarda em cache os escudos dos times e a competição de cada jogo. Roda só enquanto a
 * tela de Futebol está aberta; o resultado fica no banco e no cache de imagens para as próximas vezes.
 */
interface MatchArtWarmer {
    suspend fun warm(events: List<MatchEvent>)
}
