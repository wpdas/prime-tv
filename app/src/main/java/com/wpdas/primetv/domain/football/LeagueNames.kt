package com.wpdas.primetv.domain.football

import com.wpdas.primetv.core.util.TextNormalizer

/**
 * Nome da competição para exibir. O que a lista informa vem primeiro; o nome em inglês da base
 * pública de times (TheSportsDB, `strLeague`) é traduzido por tabela, com o nome cru como reserva.
 */
object LeagueNames {
    private val FROM_LIST = mapOf(
        "serie a" to "Brasileirão Série A",
        "serie b" to "Brasileirão Série B",
        "serie c" to "Brasileirão Série C",
        "brasileirao" to "Brasileirão",
        "estaduais" to "Estaduais",
    )

    private val FROM_SPORTSDB = mapOf(
        "english premier league" to "Premier League",
        "english league championship" to "Championship",
        "spanish la liga" to "La Liga",
        "italian serie a" to "Itália Série A",
        "italian serie b" to "Itália Série B",
        "german bundesliga" to "Bundesliga",
        "french ligue 1" to "Ligue 1",
        "portuguese primeira liga" to "Primeira Liga",
        "dutch eredivisie" to "Eredivisie",
        "brazilian serie a" to "Brasileirão Série A",
        "brazilian serie b" to "Brasileirão Série B",
        "brazilian serie c" to "Brasileirão Série C",
        "argentinian primera division" to "Argentina Primera",
        "argentine primera division" to "Argentina Primera",
        "uruguayan primera division" to "Uruguai Primera",
        "mexican primera league" to "Liga MX",
        "mexican liga mx" to "Liga MX",
        "american major league soccer" to "MLS",
        "nwsl" to "NWSL",
        "scottish premiership" to "Escócia Premiership",
        "turkish super lig" to "Süper Lig",
        "saudi pro league" to "Saudi Pro League",
        "uefa champions league" to "Champions League",
        "uefa europa league" to "Europa League",
        "copa libertadores" to "Libertadores",
        "copa sudamericana" to "Sul-Americana",
    )

    /** Rótulo a partir do que a lista diz ("Série B" -> "Brasileirão Série B"). */
    fun fromHint(hint: String?): String? {
        val h = hint?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return FROM_LIST[TextNormalizer.searchKey(h)] ?: h
    }

    /** Rótulo a partir do nome em inglês da base de times; null se não houver. */
    fun fromSportsDb(league: String?): String? {
        val l = league?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return FROM_SPORTSDB[TextNormalizer.searchKey(l)] ?: l
    }

    /** Iniciais para o escudo estilizado da liga ("Premier League" -> "PL"). */
    fun initials(label: String): String {
        val words = TextNormalizer.stripAccents(label).split(' ', '-').filter { it.isNotBlank() && it.lowercase() !in STOP }
        return when {
            words.isEmpty() -> label.take(2).uppercase()
            words.size == 1 -> words[0].take(3).uppercase()
            else -> words.take(3).joinToString("") { it.first().uppercase() }
        }
    }

    private val STOP = setOf("de", "do", "da")
}
