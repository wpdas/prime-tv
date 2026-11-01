package com.wpdas.primetv.data.remote.m3u

/** Resultado de reconhecer "Nome da Série S01E02" e variantes. */
data class SeriesMatch(val seriesTitle: String, val season: Int, val episode: Int)

/** Reconhece `S01E02`, `S1 E2`, `T1E2`, `T01 EP02`, `1x02`, `Temporada 1 Episódio 2`. */
object SeriesPattern {
    private val PATTERNS = listOf(
        Regex("""(?i)^(.*?)[\s\-–—._:]*\bS(\d{1,2})\s*[\-._ ]?\s*E[P]?\s*(\d{1,3})\b.*$"""),
        Regex("""(?i)^(.*?)[\s\-–—._:]*\bT(\d{1,2})\s*[\-._ ]?\s*E[P]?\s*(\d{1,3})\b.*$"""),
        Regex("""(?i)^(.*?)[\s\-–—._:]*\b(\d{1,2})x(\d{1,3})\b.*$"""),
        Regex("""(?i)^(.*?)[\s\-–—._:]*\btemporada\s*(\d{1,2})\D{0,12}?(?:epis[oó]dio|ep)\s*(\d{1,3})\b.*$"""),
    )
    private val TRAILING_SEPARATORS = Regex("""[\s\-–—._:|]+$""")

    fun match(name: String): SeriesMatch? {
        val clean = name.trim()
        for (pattern in PATTERNS) {
            val m = pattern.find(clean) ?: continue
            val title = m.groupValues[1].replace(TRAILING_SEPARATORS, "").trim()
            val season = m.groupValues[2].toIntOrNull() ?: continue
            val episode = m.groupValues[3].toIntOrNull() ?: continue
            if (title.isEmpty()) continue
            return SeriesMatch(title, season, episode)
        }
        return null
    }
}
