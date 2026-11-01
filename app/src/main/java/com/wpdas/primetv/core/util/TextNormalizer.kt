package com.wpdas.primetv.core.util

import java.text.Normalizer

/**
 * Normalização de títulos para busca e para cruzar a lista com o TMDB:
 * minúsculas, sem acentos, sem tags de qualidade ("4K", "FHD", "[L]"), sem ano.
 */
object TextNormalizer {
    private val DIACRITICS = Regex("\\p{Mn}+")
    private val BRACKETS = Regex("""[\[\(\{][^\]\)\}]*[\]\)\}]""")
    private val QUALITY = Regex(
        """(?i)(?<![a-z0-9])(4k|uhd|fhd|full ?hd|hd|sd|hevc|h\.?26[45]|x26[45]|2160p|1080p|720p|480p|60 ?fps|hdr|dolby|dublado|dub|legendado|leg|nacional|pt-br|ptbr)(?![a-z0-9])""",
    )
    private val YEAR = Regex("""(?<!\d)(19|20)\d{2}(?!\d)""")
    private val NON_ALNUM = Regex("""[^a-z0-9 ]""")
    private val SPACES = Regex("""\s+""")
    /** " - 2026", " (2023)", ": 2019" no fim. Exige separador: "Blade Runner 2049" fica inteiro. */
    private val TRAILING_YEAR = Regex("""(?:\s*[\-–—|:]\s*[\(\[]?|\s*[\(\[])(19|20)\d{2}[\)\]]?\s*$""")

    fun stripAccents(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(DIACRITICS, "")

    /** Forma canônica para comparar títulos. Ex.: "A Queda 1 - 2022 UHD 4k" -> "a queda 1". */
    fun normalize(text: String): String =
        stripAccents(text.lowercase())
            .replace(BRACKETS, " ")
            .replace(QUALITY, " ")
            .replace(YEAR, " ")
            .replace(NON_ALNUM, " ")
            .replace(SPACES, " ")
            .trim()

    /** Forma leve para busca: minúsculas e sem acentos, mantendo números e ano. */
    fun searchKey(text: String): String =
        stripAccents(text.lowercase()).replace(NON_ALNUM, " ").replace(SPACES, " ").trim()

    /** Ano no título, se houver (ex.: "Rota 116 (2023)" -> 2023). */
    fun extractYear(text: String): Int? = YEAR.find(text)?.value?.toIntOrNull()

    /** Título para exibição: sem tags de qualidade, colchetes e ano no fim ("Rota 116 - 2023 (L)" -> "Rota 116"). */
    fun displayTitle(text: String): String =
        text.replace(BRACKETS, " ")
            .replace(QUALITY, " ")
            .replace(SPACES, " ")
            .trim()
            .replace(TRAILING_YEAR, "")
            .trim()
            .trimEnd('-', '·', '|', ':', '–')
            .trim()
            .ifEmpty { text.trim() }
}
