package com.wpdas.primetv.ui.screens.player

import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import com.wpdas.primetv.core.util.TextNormalizer
import com.wpdas.primetv.data.player.PlayerFactory
import com.wpdas.primetv.domain.model.Channel
import com.wpdas.primetv.domain.model.Episode
import com.wpdas.primetv.domain.model.MediaType
import com.wpdas.primetv.domain.repository.CatalogRepository
import com.wpdas.primetv.domain.repository.ChannelFilter
import com.wpdas.primetv.domain.repository.LibraryRepository
import com.wpdas.primetv.ui.navigation.PlayerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * Dono do ExoPlayer enquanto a tela do player existir. Resolve o que tocar a partir da
 * rota (filme, série/episódio ou canal), salva o progresso a cada 10 s e ao sair, cuida
 * de faixas, próximo episódio e troca de canal.
 */
@OptIn(UnstableApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
    playerFactory: PlayerFactory,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PlayerRoute>()
    val player: ExoPlayer = playerFactory.create()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private sealed interface Content {
        /** [movie] é o título aberto (histórico usa o id dele); [playing] é a variante em reprodução. */
        data class Movie(
            val movie: com.wpdas.primetv.domain.model.Movie,
            val playing: com.wpdas.primetv.domain.model.Movie = movie,
            val variants: List<com.wpdas.primetv.domain.model.Movie> = emptyList(),
        ) : Content
        data class SeriesEpisode(val seriesId: String, val episodes: List<Episode>, val episode: Episode) : Content
        data class Live(val channel: Channel, val zapList: List<Channel>, val variants: List<Channel> = emptyList()) : Content
    }

    private var content: Content? = null
    private var hideJob: Job? = null
    private var lastSaveMs = 0L
    private var wasPlayingBeforeBackground = false
    private var retriedLive = false
    private val stalls = com.wpdas.primetv.core.util.StallDetector(STALL_LIMIT, STALL_WINDOW_MS)
    private var wasReady = false
    private var seekPending = false
    private var slowToastShown = false
    private val trackGroups = HashMap<String, Pair<TrackGroup, Int>>()
    /** Faixas de vídeo do stream atual (menu Qualidade). */
    private var videoTracks: List<VideoTrack> = emptyList()
    /** Faixa de vídeo escolhida à mão, ou null (Auto). */
    private var manualTrackId: String? = null
    /** O usuário escolheu a qualidade: a troca automática por conexão lenta fica desligada. */
    private var qualityManual = false
    private val speedMeter = playerFactory.speedMeter

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _state.update { it.copy(isBuffering = true) }
                    // Rebuffer no meio da reprodução (não é carga inicial nem seek): conta para baixar a qualidade.
                    if (wasReady && !seekPending && stalls.onStall(System.currentTimeMillis())) viewModelScope.launch { onSlowConnection() }
                }
                Player.STATE_READY -> {
                    wasReady = true
                    seekPending = false
                    _state.update { it.copy(isBuffering = false, errorKey = null, errorDetail = null, durationMs = player.duration.coerceAtLeast(0)) }
                }
                Player.STATE_ENDED -> onEnded()
                Player.STATE_IDLE -> Unit
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) scheduleHide() else cancelHide()
        }

        override fun onPlayerError(error: PlaybackException) {
            val live = content as? Content.Live
            if (live != null && !retriedLive) {
                // Servidores com limite de conexões recusam por alguns segundos após a prévia; tenta mais uma vez.
                retriedLive = true
                viewModelScope.launch {
                    delay(LIVE_RETRY_DELAY_MS)
                    player.prepare()
                    player.play()
                }
                return
            }
            // Variante de qualidade mais baixa do mesmo canal, se houver (UHD -> FHD -> HD -> SD).
            if (live != null) {
                val next = live.variants.firstOrNull { it.qualityRank > live.channel.qualityRank }
                if (next != null) {
                    viewModelScope.launch {
                        showToast("player.switching_quality", next.qualityLabel ?: "")
                        tuneChannel(next, keepVariants = true)
                    }
                    return
                }
            }
            _state.update {
                it.copy(
                    isBuffering = false,
                    errorKey = if (live != null) "player.error_live" else "player.error",
                    errorDetail = error.errorCodeName.removePrefix("ERROR_CODE_").lowercase().replace('_', ' '),
                    controlsVisible = true,
                )
            }
        }

        override fun onTracksChanged(tracks: Tracks) = updateTracks(tracks)
    }

    init {
        player.addListener(listener)
        viewModelScope.launch { load() }
        viewModelScope.launch { tick() }
    }

    // ------------------------------------------------------------------ carga

    private suspend fun load() {
        val id = route.itemId
        try {
            when {
                id.startsWith("xs-") || id.startsWith("ms-") -> loadSeries(id)
                id.startsWith("xl-") || id.startsWith("ml-") -> loadChannel(id)
                else -> loadMovie(id)
            }
        } catch (e: Exception) {
            _state.update { it.copy(loaded = true, isBuffering = false, errorKey = "player.error", errorDetail = e.javaClass.simpleName) }
        }
    }

    private suspend fun loadMovie(id: String) {
        val movie = catalog.movieOnce(id) ?: return notFound()
        val variants = runCatching { catalog.movieVariants(movie.id) }.getOrDefault(emptyList())
        content = Content.Movie(movie, variants = variants)
        qualityManual = false
        val start = library.progress(movie.id)?.takeUnless { it.finished }?.positionMs ?: 0L
        _state.update { it.copy(loaded = true, title = TextNormalizer.displayTitle(movie.title), subtitle = movie.year?.toString(), qualityLabel = movie.qualityLabel) }
        start(movie.streamUrl, start)
    }

    private suspend fun loadSeries(seriesId: String) {
        catalog.ensureEpisodes(seriesId)
        val series = catalog.seriesOnce(seriesId) ?: return notFound()
        val episodes = catalog.episodesOnce(seriesId)
        if (episodes.isEmpty()) return notFound()
        val episode = route.episodeId?.let { epId -> episodes.firstOrNull { it.id == epId } }
            ?: library.resumePoint(seriesId, episodes)?.episode
            ?: episodes.first()
        playEpisodeInternal(series.title, episodes, episode)
    }

    private suspend fun playEpisodeInternal(seriesTitle: String, episodes: List<Episode>, episode: Episode) {
        val seriesId = episode.seriesId
        content = Content.SeriesEpisode(seriesId, episodes, episode)
        qualityManual = false
        val start = library.progress(episode.id)?.takeUnless { it.finished }?.positionMs ?: 0L
        val index = episodes.indexOf(episode)
        _state.update {
            it.copy(
                loaded = true,
                title = TextNormalizer.displayTitle(seriesTitle),
                subtitle = "T${episode.season}:E${episode.number} · ${episode.title}",
                episodes = episodes,
                currentEpisode = episode,
                nextEpisode = episodes.getOrNull(index + 1),
                nextCountdown = null,
                finished = false,
            )
        }
        start(episode.streamUrl, start)
    }

    private suspend fun loadChannel(id: String) {
        val channel = catalog.channelOnce(id) ?: return notFound()
        val zap = catalog.channelsOnce(channel.categoryId?.let { ChannelFilter.Category(it) } ?: ChannelFilter.All)
        content = Content.Live(channel, zap)
        tuneChannel(channel)
    }

    private suspend fun tuneChannel(channel: Channel, keepVariants: Boolean = false) {
        val live = content as? Content.Live ?: return
        val variants = if (keepVariants) live.variants else runCatching { catalog.channelVariants(channel.groupKey) }.getOrDefault(emptyList())
        // Canal fora da lista de zapping atual (ex.: número digitado): a lista passa a ser "Todos",
        // para cima/baixo continuarem a partir dele e não voltarem ao primeiro.
        val zap = if (live.zapList.any { it.id == channel.id }) live.zapList
        else runCatching { catalog.channelsOnce(ChannelFilter.All) }.getOrDefault(live.zapList)
        content = live.copy(channel = channel, variants = variants, zapList = zap)
        if (!keepVariants) qualityManual = false
        retriedLive = false
        library.recordChannel(channel.id)
        val now = runCatching { catalog.nowPlaying(ChannelFilter.Category(channel.categoryId ?: ""), System.currentTimeMillis()) }.getOrDefault(emptyMap())
        _state.update {
            it.copy(
                loaded = true,
                isLive = true,
                title = channel.name,
                subtitle = channel.categoryName,
                channel = channel,
                nowProgram = channel.epgKey?.let(now::get),
                durationMs = 0,
                positionMs = 0,
                qualityLabel = channel.qualityLabel,
                liveMenu = false,
            )
        }
        start(channel.streamUrl, 0L)
    }

    /**
     * Travou várias vezes em pouco tempo. Em ordem: deixa um stream adaptativo escolher a taxa
     * sozinho; troca para a variante de qualidade abaixo do mesmo filme/canal (mantendo a
     * posição); por fim só avisa e sugere aumentar o buffer.
     */
    private suspend fun onSlowConnection() {
        if (qualityManual) {
            if (!slowToastShown) {
                slowToastShown = true
                showToast("player.slow_connection")
            }
            return
        }
        val params = player.trackSelectionParameters
        val adaptive = player.currentTracks.groups.any { it.type == C.TRACK_TYPE_VIDEO && it.length > 1 }
        if (adaptive && params.forceHighestSupportedBitrate) {
            player.trackSelectionParameters = params.buildUpon().setForceHighestSupportedBitrate(false).build()
            showToast("player.slow_adaptive")
            return
        }
        when (val c = content) {
            is Content.Movie -> {
                val next = c.variants.ifEmpty { catalog.movieVariants(c.movie.id) }.firstOrNull { it.qualityRank > c.playing.qualityRank }
                if (next != null) {
                    showToast("player.slow_switching", next.qualityLabel ?: "")
                    playMovieVariant(next)
                    return
                }
            }
            is Content.Live -> {
                val next = c.variants.firstOrNull { it.qualityRank > c.channel.qualityRank }
                if (next != null) {
                    showToast("player.slow_switching", next.qualityLabel ?: "")
                    tuneChannel(next, keepVariants = true)
                    return
                }
            }
            else -> Unit
        }
        if (!slowToastShown) {
            slowToastShown = true
            showToast("player.slow_connection")
        }
    }

    /** Mesmo filme em outra qualidade, mantendo a posição. */
    private fun playMovieVariant(next: com.wpdas.primetv.domain.model.Movie) {
        val c = content as? Content.Movie ?: return
        val position = player.currentPosition
        content = c.copy(playing = next)
        _state.update { it.copy(qualityLabel = next.qualityLabel) }
        start(next.streamUrl, position)
    }

    // ------------------------------------------------------------------ qualidade

    /** Opção do menu Qualidade escolhida pelo usuário. */
    fun selectQuality(option: QualityOption) {
        when (option.kind) {
            QualityKind.AUTO -> {
                manualTrackId = null
                qualityManual = false
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                    .setForceHighestSupportedBitrate(false)
                    .build()
                refreshQualityOptions()
            }
            QualityKind.TRACK -> {
                val (group, index) = trackGroups[option.id] ?: return
                manualTrackId = option.id
                qualityManual = true
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setOverrideForType(TrackSelectionOverride(group, index))
                    .build()
                refreshQualityOptions()
            }
            QualityKind.SOURCE -> {
                qualityManual = true
                viewModelScope.launch {
                    when (val c = content) {
                        is Content.Movie -> c.variants.firstOrNull { it.id == option.id }?.takeIf { it.id != c.playing.id }?.let { playMovieVariant(it) }
                        is Content.Live -> c.variants.firstOrNull { it.id == option.id }?.takeIf { it.id != c.channel.id }?.let { tuneChannel(it, keepVariants = true) }
                        else -> Unit
                    }
                }
            }
        }
        showToast("player.quality_set", option.label)
        closePanel()
    }

    private fun refreshQualityOptions() {
        val (sources, playing) = when (val c = content) {
            is Content.Movie -> c.variants.map { SourceVariant(it.id, it.qualityLabel ?: "—") } to c.playing.id
            is Content.Live -> c.variants.map { SourceVariant(it.id, it.qualityLabel ?: "—") } to c.channel.id
            else -> emptyList<SourceVariant>() to null
        }
        val options = QualityOptions.build(videoTracks, manualTrackId, sources, playing)
        _state.update { it.copy(qualityOptions = options) }
    }

    private fun showToast(key: String, arg: String? = null) {
        _state.update { it.copy(toastKey = key, toastArg = arg) }
        viewModelScope.launch {
            delay(TOAST_MS)
            _state.update { if (it.toastKey == key) it.copy(toastKey = null, toastArg = null) else it }
        }
    }

    // ------------------------------------------------------------------ ao vivo: números e menu

    private var digitJob: Job? = null

    /** Dígito do controle: acumula até 4 números e, após uma pausa, sintoniza o canal com esse número. */
    fun onDigit(digit: Int) {
        if (!_state.value.isLive) return
        val current = (_state.value.channelInput ?: "").takeIf { it.length < 4 } ?: ""
        val input = current + digit
        _state.update { it.copy(channelInput = input) }
        digitJob?.cancel()
        digitJob = viewModelScope.launch {
            delay(DIGIT_COMMIT_MS)
            commitChannelInput()
        }
    }

    fun commitChannelInput() {
        digitJob?.cancel()
        val number = _state.value.channelInput?.toIntOrNull()
        _state.update { it.copy(channelInput = null) }
        if (number == null) return
        viewModelScope.launch {
            val live = content as? Content.Live ?: return@launch
            val target = live.zapList.firstOrNull { it.number == number }
                ?: catalog.channelsOnce(ChannelFilter.All).firstOrNull { it.number == number }
            if (target == null) showToast("player.channel_not_found", number.toString()) else tuneChannel(target)
        }
    }

    fun openLiveMenu() {
        cancelHide()
        _state.update { it.copy(liveMenu = true, controlsVisible = true) }
    }

    fun closeLiveMenu() {
        _state.update { it.copy(liveMenu = false) }
        showControls()
    }

    private fun start(url: String, positionMs: Long) {
        trackGroups.clear()
        videoTracks = emptyList()
        manualTrackId = null
        wasReady = false
        seekPending = false
        stalls.reset()
        player.setMediaItem(MediaItem.fromUri(url), positionMs.coerceAtLeast(0))
        player.prepare()
        player.playWhenReady = true
        _state.update { it.copy(isBuffering = true, errorKey = null, errorDetail = null, controlsVisible = true, bitrateBps = null, qualityOptions = emptyList()) }
        refreshQualityOptions()
        scheduleHide()
    }

    private fun notFound() {
        _state.update { it.copy(loaded = true, isBuffering = false, errorKey = "player.error", errorDetail = "not found") }
    }

    // ------------------------------------------------------------------ relógio e histórico

    private suspend fun tick() {
        while (true) {
            delay(TICK_MS)
            if (!player.isCommandAvailable(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)) continue
            val position = player.currentPosition.coerceAtLeast(0)
            val duration = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0) ?: 0
            val buffered = player.bufferedPosition.coerceAtLeast(0)
            val speed = if (_state.value.isBuffering) speedMeter.bitrate() else null
            _state.update { s ->
                val remaining = if (duration > 0) duration - position else Long.MAX_VALUE
                val countdown = if (s.nextEpisode != null && duration > 0 && remaining <= NEXT_CARD_WINDOW_MS) (remaining / 1000).toInt().coerceAtLeast(0) else null
                s.copy(positionMs = position, durationMs = duration, bufferedMs = buffered, nextCountdown = countdown, bitrateBps = speed)
            }
            val now = System.currentTimeMillis()
            if (player.isPlaying && now - lastSaveMs >= SAVE_INTERVAL_MS) {
                lastSaveMs = now
                saveProgress()
            }
        }
    }

    private fun saveProgress() {
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: return
        val position = player.currentPosition
        val c = content
        viewModelScope.launch {
            when (c) {
                is Content.Movie -> library.savePosition(c.movie.id, MediaType.MOVIE, null, position, duration)
                is Content.SeriesEpisode -> library.savePosition(c.episode.id, MediaType.SERIES, c.seriesId, position, duration)
                else -> Unit
            }
        }
    }

    private fun onEnded() {
        val c = content
        viewModelScope.launch {
            when (c) {
                is Content.Movie -> {
                    library.markFinished(c.movie.id, MediaType.MOVIE, null, player.duration.coerceAtLeast(0))
                    _state.update { it.copy(finished = true) }
                }
                is Content.SeriesEpisode -> {
                    library.markFinished(c.episode.id, MediaType.SERIES, c.seriesId, player.duration.coerceAtLeast(0))
                    val next = _state.value.nextEpisode
                    if (next != null) playEpisode(next) else _state.update { it.copy(finished = true) }
                }
                else -> _state.update { it.copy(isBuffering = false) }
            }
        }
    }

    // ------------------------------------------------------------------ controles

    fun showControls() {
        _state.update { it.copy(controlsVisible = true) }
        scheduleHide()
    }

    fun hideControls() {
        cancelHide()
        _state.update { it.copy(controlsVisible = false) }
    }

    private fun scheduleHide() {
        cancelHide()
        hideJob = viewModelScope.launch {
            delay(CONTROLS_TIMEOUT_MS)
            val s = _state.value
            if (s.isPlaying && s.panel == PlayerPanel.NONE && !s.hasError && !s.liveMenu) _state.update { it.copy(controlsVisible = false) }
        }
    }

    private fun cancelHide() {
        hideJob?.cancel()
        hideJob = null
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
        showControls()
    }

    fun seekBy(deltaMs: Long) {
        if (_state.value.isLive) return
        val target = (player.currentPosition + deltaMs).coerceIn(0, player.duration.takeIf { it != C.TIME_UNSET } ?: Long.MAX_VALUE)
        seekPending = true
        player.seekTo(target)
        _state.update { it.copy(positionMs = target) }
        showControls()
    }

    /**
     * Passo do salto quando a tecla fica pressionada: 10 s, depois 30 s, 1 min e 2 min,
     * para atravessar um filme de duas horas em poucos segundos.
     */
    fun seekStep(repeatCount: Int): Long = when {
        repeatCount < 3 -> 10_000L
        repeatCount < 8 -> 30_000L
        repeatCount < 16 -> 60_000L
        else -> 120_000L
    }

    fun openPanel(panel: PlayerPanel) {
        cancelHide()
        _state.update { it.copy(panel = panel, controlsVisible = true) }
    }

    fun closePanel() {
        _state.update { it.copy(panel = PlayerPanel.NONE) }
        showControls()
    }

    fun retry() {
        _state.update { it.copy(errorKey = null, errorDetail = null, isBuffering = true) }
        player.prepare()
        player.play()
    }

    fun playEpisode(episode: Episode) {
        val c = content as? Content.SeriesEpisode ?: return
        saveProgress()
        viewModelScope.launch {
            closePanel()
            playEpisodeInternal(_state.value.title, c.episodes, episode)
        }
    }

    fun playNextEpisode() {
        _state.value.nextEpisode?.let { playEpisode(it) }
    }

    /** Cima/baixo no controle: canal anterior/próximo dentro da lista atual. */
    fun stepChannel(delta: Int) {
        val live = content as? Content.Live ?: return
        if (live.zapList.size < 2) return
        val index = live.zapList.indexOfFirst { it.id == live.channel.id }
        val next = live.zapList[((index + delta) % live.zapList.size + live.zapList.size) % live.zapList.size]
        viewModelScope.launch { tuneChannel(next) }
    }

    fun selectAudio(option: TrackOption) = selectTrack(option, C.TRACK_TYPE_AUDIO)

    fun selectSubtitle(option: TrackOption?) {
        if (option == null) {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            _state.update { it.copy(subtitlesOff = true, subtitleTracks = it.subtitleTracks.map { t -> t.copy(selected = false) }) }
        } else {
            selectTrack(option, C.TRACK_TYPE_TEXT)
        }
        closePanel()
    }

    private fun selectTrack(option: TrackOption, type: Int) {
        val (group, index) = trackGroups[option.id] ?: return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(type, false)
            .setOverrideForType(TrackSelectionOverride(group, index))
            .build()
        closePanel()
    }

    private fun updateTracks(tracks: Tracks) {
        trackGroups.clear()
        val audio = ArrayList<TrackOption>()
        val text = ArrayList<TrackOption>()
        var videoHeight = 0
        var selectedAudioLang: String? = null
        val video = ArrayList<VideoTrack>()
        tracks.groups.forEachIndexed { gi, group ->
            for (ti in 0 until group.length) {
                if (!group.isTrackSupported(ti)) continue
                val format = group.getTrackFormat(ti)
                val id = "$gi:$ti"
                when (group.type) {
                    C.TRACK_TYPE_AUDIO -> {
                        trackGroups[id] = group.mediaTrackGroup to ti
                        val selected = group.isTrackSelected(ti)
                        if (selected) selectedAudioLang = languageLabel(format)
                        audio += TrackOption(id, trackLabel(format, audio.size + 1), selected)
                    }
                    C.TRACK_TYPE_TEXT -> {
                        trackGroups[id] = group.mediaTrackGroup to ti
                        text += TrackOption(id, trackLabel(format, text.size + 1), group.isTrackSelected(ti))
                    }
                    C.TRACK_TYPE_VIDEO -> {
                        trackGroups[id] = group.mediaTrackGroup to ti
                        video += VideoTrack(id, format.height, format.bitrate)
                        if (group.isTrackSelected(ti)) videoHeight = format.height
                    }
                }
            }
        }
        val badge = listOfNotNull(videoHeight.takeIf { it > 0 }?.let { "${it}p" }, selectedAudioLang).joinToString(" · ").ifEmpty { null }
        videoTracks = video
        _state.update { it.copy(audioTracks = audio, subtitleTracks = text, subtitlesOff = text.none { t -> t.selected }, badge = badge) }
        refreshQualityOptions()
    }

    private fun trackLabel(format: Format, ordinal: Int): String =
        format.label?.takeIf { it.isNotBlank() } ?: languageLabel(format) ?: "Faixa $ordinal"

    private fun languageLabel(format: Format): String? {
        val lang = format.language?.takeIf { it.isNotBlank() && it != "und" } ?: return null
        val display = Locale.forLanguageTag(lang).getDisplayLanguage(Locale("pt", "BR"))
        return display.takeIf { it.isNotBlank() && it != lang }?.replaceFirstChar { it.uppercase() } ?: lang.uppercase()
    }

    // ------------------------------------------------------------------ ciclo de vida

    fun onBackground() {
        wasPlayingBeforeBackground = player.isPlaying
        saveProgress()
        player.pause()
    }

    fun onForeground() {
        if (wasPlayingBeforeBackground) player.play()
    }

    override fun onCleared() {
        saveProgress()
        player.removeListener(listener)
        player.release()
        super.onCleared()
    }

    private companion object {
        const val TICK_MS = 500L
        const val SAVE_INTERVAL_MS = 10_000L
        const val CONTROLS_TIMEOUT_MS = 4_000L
        const val NEXT_CARD_WINDOW_MS = 10_000L
        const val LIVE_RETRY_DELAY_MS = 1_500L
        const val DIGIT_COMMIT_MS = 1_500L
        const val TOAST_MS = 2_500L
        const val STALL_LIMIT = 3
        const val STALL_WINDOW_MS = 90_000L
    }
}
