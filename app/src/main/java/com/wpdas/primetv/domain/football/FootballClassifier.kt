package com.wpdas.primetv.domain.football

import com.wpdas.primetv.core.util.TextNormalizer

/**
 * Decide se um canal/filme/evento é de futebol pelo nome ou pela categoria:
 * palavras-chave configuráveis (sem acentos, minúsculas) ou o padrão "time x time".
 */
class FootballClassifier(keywords: List<String> = DEFAULT_KEYWORDS) {

    /** Cada palavra-chave vira um regex com limite de palavra: "série a" não casa com "série animada". */
    private val needles: List<Regex> = keywords
        .map { TextNormalizer.stripAccents(it.lowercase()).trim() }
        .filter { it.isNotEmpty() }
        .map { Regex("(?<![a-z0-9])" + Regex.escape(it) + "(?![a-z0-9])") }

    fun matches(name: String?, category: String?): Boolean {
        if (matchesKeywords(name, category)) return true
        val n = name?.let { TextNormalizer.stripAccents(it.lowercase()) }.orEmpty()
        return VERSUS.containsMatchIn(n)
    }

    /** Só pelas palavras-chave (sem a regra "time x time"): canais que são de futebol por natureza. */
    fun matchesKeywords(name: String?, category: String?): Boolean {
        val n = name?.let { TextNormalizer.stripAccents(it.lowercase()) }.orEmpty()
        val c = category?.let { TextNormalizer.stripAccents(it.lowercase()) }.orEmpty()
        return needles.any { it.containsMatchIn(n) || it.containsMatchIn(c) }
    }

    companion object {
        val DEFAULT_KEYWORDS = listOf(
            "futebol", "premiere", "brasileirão", "libertadores", "copa do brasil", "champions",
            "premier league", "laliga", "série a", "série b", "estaduais",
        )

        /** "Flamengo x Palmeiras", "Real Madrid vs Barcelona", "Grêmio × Inter". Exige nomes com letras dos dois lados. */
        private val VERSUS = Regex("""(?<![a-z0-9])[a-z][a-z .'-]{2,}\s+(x|vs\.?|×)\s+[a-z][a-z .'-]{2,}""")
    }
}

/** Fornece o classificador com as palavras-chave vigentes. */
fun interface FootballClassifierProvider {
    fun get(): FootballClassifier
}
