package com.wpdas.primetv.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextNormalizerTest {
    @Test fun `remove acentos, qualidade, ano e colchetes`() {
        assertEquals("a queda 1", TextNormalizer.normalize("A Queda 1 - 2022 UHD 4k"))
        assertEquals("rota 116", TextNormalizer.normalize("Rota 116 [L] FHD"))
        assertEquals("o ultimo farol", TextNormalizer.normalize("O Último Farol (2024) H.265"))
        assertEquals("globo sp", TextNormalizer.normalize("Globo SP HD"))
    }

    @Test fun `nao remove hd dentro de palavra`() {
        assertEquals("shdx", TextNormalizer.normalize("SHDX"))
    }

    @Test fun `extrai ano`() {
        assertEquals(2023, TextNormalizer.extractYear("Rota 116 (2023)"))
        assertNull(TextNormalizer.extractYear("Rota 116"))
        assertNull(TextNormalizer.extractYear("Canal 20245"))
    }

    @Test fun `chave de busca mantem numeros e ano`() {
        assertEquals("rota 116 2023", TextNormalizer.searchKey("Rota 116 (2023)"))
        assertEquals("sertao profundo", TextNormalizer.searchKey("Sertão Profundo"))
    }

    @Test fun `titulo de exibicao sem tags`() {
        assertEquals("A Queda 1", TextNormalizer.displayTitle("A Queda 1 - 2022 UHD 4k"))
        assertEquals("Rota 116", TextNormalizer.displayTitle("Rota 116 [L] FHD"))
        assertEquals("Mutante Destino Sombrio", TextNormalizer.displayTitle("Mutante Destino Sombrio - 2026 (L)"))
        assertEquals("Amor Extremo", TextNormalizer.displayTitle("Amor Extremo - 2008"))
        assertEquals("2012", TextNormalizer.displayTitle("2012"))
        assertEquals("Blade Runner 2049", TextNormalizer.displayTitle("Blade Runner 2049"))
    }
}
