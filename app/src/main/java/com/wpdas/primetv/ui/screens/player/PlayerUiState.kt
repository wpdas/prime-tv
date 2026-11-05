package com.wpdas.primetv.ui.screens.player

import com.wpdas.primetv.domain.model.Channel
import com.wpdas.primetv.domain.model.EpgProgram
import com.wpdas.primetv.domain.model.Episode

data class TrackOption(val id: String, val label: String, val selected: Boolean)

enum class PlayerPanel { NONE, AUDIO, SUBTITLES, EPISODES, QUALITY, CHANNELS }

data class PlayerUiState(
    val loaded: Boolean = false,
    val title: String = "",
    val subtitle: String? = null,
    /** Ex.: "1080p · Português". */
    val badge: String? = null,
    val isLive: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val errorKey: String? = null,
    val errorDetail: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedMs: Long = 0,
    val controlsVisible: Boolean = true,
    val panel: PlayerPanel = PlayerPanel.NONE,
    val audioTracks: List<TrackOption> = emptyList(),
    val subtitleTracks: List<TrackOption> = emptyList(),
    val subtitlesOff: Boolean = true,
    /** Menu Qualidade: Auto/faixas do stream e versões da lista. Vazio = só uma qualidade. */
    val qualityOptions: List<QualityOption> = emptyList(),
    /** Velocidade estimada do download do stream (bits/s), para mostrar junto do carregamento. */
    val bitrateBps: Long? = null,
    // séries
    val episodes: List<Episode> = emptyList(),
    val currentEpisode: Episode? = null,
    val nextEpisode: Episode? = null,
    val nextCountdown: Int? = null,
    // ao vivo
    val channel: Channel? = null,
    val nowProgram: EpgProgram? = null,
    /** Ao vivo: canais da lista de zapping atual (painel aberto com OK) e o que está no ar em cada um. */
    val zapList: List<Channel> = emptyList(),
    val zapNowPlaying: Map<String, EpgProgram> = emptyMap(),
    /** Grupos de canais (esquerda no painel): Favoritos, Futebol, Todos e categorias; o id do grupo em uso. */
    val zapRail: List<com.wpdas.primetv.ui.components.RailCategory> = emptyList(),
    val zapRailId: String = ZAP_ALL,
    /** Tamanho real do vídeo em reprodução (0 = ainda não sabe). */
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    /** Configuração: saída de vídeo na resolução do conteúdo (a TV faz o upscaling). */
    val matchDisplayMode: Boolean = false,
    /** Sinaliza à tela que a reprodução terminou e ela deve fechar. */
    val finished: Boolean = false,
    /** Ao vivo: dígitos sendo digitados para pular de canal ("12_"). */
    val channelInput: String? = null,
    /** Ao vivo: linha de opções (Áudio/Legendas) aberta com esquerda/direita. */
    val liveMenu: Boolean = false,
    /** Ao vivo: qualidade da variante em uso ("4K", "FHD"), se o nome informar. */
    val qualityLabel: String? = null,
    /** Aviso curto (ex.: "Trocando para HD"). */
    val toastKey: String? = null,
    val toastArg: String? = null,
) {
    companion object {
        const val ZAP_ALL = "all"
        const val ZAP_FOOTBALL = "football"
        const val ZAP_FAVORITES = "favorites"
    }

    val progress: Float get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    val buffered: Float get() = if (durationMs <= 0) 0f else (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f)
    val hasError: Boolean get() = errorKey != null
}
