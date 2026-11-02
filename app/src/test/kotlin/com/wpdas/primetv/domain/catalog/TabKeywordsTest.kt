package com.wpdas.primetv.domain.catalog

import com.wpdas.primetv.domain.model.HomeTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TabKeywordsTest {
    @Test fun `categorias infantis viram Kids`() {
        assertEquals(HomeTab.KIDS, TabKeywords.tabOf("INFANTIS"))
        assertEquals(HomeTab.KIDS, TabKeywords.tabOf("DESENHOS 24 HORAS"))
        assertEquals(HomeTab.KIDS, TabKeywords.tabOf("ANIMACAO/INFANTIL"))
        assertEquals(HomeTab.KIDS, TabKeywords.tabOf("Desenhos"))
        assertEquals(HomeTab.KIDS, TabKeywords.tabOf("Kids & Família"))
        assertEquals(HomeTab.KIDS, TabKeywords.tabOf("Canais Infantis HD"))
    }

    @Test fun `categorias de anime viram Anime, mesmo com palavra infantil junto`() {
        assertEquals(HomeTab.ANIME, TabKeywords.tabOf("ANIMES 24 HORAS"))
        assertEquals(HomeTab.ANIME, TabKeywords.tabOf("ANIMES"))
        assertEquals(HomeTab.ANIME, TabKeywords.tabOf("CRUNCHYROLL"))
        assertEquals(HomeTab.ANIME, TabKeywords.tabOf("Animes Infantis"))
    }

    @Test fun `categorias de novela viram Novelas`() {
        assertEquals(HomeTab.NOVELAS, TabKeywords.tabOf("NOVELAS"))
        assertEquals(HomeTab.NOVELAS, TabKeywords.tabOf("Novelas Globo"))
        assertEquals(HomeTab.NOVELAS, TabKeywords.tabOf("Telenovelas"))
        assertNull(TabKeywords.tabOf("DORAMA"))
    }

    @Test fun `o resto nao entra em nenhuma`() {
        assertNull(TabKeywords.tabOf("ANIMACAO"))
        assertNull(TabKeywords.tabOf("DORAMA"))
        assertNull(TabKeywords.tabOf("NETFLIX"))
        assertNull(TabKeywords.tabOf("Animais Selvagens"))
        assertNull(TabKeywords.tabOf("Kidnapped"))
        assertNull(TabKeywords.tabOf(null))
    }
}
