package com.wpdas.primetv.domain.model

/** Seções do catálogo. Cada uma é sincronizada e configurada separadamente. */
enum class ContentSection(val labelKey: String) {
    LIVE("section.live"),
    MOVIES("section.movies"),
    SERIES("section.series"),
    EPG("section.epg"),
}

data class Category(
    val id: String,
    val section: ContentSection,
    val name: String,
    val sortOrder: Int,
)

data class Channel(
    val id: String,
    val name: String,
    val number: Int,
    val logoUrl: String?,
    val categoryId: String?,
    val categoryName: String?,
    val streamUrl: String,
    val epgChannelId: String?,
    /** Chave usada no guia: o epgChannelId ou, na falta dele, o id do XMLTV casado pelo nome. */
    val epgKey: String?,
    val isAdult: Boolean = false,
    val isFootball: Boolean = false,
    /** Chave do grupo de variantes (mesmo canal em UHD/FHD/HD/SD). */
    val groupKey: String = "",
    val qualityRank: Int = 4,
) {
    /** Nome sem a tag de qualidade ("Globo UHD 4K" -> "Globo"). */
    val displayName: String get() = com.wpdas.primetv.core.util.TextNormalizer.displayTitle(name)
    val qualityLabel: String? get() = com.wpdas.primetv.core.util.Quality.label(qualityRank)
}

data class Movie(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val categoryId: String?,
    val categoryName: String?,
    val streamUrl: String,
    val containerExt: String?,
    val year: Int?,
    val rating: Double?,
    val tmdbId: Int?,
    val addedAt: Long?,
    val isAdult: Boolean = false,
    val isFootball: Boolean = false,
    val qualityRank: Int = 4,
) {
    val qualityLabel: String? get() = com.wpdas.primetv.core.util.Quality.label(qualityRank)
}

data class Series(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val categoryId: String?,
    val categoryName: String?,
    val plot: String?,
    val genre: String?,
    val year: Int?,
    val rating: Double?,
    val tmdbId: Int?,
    /** false quando os episódios ainda precisam ser buscados (Xtream carrega sob demanda). */
    val episodesLoaded: Boolean,
    val isFootball: Boolean = false,
)

data class Episode(
    val id: String,
    val seriesId: String,
    val season: Int,
    val number: Int,
    val title: String,
    val streamUrl: String,
    val containerExt: String?,
    val durationSec: Int?,
    val posterUrl: String?,
    val plot: String?,
)
