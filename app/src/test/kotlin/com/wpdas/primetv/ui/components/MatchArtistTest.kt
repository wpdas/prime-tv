package com.wpdas.primetv.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatchArtistTest {
    @Test fun `reconhece partidas em varios formatos`() {
        assertEquals(Match("Flamengo", "Palmeiras"), MatchArtist.parse("Flamengo x Palmeiras"))
        assertEquals(Match("Grêmio", "Inter"), MatchArtist.parse("Grêmio × Inter"))
        assertEquals(Match("Real Madrid", "Barcelona"), MatchArtist.parse("Real Madrid vs Barcelona - La Liga"))
        assertEquals(Match("Botafogo", "Atlético-MG"), MatchArtist.parse("Dia 04/11 - Botafogo x Atlético-MG (Premiere)"))
        assertEquals(Match("Londrina", "América"), MatchArtist.parse("Londrina x América"))
    }

    @Test fun `ignora o que nao e partida`() {
        assertNull(MatchArtist.parse("Globo SP HD"))
        assertNull(MatchArtist.parse("Premiere Clubes"))
        assertNull(MatchArtist.parse("Canal 4K x265"))
    }

    @Test fun `siglas conhecidas e geradas`() {
        assertEquals("FLA", MatchArtist.abbreviation("Flamengo"))
        assertEquals("CAM", MatchArtist.abbreviation("Atlético-MG"))
        assertEquals("RMA", MatchArtist.abbreviation("Real Madrid"))
        assertEquals("LON", MatchArtist.abbreviation("Londrina"))
        assertEquals(3, MatchArtist.abbreviation("Operário Ferroviário").length)
    }
}
