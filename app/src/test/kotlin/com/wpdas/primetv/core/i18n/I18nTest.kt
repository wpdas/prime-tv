package com.wpdas.primetv.core.i18n

import org.junit.Assert.assertEquals
import org.junit.Test

class I18nTest {

    private val source = object : I18nSource {
        private val files = mapOf(
            "pt-BR" to """{"a": "Olá", "b": "Faltam {n} min", "c": "Só em pt-BR"}""",
            "en" to """{"a": "Hello", "b": "{n} min left"}""",
        )
        override fun read(language: String) = files[language]
        override fun available() = files.keys.toList()
    }

    @Test
    fun `carrega o idioma padrao`() {
        val i18n = I18n(source).apply { loadBlocking() }
        assertEquals("Olá", i18n.t("a"))
        assertEquals("pt-BR", i18n.language.value)
    }

    @Test
    fun `substitui placeholders`() {
        val i18n = I18n(source).apply { loadBlocking() }
        assertEquals("Faltam 22 min", i18n.t("b", "n" to 22))
    }

    @Test
    fun `idioma escolhido sobrepoe e cai no fallback para chaves ausentes`() {
        val i18n = I18n(source).apply { loadBlocking("en") }
        assertEquals("Hello", i18n.t("a"))
        assertEquals("5 min left", i18n.t("b", "n" to 5))
        assertEquals("Só em pt-BR", i18n.t("c"))
    }

    @Test
    fun `idioma inexistente usa so o fallback`() {
        val i18n = I18n(source).apply { loadBlocking("xx") }
        assertEquals("Olá", i18n.t("a"))
    }

    @Test
    fun `chave desconhecida devolve a propria chave`() {
        val i18n = I18n(source).apply { loadBlocking() }
        assertEquals("home.nope", i18n.t("home.nope"))
    }

    @Test
    fun `lista idiomas disponiveis`() {
        assertEquals(listOf("pt-BR", "en"), I18n(source).availableLanguages())
    }
}
