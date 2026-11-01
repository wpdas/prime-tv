package com.wpdas.primetv.data.remote.tmdb

import com.wpdas.primetv.core.util.TextNormalizer

/**
 * Cruza um título do TMDB com itens da lista pelo título normalizado (pt-BR e original)
 * e, quando houver, pelo ano. Puro para ser testável.
 */
object TmdbMatcher {
    /** Candidato da lista: id, título normalizado e ano. */
    data class Candidate(val id: String, val normalizedTitle: String, val year: Int?)

    private val SMALL_NUMBERS = Regex("""(?<![a-z0-9])\d{1,2}(?![a-z0-9])""")
    private val SPACES = Regex("""\s+""")

    /** Chaves de busca em ordem de preferência: pt-BR exato, original exato, versões sem numerais soltos. */
    fun keysFor(title: String?, originalTitle: String?): List<String> {
        val exact = listOfNotNull(title, originalTitle).map { TextNormalizer.normalize(it) }.filter { it.isNotEmpty() }
        val loose = exact.map { loose(it) }.filter { it.isNotEmpty() }
        return (exact + loose).distinct()
    }

    /** "apocalipse z 2 novo mundo" -> "apocalipse z novo mundo": tolera numeração de sequência que a lista inventa. */
    fun loose(normalized: String): String = normalized.replace(SMALL_NUMBERS, " ").replace(SPACES, " ").trim()

    /** Escolhe o melhor candidato: mesmo ano, depois ano vizinho, depois qualquer um. */
    fun pick(candidates: List<Candidate>, year: Int?): Candidate? {
        if (candidates.isEmpty()) return null
        if (year == null) return candidates.first()
        return candidates.firstOrNull { it.year == year }
            ?: candidates.firstOrNull { it.year != null && kotlin.math.abs(it.year - year) <= 1 }
            ?: candidates.firstOrNull { it.year == null }
            ?: candidates.first()
    }

    /** Resultado da busca TMDB -> melhor item para um título da lista, com a mesma tolerância. */
    fun pickRemote(listTitle: String, listYear: Int?, results: List<TmdbListItem>): TmdbListItem? {
        if (results.isEmpty()) return null
        val wanted = TextNormalizer.normalize(listTitle)
        val wantedLoose = loose(wanted)
        fun score(item: TmdbListItem): Int {
            val keys = keysFor(item.displayTitle, item.original)
            var s = 0
            // Sem o título bater, o ano sozinho não vale: evita casar "Outro Filme" só por ser do mesmo ano.
            if (wanted in keys) s += 4 else if (wantedLoose in keys.map { loose(it) }) s += 2 else return -10
            val itemYear = item.year
            if (listYear != null && itemYear != null) {
                if (itemYear == listYear) s += 3 else if (kotlin.math.abs(itemYear - listYear) <= 1) s += 1 else s -= 2
            }
            return s
        }
        val best = results.maxByOrNull { score(it) } ?: return null
        return if (score(best) > 0) best else null
    }
}
