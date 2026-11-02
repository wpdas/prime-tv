package com.wpdas.primetv.ui.screens.football

import com.wpdas.primetv.domain.football.MatchEvent
import com.wpdas.primetv.domain.model.CategoryRow
import com.wpdas.primetv.domain.model.Channel
import com.wpdas.primetv.domain.model.EpgProgram
import com.wpdas.primetv.domain.model.HomeTab
import com.wpdas.primetv.ui.components.RailCategory

data class FootballUiState(
    val rail: List<RailCategory> = emptyList(),
    val selectedRailId: String = RAIL_TODAY,
    /** Guia completo (null = carregando). */
    val events: List<MatchEvent>? = null,
    val now: Long = System.currentTimeMillis(),
    /** Canais de futebol (trilho Canais). */
    val channels: List<Channel>? = null,
    val nowPlaying: Map<String, EpgProgram> = emptyMap(),
    /** Linhas de filmes e séries de futebol (trilho Vídeos). */
    val videos: List<CategoryRow> = emptyList(),
    /** A lista está sendo atualizada em segundo plano. */
    val refreshing: Boolean = false,
    /** Jogo a focar ao voltar do player; a tela consome e zera. */
    val focusEventKey: String? = null,
    /** Abas da barra superior (Kids/Anime só com conteúdo). */
    val tabs: List<HomeTab> = HomeTab.entries.filter { it !in HomeTab.OPTIONAL },
) {
    companion object {
        const val RAIL_TODAY = "today"
        const val RAIL_LIVE = "live"
        const val RAIL_CHANNELS = "channels"
        const val RAIL_VIDEOS = "videos"
    }
}
