package com.wpdas.primetv.data.repository

import com.wpdas.primetv.core.util.TextNormalizer
import com.wpdas.primetv.data.local.db.CategoryEntity
import com.wpdas.primetv.data.local.db.ChannelEntity
import com.wpdas.primetv.data.local.db.EpgProgramEntity
import com.wpdas.primetv.data.local.db.EpisodeEntity
import com.wpdas.primetv.data.local.db.MovieEntity
import com.wpdas.primetv.data.local.db.SeriesEntity
import com.wpdas.primetv.domain.model.Category
import com.wpdas.primetv.domain.model.Channel
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.EpgProgram
import com.wpdas.primetv.domain.model.Episode
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.model.MediaType
import com.wpdas.primetv.domain.model.Movie
import com.wpdas.primetv.domain.model.Series

internal fun CategoryEntity.toDomain() = Category(
    id = id,
    section = ContentSection.entries.firstOrNull { it.name == section } ?: ContentSection.LIVE,
    name = name,
    sortOrder = sortOrder,
)

internal fun ChannelEntity.toDomain() = Channel(
    id = id, name = name, number = number, logoUrl = logoUrl, categoryId = categoryId, categoryName = categoryName,
    streamUrl = streamUrl, epgChannelId = epgChannelId, epgKey = epgKey, isAdult = isAdult, isFootball = isFootball,
    groupKey = groupKey, qualityRank = qualityRank,
)

internal fun MovieEntity.toDomain() = Movie(
    id = id, title = title, posterUrl = posterUrl, categoryId = categoryId, categoryName = categoryName,
    streamUrl = streamUrl, containerExt = containerExt, year = year, rating = rating, tmdbId = tmdbId,
    addedAt = addedAt, isAdult = isAdult, isFootball = isFootball, qualityRank = qualityRank,
)

internal fun SeriesEntity.toDomain() = Series(
    id = id, title = title, posterUrl = posterUrl, backdropUrl = backdropUrl, categoryId = categoryId,
    categoryName = categoryName, plot = plot, genre = genre, year = year, rating = rating, tmdbId = tmdbId,
    episodesLoaded = episodesLoaded, isFootball = isFootball,
)

internal fun EpisodeEntity.toDomain() = Episode(
    id = id, seriesId = seriesId, season = season, number = number, title = title, streamUrl = streamUrl,
    containerExt = containerExt, durationSec = durationSec, posterUrl = posterUrl, plot = plot,
)

internal fun EpgProgramEntity.toDomain() = EpgProgram(channelKey, start, end, title, description)

/** Cores de placeholder derivadas do id, para cards sem imagem terem variedade. */
private val PLACEHOLDERS = longArrayOf(0xFF2B2440, 0xFF1F2E3A, 0xFF3A2A24, 0xFF24332A, 0xFF33243A, 0xFF1C2A3A, 0xFF2E2350, 0xFF14261C)
internal fun placeholderFor(id: String): Long = PLACEHOLDERS[(id.hashCode() and 0x7fffffff) % PLACEHOLDERS.size]

fun Movie.toMediaItem(inMyList: Boolean = false) = MediaItem(
    id = id, title = TextNormalizer.displayTitle(title), type = MediaType.MOVIE, posterUrl = posterUrl, backdropUrl = null,
    placeholderColor = placeholderFor(id), year = year, genres = listOfNotNull(categoryName), rating = rating,
    synopsis = null, inMyList = inMyList, tmdbId = tmdbId,
)

fun Series.toMediaItem(inMyList: Boolean = false) = MediaItem(
    id = id, title = TextNormalizer.displayTitle(title), type = MediaType.SERIES, posterUrl = posterUrl, backdropUrl = backdropUrl,
    placeholderColor = placeholderFor(id), year = year,
    genres = genre?.split(',', '/', '|')?.map { it.trim() }?.filter { it.isNotEmpty() }?.take(3) ?: listOfNotNull(categoryName),
    rating = rating, synopsis = plot, inMyList = inMyList, tmdbId = tmdbId,
)

fun Channel.toMediaItem(inMyList: Boolean = false) = MediaItem(
    id = id, title = displayName, type = MediaType.CHANNEL, posterUrl = logoUrl, backdropUrl = null,
    placeholderColor = placeholderFor(id), genres = listOfNotNull(categoryName), inMyList = inMyList,
)
