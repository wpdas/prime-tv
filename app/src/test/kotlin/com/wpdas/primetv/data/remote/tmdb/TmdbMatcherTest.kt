package com.wpdas.primetv.data.remote.tmdb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbMatcherTest {
    @Test fun `chaves de busca incluem titulo pt-BR, original e versoes sem numeros soltos`() {
        val keys = TmdbMatcher.keysFor("Apocalipse Z: Novo Mundo", "Apocalipsis Z: Nuevo mundo")
        assertEquals(listOf("apocalipse z novo mundo", "apocalipsis z nuevo mundo"), keys)
        assertEquals("apocalipse z novo mundo", TmdbMatcher.loose("apocalipse z 2 novo mundo"))
        assertEquals("blade runner 2049", TmdbMatcher.loose("blade runner 2049"))
    }

    @Test fun `prefere o candidato do mesmo ano, depois vizinho, depois sem ano`() {
        val c = listOf(
            TmdbMatcher.Candidate("a", "rota 116", 2019),
            TmdbMatcher.Candidate("b", "rota 116", 2023),
            TmdbMatcher.Candidate("c", "rota 116", null),
        )
        assertEquals("b", TmdbMatcher.pick(c, 2023)?.id)
        assertEquals("b", TmdbMatcher.pick(c, 2024)?.id)
        assertEquals("c", TmdbMatcher.pick(c, 2010)?.id)
        assertEquals("a", TmdbMatcher.pick(c, null)?.id)
        assertNull(TmdbMatcher.pick(emptyList(), 2023))
    }

    @Test fun `escolhe o resultado remoto certo e rejeita o que nao casa`() {
        val results = listOf(
            TmdbListItem(id = 1, title = "Rota 66", releaseDate = "2023-01-01"),
            TmdbListItem(id = 2, title = "Rota 116", releaseDate = "2019-05-01"),
            TmdbListItem(id = 3, title = "Rota 116", releaseDate = "2023-05-01"),
        )
        assertEquals(3, TmdbMatcher.pickRemote("Rota 116 (2023) [L]", 2023, results)?.id)
        assertEquals(2, TmdbMatcher.pickRemote("Rota 116", 2019, results)?.id)
        assertNull(TmdbMatcher.pickRemote("Outro Filme", 2023, results))
        assertTrue(TmdbMatcher.pickRemote("Apocalipse Z 2 Novo Mundo", 2026, listOf(TmdbListItem(id = 9, title = "Apocalipse Z: Novo Mundo", releaseDate = "2026-10-09")))?.id == 9)
    }
}
