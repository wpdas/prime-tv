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
)
