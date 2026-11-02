package com.wpdas.primetv.domain.crests

/** O que a base pública de times sabe de um clube: escudo (URL de terceiros), liga em inglês e a id dela. */
data class TeamInfo(val crestUrl: String?, val league: String?, val leagueId: String? = null)

/** Escudos de times e competições, com cache; campos null quando a base não conhece. */
interface TeamCrestRepository {
    suspend fun teamInfo(team: String): TeamInfo
    suspend fun crestUrl(team: String): String? = teamInfo(team).crestUrl
    /** Escudo da competição pela id devolvida em [TeamInfo.leagueId]. */
    suspend fun leagueBadge(leagueId: String): String?
}
