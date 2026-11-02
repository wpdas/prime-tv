package com.wpdas.primetv.core.util

/**
 * Qualidade declarada no nome ("Globo UHD 4K", "Band FHD", "SBT SD"). Quanto menor o
 * [rank], melhor. Usado para mostrar só a melhor variante de cada canal/título.
 */
object Quality {
    const val RANK_4K = 0
    const val RANK_FHD = 1
    const val RANK_HD = 2
    const val RANK_SD = 3
    const val RANK_UNKNOWN = 4

    private val TAG = Regex("""(?i)(?<![a-z0-9])(4k|uhd|2160p|fhd|full ?hd|1080p|hd|720p|sd|480p)(?![a-z0-9])""")

    fun rank(name: String): Int {
        var best = RANK_UNKNOWN
        for (m in TAG.findAll(name)) {
            val r = when (m.value.lowercase().replace(" ", "")) {
                "4k", "uhd", "2160p" -> RANK_4K
                "fhd", "fullhd", "1080p" -> RANK_FHD
                "hd", "720p" -> RANK_HD
                else -> RANK_SD
            }
            if (r < best) best = r
        }
        return best
    }

    fun label(rank: Int): String? = when (rank) {
        RANK_4K -> "4K"
        RANK_FHD -> "FHD"
        RANK_HD -> "HD"
        RANK_SD -> "SD"
        else -> null
    }

    /** Marcador de servidor repetido que alguns provedores põem no fim ("HBO FHD²"). */
    private val DUP_MARK = Regex("""[²³⁴]""")
    private val SPACES = Regex("""\s+""")

    /**
     * Chave de agrupamento estrita: dois nomes só caem no mesmo grupo se forem iguais tirando
     * a tag de qualidade e o marcador de servidor repetido. Caixa, acentos e espaços extras não
     * contam; todo o resto conta ("HBO+", "HBO Legendado", "Ben 10 2006" são grupos próprios).
     * Em dúvida, mostrar dois itens é melhor do que esconder um canal.
     */
    fun groupKey(name: String): String =
        TextNormalizer.stripAccents(name.lowercase())
            .replace(TAG, " ")
            .replace(DUP_MARK, " ")
            .replace(SPACES, " ")
            .trim()
            .ifEmpty { TextNormalizer.searchKey(name) }
}
