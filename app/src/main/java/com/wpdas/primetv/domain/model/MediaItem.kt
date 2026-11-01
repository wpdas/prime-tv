package com.wpdas.primetv.domain.model

enum class MediaType { MOVIE, SERIES, CHANNEL }

/** Item de catálogo exibido em cards, destaque e detalhes. */
data class MediaItem(
    val id: String,
    val title: String,
    val type: MediaType,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    /** Cor usada no lugar da arte enquanto não há imagem (ou nos dados fake). */
    val placeholderColor: Long = 0xFF2B2440,
    val year: Int? = null,
    val genres: List<String> = emptyList(),
    val rating: Double? = null,
    val seasons: Int? = null,
    val episodes: Int? = null,
    val synopsis: String? = null,
    val inMyList: Boolean = false,
    /** Rótulo opcional de onde veio a recomendação (ex.: "Em alta esta semana"). */
    val badgeKey: String? = null,
)

/** Linha "Continuar assistindo". [progress] de 0 a 1. */
data class ContinueWatchingItem(
    val item: MediaItem,
    val episodeLabel: String?,      // "T1:E4" ou null para filme
    val remainingMinutes: Int,
    val progress: Float,
)

/** Evento de futebol ao vivo (canal + partida). */
data class FootballEvent(
    val id: String,
    val channelName: String,
    val title: String,
    val isLive: Boolean = true,
)

/** Linha de categoria na tela de Início. [section]/[categoryId] habilitam o card "Ver todos". */
data class CategoryRow(
    val id: String,
    val title: String,
    val items: List<MediaItem>,
    val section: ContentSection? = null,
    val categoryId: String? = null,
    val total: Int? = null,
)
