package com.wpdas.primetv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumeLogicTest {
    private fun ep(id: String, season: Int, number: Int) = Episode(id, "s1", season, number, "E$number", "http://x/$id.mp4", "mp4", 1500, null, null)
    private val episodes = listOf(ep("e1", 1, 1), ep("e2", 1, 2), ep("e3", 2, 1))
    private fun progress(id: String, pos: Long, finished: Boolean) = WatchProgress(id, MediaType.SERIES, "s1", pos, 1_500_000, 1L, finished)

    @Test fun `sem historico comeca do primeiro`() {
        assertEquals(SeriesResume(episodes[0], 0L), ResumeLogic.resume(null, episodes))
    }

    @Test fun `episodio em andamento retoma na posicao`() {
        assertEquals(SeriesResume(episodes[1], 600_000L), ResumeLogic.resume(progress("e2", 600_000L, false), episodes))
    }

    @Test fun `episodio concluido aponta para o seguinte, mesmo em outra temporada`() {
        assertEquals(SeriesResume(episodes[2], 0L), ResumeLogic.resume(progress("e2", 1_500_000L, true), episodes))
    }

    @Test fun `ultimo episodio concluido encerra a serie`() {
        assertNull(ResumeLogic.resume(progress("e3", 1_500_000L, true), episodes))
    }

    @Test fun `episodio que sumiu da lista volta ao inicio`() {
        assertEquals(SeriesResume(episodes[0], 0L), ResumeLogic.resume(progress("x", 10L, false), episodes))
    }

    @Test fun `assistido acima de 90 por cento`() {
        assertTrue(ResumeLogic.isFinished(91_000, 100_000))
        assertFalse(ResumeLogic.isFinished(89_000, 100_000))
        assertFalse(ResumeLogic.isFinished(10, 0))
        assertEquals(0.5f, WatchProgress("a", MediaType.MOVIE, null, 50, 100, 0, false).fraction, 0.001f)
    }
}
