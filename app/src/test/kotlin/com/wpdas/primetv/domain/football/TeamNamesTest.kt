package com.wpdas.primetv.domain.football

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamNamesTest {
    @Test fun `chave ignora sufixos de clube`() {
        assertEquals("napoli", TeamNames.key("SSC Napoli"))
        assertEquals("vasco da gama", TeamNames.key("Vasco da Gama FC"))
        assertEquals("liverpool", TeamNames.key("Liverpool FC"))
    }

    @Test fun `mesmo time escrito de dois jeitos`() {
        assertTrue(TeamNames.same("Vasco", "Vasco da Gama"))
        assertTrue(TeamNames.same("Liverpool FC", "Liverpool"))
        assertFalse(TeamNames.same("Inter", "Internacional"))
        // Prefixo conta como o mesmo time: a fusão de jogos ainda exige o mesmo adversário e horário próximo.
        assertTrue(TeamNames.same("Botafogo", "Botafogo SP"))
        assertFalse(TeamNames.same("Atlético Mineiro", "Atlético Goianiense"))
    }

    @Test fun `titulos de EPG viram partida`() {
        assertEquals(Match("Palmeiras", "Vasco"), MatchTitle.parse("Palmeiras x Vasco - Ao Vivo"))
        assertEquals(Match("ATHLETIC BILBAO", "REAL SOCIEDAD"), MatchTitle.parse("AO VIVO: ATHLETIC BILBAO X REAL SOCIEDAD | LALIGA 26/27 | 11ª RODADA"))
        assertEquals(Match("Fortaleza", "Atlético Goianiense"), MatchTitle.parse("Fortaleza x Atlético Goianiense - Copa do Brasil ║ Fortaleza x Atlético Goianiense"))
    }
}
