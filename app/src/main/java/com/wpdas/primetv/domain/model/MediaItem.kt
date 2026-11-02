package com.wpdas.primetv.domain.model

enum class MediaType(val key: String) { MOVIE("movie"), SERIES("series"), CHANNEL("channel") }

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
    val tmdbId: Int? = null,
)

/** Linha "Continuar assistindo". [progress] de 0 a 1. */
data class ContinueWatchingItem(
    val item: MediaItem,
    val episodeLabel: String?,      // "T1:E4" ou null para filme
    val remainingMinutes: Int,
    val progress: Float,
    val episodeId: String? = null,
)

/** Jogo de futebol para a linha da Início: canal, partida e, quando a lista informa, horário e competição. */
data class FootballEvent(
    /** Único na linha (chave do jogo ou id do canal). */
    val id: String,
    /** Canal a abrir no player. */
    val channelId: String = id,
    val channelName: String,
    val title: String,
    val isLive: Boolean = true,
    /** Início em epoch ms (null quando só sabemos que está no ar agora). */
    val start: Long? = null,
    /** Competição já pronta para exibir ("Brasileirão Série B"), ou null. */
    val league: String? = null,
    val home: String? = null,
    val away: String? = null,
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
