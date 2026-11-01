package com.wpdas.primetv.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogQueryTest {
    @Test fun `monta consulta FTS por prefixo`() {
        assertEquals("glob*", CatalogRepositoryImpl.buildMatchQuery("Glob"))
        assertEquals("o* ultimo* farol*", CatalogRepositoryImpl.buildMatchQuery("O Último Farol "))
        assertEquals("rota* 116*", CatalogRepositoryImpl.buildMatchQuery("rota (116)"))
        assertNull(CatalogRepositoryImpl.buildMatchQuery("   "))
        assertNull(CatalogRepositoryImpl.buildMatchQuery("\"'*"))
    }

    @Test fun `duracao hh mm ss`() {
        assertEquals(2700, CatalogRepositoryImpl.parseDuration("00:45:00"))
        assertEquals(125, CatalogRepositoryImpl.parseDuration("2:05"))
        assertNull(CatalogRepositoryImpl.parseDuration("x"))
        assertNull(CatalogRepositoryImpl.parseDuration(null))
    }
}
