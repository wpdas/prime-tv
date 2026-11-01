package com.wpdas.primetv.data.remote.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class XtreamAuthResponse(
    @SerialName("user_info") val userInfo: XtreamUserInfo? = null,
    @SerialName("server_info") val serverInfo: XtreamServerInfo? = null,
)

@Serializable
data class XtreamUserInfo(
    @Serializable(LenientString::class) val auth: String? = null,
    @Serializable(LenientString::class) val status: String? = null,
    @Serializable(LenientString::class) @SerialName("exp_date") val expDate: String? = null,
    @Serializable(LenientString::class) @SerialName("max_connections") val maxConnections: String? = null,
    @SerialName("allowed_output_formats") val allowedOutputFormats: List<String>? = null,
) {
    val isAuthenticated: Boolean get() = auth.toBoolLenient()
}

@Serializable
data class XtreamServerInfo(
    @Serializable(LenientString::class) val url: String? = null,
    @Serializable(LenientString::class) val port: String? = null,
    @Serializable(LenientString::class) @SerialName("https_port") val httpsPort: String? = null,
    @Serializable(LenientString::class) @SerialName("server_protocol") val serverProtocol: String? = null,
    @Serializable(LenientString::class) val timezone: String? = null,
)

@Serializable
data class XtreamCategoryDto(
    @Serializable(LenientString::class) @SerialName("category_id") val categoryId: String? = null,
    @Serializable(LenientString::class) @SerialName("category_name") val categoryName: String? = null,
    @Serializable(LenientString::class) @SerialName("parent_id") val parentId: String? = null,
)

@Serializable
data class XtreamLiveStreamDto(
    @Serializable(LenientString::class) val num: String? = null,
    @Serializable(LenientString::class) val name: String? = null,
    @Serializable(LenientString::class) @SerialName("stream_id") val streamId: String? = null,
    @Serializable(LenientString::class) @SerialName("stream_icon") val streamIcon: String? = null,
    @Serializable(LenientString::class) @SerialName("epg_channel_id") val epgChannelId: String? = null,
    @Serializable(LenientString::class) @SerialName("category_id") val categoryId: String? = null,
    @Serializable(LenientString::class) @SerialName("is_adult") val isAdult: String? = null,
    @Serializable(LenientString::class) @SerialName("tv_archive") val tvArchive: String? = null,
)

@Serializable
data class XtreamVodStreamDto(
    @Serializable(LenientString::class) val num: String? = null,
    @Serializable(LenientString::class) val name: String? = null,
    @Serializable(LenientString::class) @SerialName("stream_id") val streamId: String? = null,
    @Serializable(LenientString::class) @SerialName("stream_icon") val streamIcon: String? = null,
    @Serializable(LenientString::class) val rating: String? = null,
    @Serializable(LenientString::class) val added: String? = null,
    @Serializable(LenientString::class) @SerialName("category_id") val categoryId: String? = null,
    @Serializable(LenientString::class) @SerialName("container_extension") val containerExtension: String? = null,
    @Serializable(LenientString::class) @SerialName("is_adult") val isAdult: String? = null,
    @Serializable(LenientString::class) @SerialName("tmdb_id") val tmdbId: String? = null,
    @Serializable(LenientString::class) val year: String? = null,
)

@Serializable
data class XtreamSeriesDto(
    @Serializable(LenientString::class) val num: String? = null,
    @Serializable(LenientString::class) val name: String? = null,
    @Serializable(LenientString::class) @SerialName("series_id") val seriesId: String? = null,
    @Serializable(LenientString::class) val cover: String? = null,
    @Serializable(LenientString::class) val plot: String? = null,
    @Serializable(LenientString::class) val genre: String? = null,
    @Serializable(LenientString::class) @SerialName("releaseDate") val releaseDate: String? = null,
    @Serializable(LenientString::class) val rating: String? = null,
    @Serializable(LenientString::class) @SerialName("category_id") val categoryId: String? = null,
    @Serializable(LenientString::class) @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
    @Serializable(LenientString::class) @SerialName("tmdb") val tmdb: String? = null,
    /** Vem como lista de URLs ou como string; tratado no mapper. */
    @SerialName("backdrop_path") val backdropPath: JsonElement? = null,
)

@Serializable
data class XtreamSeriesInfoResponse(
    val info: JsonElement? = null,
    val seasons: JsonElement? = null,
    /** Normalmente um objeto {"1": [episódios], "2": [...]}, às vezes uma lista. */
    val episodes: JsonElement? = null,
)

@Serializable
data class XtreamEpisodeDto(
    @Serializable(LenientString::class) val id: String? = null,
    @Serializable(LenientString::class) @SerialName("episode_num") val episodeNum: String? = null,
    @Serializable(LenientString::class) val title: String? = null,
    @Serializable(LenientString::class) @SerialName("container_extension") val containerExtension: String? = null,
    @Serializable(LenientString::class) val season: String? = null,
    val info: XtreamEpisodeInfoDto? = null,
)

@Serializable
data class XtreamEpisodeInfoDto(
    @Serializable(LenientString::class) @SerialName("duration_secs") val durationSecs: String? = null,
    @Serializable(LenientString::class) val duration: String? = null,
    @Serializable(LenientString::class) @SerialName("movie_image") val movieImage: String? = null,
    @Serializable(LenientString::class) val plot: String? = null,
    @Serializable(LenientString::class) val rating: String? = null,
)

@Serializable
data class XtreamVodInfoResponse(
    val info: JsonElement? = null,
    @SerialName("movie_data") val movieData: JsonElement? = null,
)
