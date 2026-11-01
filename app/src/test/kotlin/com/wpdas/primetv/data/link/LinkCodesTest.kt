package com.wpdas.primetv.data.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkCodesTest {
    @Test fun `codigo de 6 caracteres sem ambiguidades`() {
        repeat(200) {
            val code = LinkCodes.generate()
            assertEquals(6, code.length)
            assertTrue(code, code.none { it in "01OIL" })
            assertTrue(code, code.all { it.isUpperCase() || it.isDigit() })
        }
    }

    @Test fun `formato com hifen e url do site`() {
        assertEquals("K7P-4QX", LinkCodes.format("K7P4QX"))
        assertEquals("ABC", LinkCodes.format("ABC"))
        assertTrue(LinkCodes.urlFor("K7P4QX").endsWith("?code=K7P4QX"))
    }
}
