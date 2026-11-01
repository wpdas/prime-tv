package com.wpdas.primetv.data.remote.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbPage(val page: Int = 1, val results: List<TmdbListItem> = emptyList())

@Serializable
data class TmdbListItem(
    val id: Int,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    val overview: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
) {
    val displayTitle: String get() = title ?: name.orEmpty()
    val original: String? get() = originalTitle ?: originalName
    val year: Int? get() = (releaseDate ?: firstAirDate)?.take(4)?.toIntOrNull()
}

@Serializable
data class TmdbGenre(val id: Int, val name: String)

@Serializable
data class TmdbGenreList(val genres: List<TmdbGenre> = emptyList())

@Serializable
data class TmdbVideo(
    val key: String? = null,
    val site: String? = null,
    val type: String? = null,
    val official: Boolean? = null,
    @SerialName("iso_639_1") val language: String? = null,
)

@Serializable
data class TmdbVideos(val results: List<TmdbVideo> = emptyList())

@Serializable
data class TmdbDetails(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    val genres: List<TmdbGenre> = emptyList(),
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    val videos: TmdbVideos? = null,
) {
    val displayTitle: String get() = title ?: name.orEmpty()
    val year: Int? get() = (releaseDate ?: firstAirDate)?.take(4)?.toIntOrNull()
    val trailerKey: String? get() = videos?.results
        ?.filter { it.site.equals("YouTube", ignoreCase = true) && it.type.equals("Trailer", ignoreCase = true) && !it.key.isNullOrBlank() }
        ?.sortedWith(compareByDescending<TmdbVideo> { it.language == "pt" }.thenByDescending { it.official == true })
        ?.firstOrNull()?.key
}
