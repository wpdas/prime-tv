package com.wpdas.primetv.ui.components

import com.wpdas.primetv.ui.screens.player.QualityKind
import com.wpdas.primetv.ui.screens.player.QualityOptions
import com.wpdas.primetv.ui.screens.player.SourceVariant
import com.wpdas.primetv.ui.screens.player.VideoTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityOptionsTest {
    private val tracks = listOf(VideoTrack("0:0", 1080, 5_000_000), VideoTrack("0:1", 720, 2_500_000), VideoTrack("0:2", 720, 1_800_000), VideoTrack("0:3", 360, 600_000))
    private val sources = listOf(SourceVariant("c-fhd", "FHD"), SourceVariant("c-hd", "HD"))

    @Test fun `faixas distintas por altura, Auto primeiro e selecionado sem escolha manual`() {
        val options = QualityOptions.build(tracks, manualTrackId = null, sources = emptyList(), playingSourceId = null)
        assertEquals(listOf("Auto", "1080p", "720p", "360p"), options.map { it.label })
        assertEquals(QualityKind.AUTO, options[0].kind)
        assertTrue(options[0].selected)
        assertEquals("0:1", options[2].id) // a de maior taxa entre as duas de 720p
    }

    @Test fun `escolha manual marca a faixa e desmarca Auto`() {
        val options = QualityOptions.build(tracks, manualTrackId = "0:3", sources = emptyList(), playingSourceId = null)
        assertEquals(listOf(false, false, false, true), options.map { it.selected })
    }

    @Test fun `variantes da lista entram depois das faixas e uma so faixa nao gera secao`() {
        val options = QualityOptions.build(listOf(VideoTrack("0:0", 1080, 1)), null, sources, "c-hd")
        assertEquals(listOf("FHD", "HD"), options.map { it.label })
        assertEquals(listOf(false, true), options.map { it.selected })
        assertTrue(options.all { it.kind == QualityKind.SOURCE })
        assertTrue(QualityOptions.build(emptyList(), null, listOf(sources[0]), "c-fhd").isEmpty())
    }
}
