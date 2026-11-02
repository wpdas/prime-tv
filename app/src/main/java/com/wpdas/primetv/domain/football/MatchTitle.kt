package com.wpdas.primetv.domain.football

import com.wpdas.primetv.core.util.TextNormalizer

/** Partida reconhecida num nome: "Flamengo x Palmeiras". */
data class Match(val home: String, val away: String)

/** Reconhece "time x time" em nomes de canais e títulos de EPG. */
object MatchTitle {
    private val VERSUS = Regex("""^(.*?[\p{L}\d].*?)\s+(?:x|vs\.?|×|v)\s+(.*?[\p{L}\d].*?)$""", RegexOption.IGNORE_CASE)
    /** Prefixos que não são time: "VT - ", "AO VIVO: ", "Dia 04/11 - ", "18:30 - ", "Jogo 2 - ". */
    private val NOISE = Regex("""(?i)^(?:(?:vt|ao vivo|reprise|replay|compacto|melhores momentos|jogo\s*\d*|dia\s+\d{1,2}/\d{1,2}|\d{1,2}(?::|h)\d{2})\s*[-–:|]?\s*)+""")
    /** Corta o que vem depois de " - ", " | ", " ║" ou " (": "Atlético-MG (Premiere)" -> "Atlético-MG". */
    private val TRAILING = Regex("""\s+[-–|║]\s.*$|\s*\(.*$""")

    /** Extrai os dois times de um título; null quando não é uma partida. */
    fun parse(title: String): Match? {
        val cleaned = title.replace(NOISE, "").trim()
        val m = VERSUS.find(cleaned) ?: return null
        val home = m.groupValues[1].replace(TRAILING, "").trim().trimEnd('-', '–', ':')
        val away = m.groupValues[2].replace(TRAILING, "").trim()
        if (home.length < 2 || away.length < 2 || home.length > 40 || away.length > 40) return null
        return Match(home, away)
    }
}

/** Nomes de time como aparecem nas listas -> chave de comparação. */
object TeamNames {
    private val SUFFIXES = Regex("""\b(fc|ec|sc|ac|cf|afc|ssc|clube|club|futebol|esporte|sport|saf)\b""")
    private val SPACES = Regex("""\s+""")

    /** "SSC Napoli" -> "napoli"; "Vasco da Gama FC" -> "vasco da gama". */
    fun key(team: String): String = TextNormalizer.searchKey(team).replace(SUFFIXES, " ").replace(SPACES, " ").trim()

    /** Mesmo time escrito de dois jeitos: chaves iguais ou uma começa com a outra ("vasco" ~ "vasco da gama"). */
    fun same(a: String, b: String): Boolean {
        val ka = key(a)
        val kb = key(b)
        if (ka.isEmpty() || kb.isEmpty()) return false
        return ka == kb || ka.startsWith("$kb ") || kb.startsWith("$ka ")
    }
}
