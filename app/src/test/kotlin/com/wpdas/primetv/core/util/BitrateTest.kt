package com.wpdas.primetv.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class BitrateTest {
    @Test fun `kilobits abaixo de um megabit, megabits com uma casa e virgula`() {
        assertEquals("850 kb/s", Bitrate.format(850_000))
        assertEquals("2,4 Mb/s", Bitrate.format(2_400_000))
        assertEquals("12,0 Mb/s", Bitrate.format(12_000_000))
        assertEquals("0 kb/s", Bitrate.format(-5))
    }
}
