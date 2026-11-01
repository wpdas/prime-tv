package com.wpdas.primetv.data.remote.m3u

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class M3uClassifierTest {
    private fun entry(name: String, url: String, group: String? = null) =
        M3uEntry(name = name, url = url, attributes = emptyMap(), group = group)

    @Test fun `caminho da url manda`() {
        assertEquals(M3uKind.MOVIE, M3uClassifier.classify(entry("Globo", "http://h/movie/u/p/1.mp4", "Canais")))
        assertEquals(M3uKind.SERIES_EPISODE, M3uClassifier.classify(entry("Qualquer", "http://h/series/u/p/1.mp4", "Filmes")))
    }

    @Test fun `extensao de video sem padrao de episodio e filme`() {
        assertEquals(M3uKind.MOVIE, M3uClassifier.classify(entry("Filme Avulso", "http://h/vod/x.avi", "VOD")))
        assertEquals(M3uKind.SERIES_EPISODE, M3uClassifier.classify(entry("Casa de Vidro T3 E9", "http://h/vod/x.mp4")))
    }

    @Test fun `streams sem extensao sao ao vivo, ts e m3u8 tambem`() {
        assertEquals(M3uKind.LIVE, M3uClassifier.classify(entry("Globo SP FHD", "http://h/u/p/1001", "Canais Abertos")))
        assertEquals(M3uKind.LIVE, M3uClassifier.classify(entry("Band HD", "http://h/u/p/1002.ts")))
        assertEquals(M3uKind.LIVE, M3uClassifier.classify(entry("Premiere 1", "http://h/u/p/2001.m3u8", "Esportes")))
    }

    @Test fun `palavras no grupo decidem quando nao ha extensao`() {
        assertEquals(M3uKind.MOVIE, M3uClassifier.classify(entry("Rota 116", "http://h/u/p/5", "Filmes | Ação")))
        assertEquals(M3uKind.SERIES_EPISODE, M3uClassifier.classify(entry("Algo", "http://h/u/p/6", "SÉRIES")))
        assertEquals(M3uKind.SERIES_EPISODE, M3uClassifier.classify(entry("Algo", "http://h/u/p/7", "Novelas")))
    }

    @Test fun `padrao de episodio no nome como ultimo recurso`() {
        assertEquals(M3uKind.SERIES_EPISODE, M3uClassifier.classify(entry("O Último Farol S01E02", "http://h/u/p/8")))
    }

    @Test fun `series pattern reconhece variantes`() {
        assertEquals(SeriesMatch("O Último Farol", 1, 2), SeriesPattern.match("O Último Farol S01E02"))
        assertEquals(SeriesMatch("O Último Farol", 1, 2), SeriesPattern.match("O Último Farol - S1 E2 - A tripulação"))
        assertEquals(SeriesMatch("Casa de Vidro", 3, 9), SeriesPattern.match("Casa de Vidro T3 E9"))
        assertEquals(SeriesMatch("Casa de Vidro", 3, 9), SeriesPattern.match("Casa de Vidro T03 EP09"))
        assertEquals(SeriesMatch("Noite em Lisboa", 1, 2), SeriesPattern.match("Noite em Lisboa 1x02"))
        assertEquals(SeriesMatch("Sertão", 2, 5), SeriesPattern.match("Sertão Temporada 2 Episódio 5"))
        assertNull(SeriesPattern.match("Rota 116 (2023)"))
        assertNull(SeriesPattern.match("S01E01"))
    }
}
