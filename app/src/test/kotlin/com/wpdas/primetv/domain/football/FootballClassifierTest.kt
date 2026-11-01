package com.wpdas.primetv.domain.football

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FootballClassifierTest {
    private val classifier = FootballClassifier()

    @Test fun `palavras-chave no nome ou na categoria, sem acento e sem caixa`() {
        assertTrue(classifier.matches("Premiere 1 FHD", "Esportes"))
        assertTrue(classifier.matches("Canal 5", "FUTEBOL AO VIVO"))
        assertTrue(classifier.matches("Rodada do Brasileirao", null))
        assertTrue(classifier.matches("Final da Champions 2024", "Filmes"))
        assertTrue(classifier.matches("Série A Italiana", null))
    }

    @Test fun `padrao time x time`() {
        assertTrue(classifier.matches("Flamengo x Palmeiras", null))
        assertTrue(classifier.matches("Real Madrid vs Barcelona", null))
        assertTrue(classifier.matches("Grêmio × Inter", null))
        assertTrue(classifier.matches("JOGO: Santos X Bahia", null))
    }

    @Test fun `nao marca o que nao e futebol`() {
        assertFalse(classifier.matches("Globo SP HD", "Abertos"))
        assertFalse(classifier.matches("Max Steel", "Infantil"))
        assertFalse(classifier.matches("Canal 4K x265", null))
        assertFalse(classifier.matches("Xuxa", "Infantil"))
        assertFalse(classifier.matches("Casa de Vidro S01E02", "Séries"))
        assertFalse(classifier.matches("Batman: a série animada", "Infantil"))
        assertFalse(classifier.matches("Premiereira", null))
    }

    @Test fun `palavras personalizadas`() {
        val custom = FootballClassifier(listOf("Copa do Nordeste"))
        assertTrue(custom.matches("COPA DO NORDESTE: Final", null))
        assertFalse(custom.matches("Premiere 1", null))
    }
}
