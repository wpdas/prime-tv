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

@Entity(tableName = "channels", indices = [Index("categoryId"), Index("number"), Index("epgKey"), Index("isFootball")])
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
)

@Entity(tableName = "movies", indices = [Index("categoryId"), Index("normalizedTitle"), Index("addedAt"), Index("isFootball")])
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
