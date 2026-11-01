package com.wpdas.primetv.data.remote.m3u

import com.wpdas.primetv.core.util.TextNormalizer

enum class M3uKind { LIVE, MOVIE, SERIES_EPISODE }

/**
 * Classifica uma entrada M3U em Ao vivo, Filme ou Episódio de série, nesta ordem de
 * confiança: caminho da URL (`/movie/`, `/series/`), extensão do arquivo, palavras no
 * `group-title` e, por fim, o padrão de episódio no nome.
 */
object M3uClassifier {
    private val VIDEO_EXT = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "m4v", "webm", "mpg", "mpeg")
    private val MOVIE_WORDS = listOf("filme", "movie", "vod", "cinema", "lancamento", "lançamento")
    private val SERIES_WORDS = listOf("serie", "série", "series", "novela", "anime", "dorama", "temporada")

    fun classify(entry: M3uEntry): M3uKind {
        val path = entry.url.substringBefore('?').lowercase()
        if (path.contains("/movie/")) return M3uKind.MOVIE
        if (path.contains("/series/")) return M3uKind.SERIES_EPISODE

        val ext = path.substringAfterLast('/').substringAfterLast('.', "")
        val looksLikeFile = ext in VIDEO_EXT
        val group = entry.group?.let { TextNormalizer.stripAccents(it.lowercase()) }.orEmpty()
        val hasEpisode = SeriesPattern.match(entry.name) != null

        if (looksLikeFile) return if (hasEpisode) M3uKind.SERIES_EPISODE else M3uKind.MOVIE
        if (SERIES_WORDS.any { group.contains(TextNormalizer.stripAccents(it)) }) return M3uKind.SERIES_EPISODE
        if (MOVIE_WORDS.any { group.contains(TextNormalizer.stripAccents(it)) }) return M3uKind.MOVIE
        if (hasEpisode) return M3uKind.SERIES_EPISODE
        return M3uKind.LIVE
    }
}
