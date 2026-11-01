package com.wpdas.primetv.data.remote.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XmlTvParserTest {
    private fun items() = XmlTvParser.parse(javaClass.classLoader!!.getResourceAsStream("epg/sample.xml")!!).toList()

    @Test fun `le canais, usa o nome quando o id e vazio e so o primeiro display-name`() {
        val channels = items().filterIsInstance<XmlTvItem.Channel>().map { it.channel }
        assertEquals(3, channels.size)
        assertEquals(XmlTvChannel("Copa do Mundo", "Copa do Mundo", "http://img/copa.png"), channels[0])
        assertEquals("GLOBO SAO PAULO", channels[1].id)
        assertEquals("Globo SP", channels[1].displayName)
        assertNull(channels[2].icon)
    }

    @Test fun `le programas com fuso, sem fuso e descarta invalidos`() {
        val programmes = items().filterIsInstance<XmlTvItem.Programme>().map { it.programme }
        assertEquals(3, programmes.size)
        val jornal = programmes[0]
        assertEquals("GLOBO SAO PAULO", jornal.channel)
        assertEquals("Jornal & Notícias", jornal.title)
        assertEquals("Edição da noite", jornal.description)
        // 16:30 -03:00 == 19:30 UTC
        assertEquals(XmlTvParser.parseTime("20261101193000 +0000"), jornal.start)
        assertEquals(3 * 60 * 60 * 1000L, jornal.end - jornal.start)
        val fla = programmes[2]
        assertEquals("Flamengo x Palmeiras", fla.title)
        assertEquals(XmlTvParser.parseTime("20261101180000 +0000"), fla.start) // sem fuso assume UTC
    }

    @Test fun `parseTime aceita formatos comuns`() {
        val expected = java.time.ZonedDateTime.of(2026, 11, 1, 19, 30, 0, 0, java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(expected, XmlTvParser.parseTime("20261101163000 -0300"))
        assertEquals(expected, XmlTvParser.parseTime("20261101193000"))
        assertEquals(expected, XmlTvParser.parseTime("20261101193000 +00:00"))
        assertNull(XmlTvParser.parseTime("abc"))
        assertNull(XmlTvParser.parseTime(null))
    }
}
