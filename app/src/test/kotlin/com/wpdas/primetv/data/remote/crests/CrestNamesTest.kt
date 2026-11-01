package com.wpdas.primetv.data.remote.crests

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CrestNamesTest {
    private fun team(name: String, sport: String = "Soccer", country: String = "Brazil", alt: String? = null) =
        SportsDbTeam(name = name, alternate = alt, sport = sport, country = country, badge = "http://img/$name.png")

    @Test fun `apelidos viram o nome que a base conhece`() {
        assertEquals("Atletico Mineiro", CrestNames.searchName("Atlético-MG"))
        assertEquals("Internacional", CrestNames.searchName("Inter"))
        assertEquals("Flamengo", CrestNames.searchName("Flamengo"))
        assertEquals("Londrina", CrestNames.searchName("Londrina"))
        assertEquals("Sao Paulo", CrestNames.searchName("São Paulo FC"))
    }

    @Test fun `escolhe futebol, prefere Brasil e rejeita nome que nao lembra o buscado`() {
        assertNull(CrestNames.pick("Internacional", listOf(team("Intercity", country = "Spain"))))
        assertEquals("Botafogo", CrestNames.pick("Botafogo", listOf(team("Botafogo SP", country = "Brazil"), team("Botafogo", country = "Brazil")))?.name)
        assertNull(CrestNames.pick("America Mineiro", listOf(team("BAR", sport = "Motorsport", country = "United Kingdom"))))
        assertEquals("Flamengo", CrestNames.pick("Flamengo", listOf(team("Flamengo", alt = "Clube de Regatas do Flamengo")))?.name)
    }
}
