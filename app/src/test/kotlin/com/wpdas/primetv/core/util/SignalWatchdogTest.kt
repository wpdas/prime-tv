package com.wpdas.primetv.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalWatchdogTest {
    private val dog = SignalWatchdog(timeoutMs = 6_000)

    @Test
    fun `nada chega desde a sintonia - dispara uma vez ao estourar o tempo`() {
        dog.start(0)
        assertFalse(dog.onTick(3_000, receivingData = false))
        assertFalse(dog.onTick(5_999, receivingData = false))
        assertTrue(dog.onTick(6_000, receivingData = false))
        // Já disparou: fica quieto até o próximo start.
        assertFalse(dog.onTick(20_000, receivingData = false))
    }

    @Test
    fun `dados que chegam adiam o prazo e dados que param disparam`() {
        dog.start(0)
        assertFalse(dog.onTick(4_000, receivingData = true))
        assertFalse(dog.onTick(9_000, receivingData = false)) // 5 s depois do último dado
        assertTrue(dog.onTick(10_000, receivingData = false)) // 6 s
    }

    @Test
    fun `stop desarma e start rearma`() {
        dog.start(0)
        dog.stop()
        assertFalse(dog.onTick(60_000, receivingData = false))
        dog.start(60_000)
        assertFalse(dog.onTick(65_000, receivingData = false))
        assertTrue(dog.onTick(66_000, receivingData = false))
    }

    @Test
    fun `sem start nunca dispara`() {
        assertFalse(dog.onTick(100_000, receivingData = false))
    }
}
