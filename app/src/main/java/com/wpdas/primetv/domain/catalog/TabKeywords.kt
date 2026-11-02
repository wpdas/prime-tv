package com.wpdas.primetv.domain.catalog

import com.wpdas.primetv.core.util.TextNormalizer
import com.wpdas.primetv.domain.model.HomeTab

/**
 * Abas Kids e Anime: decididas só pelo nome da categoria da lista (nunca pelo título do item),
 * com limite de palavra e sem acentos. "Animação/Infantil" é Kids por "infantil"; "Animes 24 horas"
 * é Anime; uma categoria "Animação" adulta não entra em nenhuma.
 */
object TabKeywords {
    val KIDS = listOf(
        "infantil", "infantis", "kids", "kid", "crianca", "criancas", "desenho", "desenhos", "cartoon", "cartoons",
        "baby", "junior", "gloob", "cartoonito", "nick jr", "discovery kids", "zoomoo",
    )
    val ANIME = listOf("anime", "animes", "crunchyroll", "otaku")

    private val kids = KIDS.map(::needle)
    private val anime = ANIME.map(::needle)

    private fun needle(word: String) = Regex("(?<![a-z0-9])" + Regex.escape(TextNormalizer.stripAccents(word.lowercase())) + "(?![a-z0-9])")

    /** Aba a que a categoria pertence, ou null. Anime vence quando as duas batem. */
    fun tabOf(categoryName: String?): HomeTab? {
        val c = categoryName?.let { TextNormalizer.stripAccents(it.lowercase()) }.orEmpty()
        if (c.isEmpty()) return null
        if (anime.any { it.containsMatchIn(c) }) return HomeTab.ANIME
        if (kids.any { it.containsMatchIn(c) }) return HomeTab.KIDS
        return null
    }

    fun isKids(categoryName: String?) = tabOf(categoryName) == HomeTab.KIDS
    fun isAnime(categoryName: String?) = tabOf(categoryName) == HomeTab.ANIME
}
