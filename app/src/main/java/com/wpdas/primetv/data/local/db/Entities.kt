package com.wpdas.primetv.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories", indices = [Index("section")])
data class CategoryEntity(
    @PrimaryKey val id: String,
    val section: String,
    val name: String,
    val sortOrder: Int,
)

@Entity(tableName = "channels", indices = [Index("categoryId"), Index("number"), Index("epgKey"), Index("isFootball"), Index("groupKey")])
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val number: Int,
    val logoUrl: String?,
    val categoryId: String?,
    val categoryName: String?,
    val streamUrl: String,
    val epgChannelId: String?,
    val epgKey: String?,
    val isAdult: Boolean,
    val isFootball: Boolean,
    val searchKey: String,
    /** Nome sem tag de qualidade; variantes (UHD/FHD/HD/SD) do mesmo canal compartilham a chave. */
    val groupKey: String,
    /** 0 = 4K … 3 = SD, 4 = sem tag. */
    val qualityRank: Int,
)

@Entity(tableName = "movies", indices = [Index("categoryId"), Index("normalizedTitle"), Index("addedAt"), Index("isFootball"), Index("groupKey")])
data class MovieEntity(
    @PrimaryKey val id: String,
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
    val isAdult: Boolean,
    val isFootball: Boolean,
    val normalizedTitle: String,
    val qualityRank: Int,
    /** Chave estrita de variantes de qualidade (ver [com.wpdas.primetv.core.util.Quality.groupKey]). */
    @ColumnInfo(defaultValue = "") val groupKey: String = "",
)

@Entity(tableName = "series", indices = [Index("categoryId"), Index("normalizedTitle"), Index("isFootball")])
data class SeriesEntity(
    @PrimaryKey val id: String,
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
    val episodesLoaded: Boolean,
    val isFootball: Boolean,
    val normalizedTitle: String,
)

@Entity(tableName = "episodes", indices = [Index("seriesId")])
data class EpisodeEntity(
    @PrimaryKey val id: String,
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

/** Índice de busca (FTS4, tokenizador unicode61 ignora acentos). [type]: channel | movie | series. */
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "search_index")
data class SearchIndexEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Int? = null,
    val title: String,
    val type: String,
    val refId: String,
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val section: String,
    /** done | skipped | error */
    val state: String,
    val count: Int,
    val finishedAt: Long,
    val detail: String?,
)

/** Programa do guia (XMLTV). [channelKey] é o id do canal no XMLTV. */
@Entity(tableName = "epg_programs", indices = [Index("channelKey", "start"), Index("end")])
data class EpgProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelKey: String,
    val start: Long,
    val end: Long,
    val title: String,
    val description: String?,
)

/** Canais declarados no XMLTV, para casar por nome quando a lista não traz tvg-id. */
@Entity(tableName = "epg_channels", indices = [Index("displayKey")])
data class EpgChannelEntity(
    @PrimaryKey val xmltvId: String,
    val displayKey: String,
    val iconUrl: String?,
)

/** Favoritos: filmes, séries e canais. [type]: movie | series | channel. */
@Entity(tableName = "favorites", indices = [Index("type")])
data class FavoriteEntity(
    @PrimaryKey val itemId: String,
    val type: String,
    val addedAt: Long,
)

/**
 * Histórico de reprodução. [itemId] é o filme, o episódio ou o canal; [seriesId] só
 * para episódios. Canais guardam posição 0 e servem como "últimos canais".
 */
@Entity(tableName = "watch_history", indices = [Index("seriesId"), Index("updatedAt"), Index("type")])
data class WatchHistoryEntity(
    @PrimaryKey val itemId: String,
    val type: String,
    val seriesId: String?,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val finished: Boolean,
)

/** Resposta de lista do TMDB (trending, populares) em JSON, com validade de ~12 h. */
@Entity(tableName = "tmdb_cache")
data class TmdbCacheEntity(
    @PrimaryKey val key: String,
    val json: String,
    val fetchedAt: Long,
)

/** Detalhes de um título do TMDB (~7 dias). [type]: movie | series. */
@Entity(tableName = "tmdb_details", primaryKeys = ["tmdbId", "type"])
data class TmdbDetailsEntity(
    val tmdbId: Int,
    val type: String,
    val title: String,
    val overview: String?,
    val backdropPath: String?,
    val posterPath: String?,
    val genres: String?,
    val rating: Double?,
    val voteCount: Int?,
    val year: Int?,
    val seasons: Int?,
    val episodes: Int?,
    val trailerKey: String?,
    val fetchedAt: Long,
)

/** Correspondência item da lista -> TMDB (null = procurado e não encontrado), para não repetir buscas. */
@Entity(tableName = "tmdb_match")
data class TmdbMatchEntity(
    @PrimaryKey val itemId: String,
    val tmdbId: Int?,
    val type: String,
    val matchedAt: Long,
)

/** Escudo de time (URL de terceiros). [url] null = procurado e não encontrado. */
@Entity(tableName = "team_crests")
data class TeamCrestEntity(
    @PrimaryKey val teamKey: String,
    val url: String?,
    val fetchedAt: Long,
)
