package com.wpdas.primetv.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DisplayModePickerTest {
    private val uhd60 = DisplayMode(1, 3840, 2160, 60f)
    private val uhd30 = DisplayMode(2, 3840, 2160, 30f)
    private val fhd60 = DisplayMode(3, 1920, 1080, 60f)
    private val fhd50 = DisplayMode(4, 1920, 1080, 50f)
    private val hd60 = DisplayMode(5, 1280, 720, 60f)
    private val sd = DisplayMode(6, 720, 576, 50f)
    private val modes = listOf(uhd60, uhd30, fhd60, fhd50, hd60, sd)

    @Test fun `video 1080p numa saida 4K desce para 1080p na mesma taxa`() {
        assertEquals(fhd60, DisplayModePicker.pick(modes, uhd60, 1920, 1080))
    }

    @Test fun `video 720p vai para 720p e SD nunca desce de 720`() {
        assertEquals(hd60, DisplayModePicker.pick(modes, uhd60, 1280, 720))
        assertEquals(hd60, DisplayModePicker.pick(modes, uhd60, 720, 576))
    }

    @Test fun `ja no modo certo ou sem tamanho nao mexe`() {
        assertNull(DisplayModePicker.pick(modes, fhd60, 1920, 1080))
        assertNull(DisplayModePicker.pick(modes, uhd60, 0, 0))
    }

    @Test fun `video maior que todos os modos fica como esta`() {
        assertNull(DisplayModePicker.pick(listOf(fhd60, hd60), fhd60, 3840, 2160))
    }

    @Test fun `sem modo na mesma taxa aceita outra taxa`() {
        assertEquals(fhd50, DisplayModePicker.pick(listOf(uhd30, fhd50), uhd30, 1920, 1080))
    }
}
