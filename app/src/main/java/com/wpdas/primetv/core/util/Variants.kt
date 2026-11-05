package com.wpdas.primetv.core.util

/**
 * Ordem de tentativa entre as variantes de um mesmo canal/filme. A lista já vem da melhor para a
 * pior qualidade e, dentro da mesma qualidade, pela ordem da lista ("FHD", "FHD²", "HD", "HD²").
 */
object Variants {
    /**
     * A variante seguinte à que está tocando: primeiro o servidor repetido da mesma qualidade
     * ("FHD²"), só depois a qualidade abaixo. Se a atual não está na lista, a primeira pior que ela.
     */
    fun <T> next(variants: List<T>, currentId: String, currentRank: Int, id: (T) -> String, rank: (T) -> Int): T? {
        val index = variants.indexOfFirst { id(it) == currentId }
        return if (index >= 0) variants.getOrNull(index + 1) else variants.firstOrNull { rank(it) > currentRank }
    }

    /** Rótulo da variante para avisos e menu: qualidade mais o marcador de servidor repetido ("FHD²"). */
    fun label(name: String, rank: Int): String? {
        val mark = Quality.dupMark(name)
        val quality = Quality.label(rank)
        return if (quality == null && mark == null) null else quality.orEmpty() + mark.orEmpty()
    }
}
