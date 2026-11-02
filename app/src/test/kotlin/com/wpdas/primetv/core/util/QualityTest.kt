package com.wpdas.primetv.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class QualityTest {
    @Test fun `rank pela tag no nome, prefixo ou sufixo`() {
        assertEquals(Quality.RANK_4K, Quality.rank("Globo UHD 4K"))
        assertEquals(Quality.RANK_4K, Quality.rank("UHD Globo"))
        assertEquals(Quality.RANK_FHD, Quality.rank("Globo SP FHD"))
        assertEquals(Quality.RANK_FHD, Quality.rank("Globo Full HD"))
        assertEquals(Quality.RANK_HD, Quality.rank("Band HD"))
        assertEquals(Quality.RANK_SD, Quality.rank("SBT SD"))
        assertEquals(Quality.RANK_UNKNOWN, Quality.rank("Globo News"))
        assertEquals(Quality.RANK_UNKNOWN, Quality.rank("SHDX"))
    }

    @Test fun `variantes do mesmo canal compartilham a chave`() {
        val key = Quality.groupKey("Globo SP FHD")
        assertEquals(key, Quality.groupKey("Globo SP HD"))
        assertEquals(key, Quality.groupKey("Globo SP UHD 4K"))
        assertEquals(key, Quality.groupKey("UHD Globo SP"))
        assertEquals("globo sp", key)
        assert(Quality.groupKey("Globo RJ HD") != key)
    }

    @Test
    fun `agrupamento e estrito - so a tag de qualidade e o marcador de servidor sao ignorados`() {
        assertEquals(Quality.groupKey("HBO+ FHD"), Quality.groupKey("HBO+ HD²"))
        assertEquals(Quality.groupKey("Globo São Paulo FHD"), Quality.groupKey("globo sao paulo  HD"))
        assert(Quality.groupKey("HBO+ FHD") != Quality.groupKey("HBO FHD"))
        assert(Quality.groupKey("SPORT TV+") != Quality.groupKey("SPORT TV"))
        assert(Quality.groupKey("HBO FHD Legendado") != Quality.groupKey("HBO FHD"))
        assert(Quality.groupKey("Ben 10 2006 - 2008") != Quality.groupKey("Ben 10"))
        assert(Quality.groupKey("Filme - 2024 (L)") != Quality.groupKey("Filme - 2024"))
    }
}
