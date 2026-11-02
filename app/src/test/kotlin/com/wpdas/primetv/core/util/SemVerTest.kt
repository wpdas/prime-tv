package com.wpdas.primetv.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerTest {
    @Test fun `parse aceita v, partes faltando e pre-release`() {
        assertEquals(SemVer(1, 2, 3), SemVer.parse("v1.2.3"))
        assertEquals(SemVer(1, 2, 0), SemVer.parse("1.2"))
        assertEquals(SemVer(1, 2, 3, "dev.4"), SemVer.parse("1.2.3-dev.4"))
        assertEquals(SemVer(1, 0, 0, "beta.1"), SemVer.parse("v1.0.0-beta.1+build7"))
        assertNull(SemVer.parse("abc"))
        assertNull(SemVer.parse(null))
    }

    @Test fun `comparacao e isNewer`() {
        assertTrue(SemVer.isNewer("v1.0.1", "1.0.0"))
        assertTrue(SemVer.isNewer("v1.1.0", "1.0.9"))
        assertTrue(SemVer.isNewer("v2.0.0", "1.9.9"))
        assertFalse(SemVer.isNewer("v1.0.0", "1.0.0"))
        assertFalse(SemVer.isNewer("v0.9.9", "1.0.0"))
        // build local 1.0.0-dev.3 é anterior à release 1.0.0
        assertTrue(SemVer.isNewer("v1.0.0", "1.0.0-dev.3"))
        assertFalse(SemVer.isNewer("v1.0.0", "1.0.1-dev.3"))
        // versão de desenvolvimento 0.0.0-dev recebe qualquer release
        assertTrue(SemVer.isNewer("v0.1.0", "0.0.0-dev"))
        assertFalse(SemVer.isNewer("lixo", "1.0.0"))
    }

    @Test
    fun `build de desenvolvimento depois da tag nao e tratado como desatualizado`() {
        assertFalse(SemVer.isNewer("v0.0.2", "0.0.2-dev.4"))
        assertTrue(SemVer.isNewer("v0.0.3", "0.0.2-dev.4"))
        assertTrue(SemVer.isNewer("v0.0.2", "0.0.2-rc.1"))
    }
}
