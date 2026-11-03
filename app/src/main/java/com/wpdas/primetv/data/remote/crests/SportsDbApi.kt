package com.wpdas.primetv.data.remote.crests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * TheSportsDB (base pública de dados esportivos). As imagens dos escudos são servidas por
 * eles e pertencem aos respectivos clubes; o app só exibe. Chave de teste pública "123".
 */
interface SportsDbApi {
    @GET("api/v1/json/$TEST_KEY/searchteams.php")
    suspend fun searchTeams(@Query("t") name: String): SportsDbTeams

    @GET("api/v1/json/$TEST_KEY/lookupleague.php")
    suspend fun lookupLeague(@Query("id") id: String): SportsDbLeagues

    /** Partida pelo nome "Mandante_vs_Visitante" (nomes como a base conhece); traz a competição real do jogo. */
    @GET("api/v1/json/$TEST_KEY/searchevents.php")
    suspend fun searchEvents(@Query("e") event: String): SportsDbEvents

    companion object {
        const val BASE_URL = "https://www.thesportsdb.com/"
        const val TEST_KEY = "123"
    }
}

@Serializable
data class SportsDbTeams(val teams: List<SportsDbTeam>? = null)

@Serializable
data class SportsDbTeam(
    @SerialName("strTeam") val name: String? = null,
    @SerialName("strTeamAlternate") val alternate: String? = null,
    @SerialName("strTeamShort") val short: String? = null,
    @SerialName("strSport") val sport: String? = null,
    @SerialName("strCountry") val country: String? = null,
    @SerialName("strBadge") val badge: String? = null,
    @SerialName("strLeague") val league: String? = null,
    @SerialName("idLeague") val leagueId: String? = null,
    @SerialName("strGender") val gender: String? = null,
)

@Serializable
data class SportsDbEvents(@SerialName("event") val events: List<SportsDbEvent>? = null)

@Serializable
data class SportsDbEvent(
    @SerialName("strSport") val sport: String? = null,
    @SerialName("strHomeTeam") val home: String? = null,
    @SerialName("strAwayTeam") val away: String? = null,
    @SerialName("strLeague") val league: String? = null,
    @SerialName("idLeague") val leagueId: String? = null,
    @SerialName("strLeagueBadge") val leagueBadge: String? = null,
    /** "AAAA-MM-DD". */
    @SerialName("dateEvent") val date: String? = null,
)

@Serializable
data class SportsDbLeagues(val leagues: List<SportsDbLeague>? = null)

@Serializable
data class SportsDbLeague(
    @SerialName("idLeague") val id: String? = null,
    @SerialName("strLeague") val name: String? = null,
    @SerialName("strBadge") val badge: String? = null,
    @SerialName("strLogo") val logo: String? = null,
)
