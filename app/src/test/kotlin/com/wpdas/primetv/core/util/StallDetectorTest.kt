package com.wpdas.primetv.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StallDetectorTest {
    @Test
    fun `dispara so no terceiro travamento dentro da janela`() {
        val d = StallDetector(limit = 3, windowMs = 90_000)
        assertFalse(d.onStall(0))
        assertFalse(d.onStall(10_000))
        assertTrue(d.onStall(20_000))
    }

    @Test
    fun `travamentos espacados nao disparam`() {
        val d = StallDetector(limit = 3, windowMs = 90_000)
        assertFalse(d.onStall(0))
        assertFalse(d.onStall(60_000))
        assertFalse(d.onStall(130_000)) // o primeiro já saiu da janela
        assertTrue(d.onStall(140_000))
    }

    @Test
    fun `zera depois de disparar`() {
        val d = StallDetector(limit = 2, windowMs = 90_000)
        assertFalse(d.onStall(0))
        assertTrue(d.onStall(1_000))
        assertFalse(d.onStall(2_000))
        assertTrue(d.onStall(3_000))
    }
}
