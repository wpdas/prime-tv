package com.wpdas.primetv.domain.football

import com.wpdas.primetv.domain.model.Channel
import com.wpdas.primetv.domain.model.EpgProgram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class MatchScheduleTest {
    private val zone: ZoneId = ZoneId.of("America/Sao_Paulo")
    private val today: LocalDate = LocalDate.of(2026, 11, 2)
    private val now: Long = at(today, 15, 0)

    private fun at(day: LocalDate, hour: Int, minute: Int): Long =
        day.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

    private fun channel(id: String, name: String, category: String? = "JOGOS DO DIA", epgKey: String? = null) = Channel(
        id = id, name = name, number = id.hashCode() and 0xffff, logoUrl = null, categoryId = category, categoryName = category,
        streamUrl = "http://example.test/$id", epgChannelId = null, epgKey = epgKey, isFootball = true,
    )

    private fun program(key: String, title: String, start: Long, durationMin: Int = 120) =
        EpgProgram(key, start, start + durationMin * 60_000L, title, null)

    @Test fun `le times, horario e competicao do nome`() {
        val m = MatchSchedule.parseName("Cuiabá x São Bernardo - 18:30 Série B FHD")
        assertEquals(MatchSchedule.NamedMatch("Cuiabá", "São Bernardo", 18, 30, "Série B"), m)
        assertEquals(MatchSchedule.NamedMatch("Palmeiras", "Vasco da Gama", 16, 0, null), MatchSchedule.parseName("Palmeiras x Vasco da Gama - 16:00 HD²"))
        assertEquals(MatchSchedule.NamedMatch("Grêmio Novorizont..", "Operário-PR", 18, 30, "Série B"), MatchSchedule.parseName("Grêmio Novorizont.. x Operário-PR - 18:30 Série B HD"))
        assertEquals("Newell's Old Boys", MatchSchedule.parseName("Newell's Old Boys x Club Atlético Unión - 18:30 FHD")?.home)
    }

    @Test fun `sem horario no nome nao e slot`() {
        assertNull(MatchSchedule.parseName("Palmeiras x Vasco"))
        assertNull(MatchSchedule.parseName("Premiere Clubes 2 FHD"))
        assertNull(MatchSchedule.parseName("Jogos do Dia"))
        assertNull(MatchSchedule.parseName("Canal 4K x265 - 10:00"))
    }

    @Test fun `slots genericos sao ignorados`() {
        assertTrue(MatchSchedule.isPlaceholder(channel("a", "Brasileirão", "BRASILEIRAO")))
        assertTrue(MatchSchedule.isPlaceholder(channel("b", "Estaduais FHD", "ESTADUAIS")))
        assertTrue(MatchSchedule.isPlaceholder(channel("c", "Jogos do Dia", "JOGOS DO DIA")))
        assertFalse(MatchSchedule.isPlaceholder(channel("d", "Premiere 2 HD", "PREMIERE")))
        assertFalse(MatchSchedule.isPlaceholder(channel("e", "Lecce x Genoa - 14:00 FHD")))
    }

    @Test fun `data vem do EPG do slot quando o horario bate, senao hoje`() {
        val tomorrow = today.plusDays(1)
        val channels = listOf(
            channel("s1", "Sporting Braga x Famalicão - 17:30 FHD", epgKey = "JOGODIA 1"),
            channel("s2", "Lecce x Genoa - 14:00 FHD", epgKey = "JOGODIA 2"),
            channel("s3", "Boca Juniors x River Plate - 15:00 FHD"),
        )
        val programs = listOf(
            program("JOGODIA 1", "AC Milan x Inter", at(today, 16, 45)),
            program("JOGODIA 1", "Sporting Braga x Famalicão", at(tomorrow, 17, 30)),
            program("JOGODIA 2", "Lecce x Genoa", at(today, 14, 0)),
        )
        val events = MatchSchedule.build(channels, programs, now, zone)
        val byHome = events.associateBy { it.home }
        assertEquals(at(tomorrow, 17, 30), byHome.getValue("Sporting Braga").start)
        assertEquals(at(today, 14, 0), byHome.getValue("Lecce").start)
        assertEquals(at(today, 15, 0), byHome.getValue("Boca Juniors").start)
        assertEquals(MatchStatus.LIVE, byHome.getValue("Boca Juniors").status(now))
        assertEquals(MatchStatus.LIVE, byHome.getValue("Lecce").status(now))
        assertEquals(MatchStatus.FINISHED, byHome.getValue("Lecce").status(at(today, 16, 30)))
        assertEquals(MatchStatus.UPCOMING, byHome.getValue("Sporting Braga").status(now))
    }

    @Test fun `competicao - o que a lista diz vem antes da categoria`() {
        val channels = listOf(
            channel("b1", "Cuiabá x São Bernardo - 18:30 Série B FHD", "BRASILEIRAO"),
            channel("b2", "Grêmio x Atlético Mineiro - 18:30 FHD", "BRASILEIRAO"),
            channel("j1", "Juventus x SSC Napoli - 16:45 FHD", "JOGOS DO DIA"),
        )
        val events = MatchSchedule.build(channels, emptyList(), now, zone).associateBy { it.home }
        assertEquals("Série B", events.getValue("Cuiabá").leagueHint)
        assertEquals("Brasileirão", events.getValue("Grêmio").leagueHint)
        assertNull(events.getValue("Juventus").leagueHint)
    }

    @Test fun `EPG de canal de futebol por palavra-chave entra como fonte 2 e se funde ao slot`() {
        val channels = listOf(
            channel("s1", "Palmeiras x Vasco da Gama - 16:00 FHD", "JOGOS DO DIA", epgKey = "JOGODIA 1"),
            channel("p2", "Premiere Clubes 2 FHD", "PREMIERE", epgKey = "PREMIERE 2"),
            channel("p3", "Premiere Clubes 3 FHD", "PREMIERE", epgKey = "PREMIERE 3"),
            channel("e1", "ESPN 4 HD", "ESPN", epgKey = "ESPN 4"),
        )
        val programs = listOf(
            program("PREMIERE 2", "Palmeiras x Vasco - Ao Vivo", at(today, 16, 0)),
            program("PREMIERE 3", "Fortaleza x Atlético Goianiense - Copa do Brasil ║ Fortaleza x Atlético Goianiense", at(today, 19, 0)),
            program("PREMIERE 3", "Circuito Brasileiro de Vôlei de Praia - Ao Vivo", at(today, 21, 0)),
            program("ESPN 4", "Lakers x Celtics", at(today, 22, 0)),
            program("PREMIERE 2", "Winnipeg Jets x Carolina Hurricanes NHL", at(today, 23, 0)),
            program("PREMIERE 2", "VT - São Paulo x Vitória", at(today, 21, 10), 30),
            program("PREMIERE 3", "Dia 04/11 - Botafogo x Atlético-MG", at(today, 22, 0)),
        )
        val keyword = { c: Channel -> c.categoryName == "PREMIERE" }
        val events = MatchSchedule.build(channels, programs, now, zone, keyword)
        assertEquals(listOf("Palmeiras", "Fortaleza"), events.map { it.home })
        val palmeiras = events[0]
        assertEquals("s1", palmeiras.channelId)
        assertEquals(listOf("p2"), palmeiras.altChannelIds)
        assertTrue(palmeiras.fromChannelName)
        val fortaleza = events[1]
        assertEquals("Copa do Brasil", fortaleza.leagueHint)
        assertEquals("p3", fortaleza.channelId)
        assertFalse(fortaleza.fromChannelName)
    }

    @Test fun `ordem - ao vivo, depois proximos, encerrados por ultimo e ontem some`() {
        val channels = listOf(
            channel("a", "Time A x Time B - 10:00 FHD"),
            channel("b", "Time C x Time D - 14:30 FHD"),
            channel("c", "Time E x Time F - 18:00 FHD", epgKey = "K"),
            channel("d", "Time G x Time H - 16:00 FHD"),
        )
        val programs = listOf(program("K", "Time E x Time F", at(today.minusDays(1), 18, 0)))
        val events = MatchSchedule.build(channels, programs, now, zone)
        assertEquals(listOf("Time C", "Time G", "Time A"), events.map { it.home })
    }
}
