package com.wpdas.primetv.domain.football

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LeagueNamesTest {
    @Test fun `o que a lista diz vira rotulo`() {
        assertEquals("Brasileirão Série B", LeagueNames.fromHint("Série B"))
        assertEquals("Brasileirão Série B", LeagueNames.fromHint("serie b"))
        assertEquals("Brasileirão", LeagueNames.fromHint("Brasileirão"))
        assertEquals("Copa do Brasil", LeagueNames.fromHint("Copa do Brasil"))
        assertNull(LeagueNames.fromHint(null))
        assertNull(LeagueNames.fromHint("  "))
    }

    @Test fun `nome em ingles da base de times e traduzido, com reserva no nome cru`() {
        assertEquals("Premier League", LeagueNames.fromSportsDb("English Premier League"))
        assertEquals("Itália Série A", LeagueNames.fromSportsDb("Italian Serie A"))
        assertEquals("Brasileirão Série A", LeagueNames.fromSportsDb("Brazilian Serie A"))
        assertEquals("Liga MX", LeagueNames.fromSportsDb("Mexican Primera League"))
        assertEquals("Chilean Primera Division", LeagueNames.fromSportsDb("Chilean Primera Division"))
        assertNull(LeagueNames.fromSportsDb(""))
    }

    @Test fun `iniciais para o escudo estilizado`() {
        assertEquals("PL", LeagueNames.initials("Premier League"))
        assertEquals("ISA", LeagueNames.initials("Itália Série A"))
        assertEquals("BSB", LeagueNames.initials("Brasileirão Série B"))
        assertEquals("MLS", LeagueNames.initials("MLS"))
    }
}
