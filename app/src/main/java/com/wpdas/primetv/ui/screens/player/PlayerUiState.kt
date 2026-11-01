package com.wpdas.primetv.ui.screens.player

import com.wpdas.primetv.domain.model.Channel
import com.wpdas.primetv.domain.model.EpgProgram
import com.wpdas.primetv.domain.model.Episode

data class TrackOption(val id: String, val label: String, val selected: Boolean)

enum class PlayerPanel { NONE, AUDIO, SUBTITLES, EPISODES }

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
    // séries
    val episodes: List<Episode> = emptyList(),
    val currentEpisode: Episode? = null,
    val nextEpisode: Episode? = null,
    val nextCountdown: Int? = null,
    // ao vivo
    val channel: Channel? = null,
    val nowProgram: EpgProgram? = null,
    /** Sinaliza à tela que a reprodução terminou e ela deve fechar. */
    val finished: Boolean = false,
) {
    val progress: Float get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    val buffered: Float get() = if (durationMs <= 0) 0f else (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f)
    val hasError: Boolean get() = errorKey != null
}
