package com.wpdas.primetv.data.remote.crests

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MatchEventsTest {
    private val day: LocalDate = LocalDate.of(2026, 11, 3)

    private fun event(home: String, away: String, league: String, date: String, sport: String = "Soccer") =
        SportsDbEvent(sport = sport, home = home, away = away, league = league, leagueId = "1", date = date)

    @Test
    fun `consulta usa underline no lugar de espaco e tira acentos`() {
        assertEquals("Manchester_United_vs_Roma", MatchEvents.query("Manchester United", "Roma"))
        assertEquals("Atletico_Tucuman_vs_Argentinos_Juniors", MatchEvents.query("Atlético Tucumán", "Argentinos Juniors"))
    }

    @Test
    fun `chave normaliza os nomes e inclui o dia`() {
        assertEquals("galatasaray|vfb stuttgart|2026-11-03", MatchEvents.key("Galatasaray", "VfB Stuttgart FC", day))
        assertEquals("", MatchEvents.key("", "Roma", day))
    }

    @Test
    fun `escolhe o evento de futebol com os mesmos times no dia`() {
        val events = listOf(
            event("Galatasaray", "Stuttgart", "Turkish Super Lig", "2026-03-01"), // outra data: fora
            event("Galatasaray", "Stuttgart", "UEFA Champions League", "2026-11-03"),
            event("Galatasaray", "Stuttgart", "Basketball Euroleague", "2026-11-03", sport = "Basketball"),
        )
        assertEquals("UEFA Champions League", MatchEvents.pick("Galatasaray", "VfB Stuttgart", day, events)?.league)
    }

    @Test
    fun `aceita o dia vizinho por diferenca de fuso mas prefere a data exata`() {
        val events = listOf(
            event("Chapecoense", "Mirassol", "Copa do Brasil", "2026-11-04"),
            event("Chapecoense", "Mirassol", "Brazilian Serie A", "2026-11-03"),
        )
        assertEquals("Brazilian Serie A", MatchEvents.pick("Chapecoense", "Mirassol", day, events)?.league)
        assertEquals("Copa do Brasil", MatchEvents.pick("Chapecoense", "Mirassol", day, events.take(1))?.league)
    }

    @Test
    fun `times trocados ou diferentes nao servem`() {
        val events = listOf(event("Stuttgart", "Galatasaray", "UEFA Champions League", "2026-11-03"))
        assertNull(MatchEvents.pick("Galatasaray", "Stuttgart", day, events))
        assertNull(MatchEvents.pick("Galatasaray", "Fenerbahce", day, events))
    }
}
