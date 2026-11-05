package com.wpdas.primetv.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VariantsTest {
    private data class V(val id: String, val name: String, val rank: Int)
    private val list = listOf(V("a", "History FHD", 1), V("b", "History FHD²", 1), V("c", "History HD", 2), V("d", "History HD²", 2))
    private fun next(current: V) = Variants.next(list, current.id, current.rank, { it.id }, { it.rank })

    @Test fun `servidor repetido da mesma qualidade vem antes da qualidade abaixo`() {
        assertEquals("b", next(list[0])?.id)
        assertEquals("c", next(list[1])?.id)
        assertEquals("d", next(list[2])?.id)
        assertNull(next(list[3]))
    }

    @Test fun `atual fora da lista cai para a primeira qualidade pior`() {
        assertEquals("c", Variants.next(list, "x", 1, { it.id }, { it.rank })?.id)
        assertNull(Variants.next(list, "x", 2, { it.id }, { it.rank }))
    }

    @Test fun `rotulo traz o marcador de servidor repetido`() {
        assertEquals("FHD", Variants.label("History FHD", 1))
        assertEquals("FHD²", Variants.label("History FHD²", 1))
        assertEquals("²", Variants.label("Canal²", 4))
        assertNull(Variants.label("Canal", 4))
    }
}
