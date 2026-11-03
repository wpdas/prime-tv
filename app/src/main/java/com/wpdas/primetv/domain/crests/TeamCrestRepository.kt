package com.wpdas.primetv.domain.crests

/**
 * O que a base pública de times sabe de um clube: escudo (URL de terceiros), liga em inglês e a id dela,
 * e o nome como a base o escreve.
 */
data class TeamInfo(val crestUrl: String?, val league: String?, val leagueId: String? = null, val name: String? = null)

/** Competição real de uma partida: nome em inglês da base, id e escudo (URL de terceiros). */
data class LeagueInfo(val league: String, val leagueId: String?, val badgeUrl: String?)

/** Escudos de times e competições, com cache; campos null quando a base não conhece. */
interface TeamCrestRepository {
    suspend fun teamInfo(team: String): TeamInfo
    suspend fun crestUrl(team: String): String? = teamInfo(team).crestUrl
    /** Escudo da competição pela id devolvida em [TeamInfo.leagueId]. */
    suspend fun leagueBadge(leagueId: String): String?
    /**
     * Competição de uma partida (mandante x visitante no dia que contém [startMs]), pela base pública.
     * null quando a base não tem o jogo: nesse caso a tela não mostra competição nenhuma, em vez de chutar.
     */
    suspend fun matchLeague(home: String, away: String, startMs: Long): LeagueInfo?
}
