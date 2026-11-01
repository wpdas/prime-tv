package com.wpdas.primetv.data.remote.m3u

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

class M3uParserTest {

    private fun resource(name: String) = javaClass.classLoader!!.getResourceAsStream(name)!!.bufferedReader()

    @Test fun `le cabecalho e todas as entradas`() {
        val items = M3uParser.parse(resource("m3u/sample.m3u")).toList()
        val header = (items.first() as M3uItem.Header).header
        assertEquals("http://epg.exemplo.com/guia.xml.gz", header.epgUrl)
        val entries = items.filterIsInstance<M3uItem.Entry>().map { it.entry }
        assertEquals(13, entries.size)

        val globo = entries[0]
        assertEquals("Globo SP FHD", globo.name)
        assertEquals("GLOBO SP", globo.tvgId)
        assertEquals("http://img.exemplo/globo.png", globo.logo)
        assertEquals("Canais Abertos", globo.group)
        assertEquals("http://servidor.exemplo:8080/usuario/senha/1001", globo.url)
    }

    @Test fun `atributos vazios viram null e virgula dentro do grupo nao quebra o nome`() {
        val entries = M3uParser.parse(resource("m3u/sample.m3u")).filterIsInstance<M3uItem.Entry>().map { it.entry }.toList()
        val band = entries[1]
        assertNull(band.tvgId)
        assertNull(band.logo)
        val premiere = entries[2]
        assertEquals("Premiere 1 FHD", premiere.name)
        assertEquals("Esportes, Futebol", premiere.group)
    }

    @Test fun `atributos sem aspas e entrada sem atributos`() {
        val entries = M3uParser.parse(resource("m3u/sample.m3u")).filterIsInstance<M3uItem.Entry>().map { it.entry }.toList()
        val news = entries[3]
        assertEquals("http://img.exemplo/semaspas.png", news.logo)
        assertEquals("Noticias", news.group)
        val bare = entries[4]
        assertEquals("Canal sem atributos", bare.name)
        assertTrue(bare.attributes.isEmpty())
        assertNull(bare.group)
    }

    @Test fun `EXTGRP define o grupo quando nao ha group-title`() {
        val entries = M3uParser.parse(resource("m3u/sample.m3u")).filterIsInstance<M3uItem.Entry>().map { it.entry }.toList()
        val casa = entries.first { it.name.startsWith("Casa de Vidro") }
        assertEquals("Novelas", casa.group)
    }

    @Test fun `aceita CRLF e BOM`() {
        val crlf = M3uParser.parse(resource("m3u/crlf.m3u")).filterIsInstance<M3uItem.Entry>().single().entry
        assertEquals("Canal CRLF", crlf.name)
        assertEquals("http://servidor.exemplo/crlf/1", crlf.url)
        val bom = M3uParser.parse(resource("m3u/bom.m3u")).toList()
        assertTrue(bom.first() is M3uItem.Header)
        assertEquals("Com BOM", (bom[1] as M3uItem.Entry).entry.name)
    }

    @Test fun `lista sem cabecalho ainda emite header sintetico`() {
        val items = M3uParser.parse(StringReader("#EXTINF:-1,X\nhttp://a/b\n").buffered()).toList()
        assertTrue(items[0] is M3uItem.Header)
        assertNull((items[0] as M3uItem.Header).header.epgUrl)
        assertEquals("X", (items[1] as M3uItem.Entry).entry.name)
    }

    @Test fun `lista grande em streaming`() {
        val n = 200_000
        val sb = StringBuilder("#EXTM3U\n")
        repeat(n) { i ->
            sb.append("#EXTINF:-1 tvg-id=\"id$i\" tvg-logo=\"http://img/$i.png\" group-title=\"Grupo ${i % 50}\",Canal $i HD\n")
            sb.append("http://servidor.exemplo/u/p/$i\n")
        }
        val start = System.nanoTime()
        var count = 0
        var lastName = ""
        for (item in M3uParser.parse(StringReader(sb.toString()).buffered())) {
            if (item is M3uItem.Entry) { count++; lastName = item.entry.name }
        }
        val ms = (System.nanoTime() - start) / 1_000_000
        assertEquals(n, count)
        assertEquals("Canal ${n - 1} HD", lastName)
        assertTrue("parse de 200k entradas levou ${ms}ms", ms < 10_000)
    }

    @Test fun `splitAttributesAndName respeita aspas`() {
        val (attrs, name) = M3uParser.splitAttributesAndName("""-1 group-title="A, B" tvg-id="x",Nome, com vírgula""")
        assertEquals("""-1 group-title="A, B" tvg-id="x"""", attrs)
        assertEquals("Nome, com vírgula", name)
    }
}
