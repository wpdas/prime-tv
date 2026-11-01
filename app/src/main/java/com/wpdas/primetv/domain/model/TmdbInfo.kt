package com.wpdas.primetv.domain.model

/** Dados do TMDB para um título da lista (cache local de ~7 dias). */
data class TmdbInfo(
    val tmdbId: Int,
    val type: MediaType,
    val title: String,
    val overview: String?,
    val backdropUrl: String?,
    val posterUrl: String?,
    val genres: List<String>,
    val rating: Double?,
    val voteCount: Int?,
    val year: Int?,
    val seasons: Int?,
    val episodes: Int?,
    val trailerYoutubeKey: String?,
)
