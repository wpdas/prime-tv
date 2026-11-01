package com.wpdas.primetv.ui.screens.player

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.data.player.PlayerFactory
import com.wpdas.primetv.domain.model.Episode
import com.wpdas.primetv.ui.components.Artwork
import com.wpdas.primetv.ui.components.EpisodeRow
import com.wpdas.primetv.ui.components.LiveBadge
import com.wpdas.primetv.ui.components.LoadingSpinner
import com.wpdas.primetv.ui.components.PrimeButton
import com.wpdas.primetv.ui.components.PrimeButtonStyle
import com.wpdas.primetv.ui.components.PrimeIconButton
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = PrimeTheme.colors
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val rootFocus = remember { FocusRequester() }
    val playPauseFocus = remember { FocusRequester() }

    // Tela acesa e pausa ao ir para segundo plano.
    DisposableEffect(lifecycleOwner) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onBackground()
                Lifecycle.Event.ON_START -> viewModel.onForeground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(state.finished) { if (state.finished) onBack() }

    BackHandler {
        when {
            state.panel != PlayerPanel.NONE -> viewModel.closePanel()
            state.controlsVisible && !state.hasError && !state.isBuffering -> viewModel.hideControls()
            else -> onBack()
        }
    }

    // Foco: controles visíveis -> botão play/pause; escondidos -> a própria tela recebe as teclas.
    LaunchedEffect(state.controlsVisible, state.panel, state.loaded) {
        if (!state.loaded) return@LaunchedEffect
        if (state.panel != PlayerPanel.NONE) return@LaunchedEffect
        if (state.controlsVisible) runCatching { playPauseFocus.requestFocus() } else runCatching { rootFocus.requestFocus() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event -> handleKey(event.type == KeyEventType.KeyDown, event.key, state, viewModel) },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    setKeepContentOnPlayerReset(true)
                    player = viewModel.player
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (state.isBuffering && !state.hasError) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingSpinner(size = 44.dp, strokeWidth = 4.dp) }
        }

        AnimatedVisibility(visible = state.controlsVisible || state.hasError, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                TopOverlay(state = state, onBack = onBack)
                if (state.hasError) {
                    ErrorOverlay(state = state, onRetry = viewModel::retry, onBack = onBack, modifier = Modifier.align(Alignment.Center))
                } else {
                    BottomControls(state = state, viewModel = viewModel, playPauseFocus = playPauseFocus, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }

        val countdown = state.nextCountdown
        val next = state.nextEpisode
        if (countdown != null && next != null && !state.hasError) {
            NextEpisodeCard(next = next, seconds = countdown, modifier = Modifier.align(Alignment.BottomEnd).padding(end = PrimeDimens.screenMargin, bottom = 170.dp))
        }

        when (state.panel) {
            PlayerPanel.AUDIO -> TrackPanel(
                title = t("player.audio"),
                options = state.audioTracks,
                offLabel = null,
                offSelected = false,
                onSelect = { viewModel.selectAudio(it) },
                onOff = null,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
            PlayerPanel.SUBTITLES -> TrackPanel(
                title = t("player.subtitles"),
                options = state.subtitleTracks,
                offLabel = t("player.subtitles_off"),
                offSelected = state.subtitlesOff,
                onSelect = { viewModel.selectSubtitle(it) },
                onOff = { viewModel.selectSubtitle(null) },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
            PlayerPanel.EPISODES -> EpisodesPanel(state = state, onPlay = viewModel::playEpisode, modifier = Modifier.align(Alignment.CenterEnd))
            PlayerPanel.NONE -> Unit
        }
    }
}

/** Teclas com os controles escondidos: OK mostra, esquerda/direita pulam 10 s, cima/baixo trocam canal. */
private fun handleKey(isDown: Boolean, key: Key, state: PlayerUiState, vm: PlayerViewModel): Boolean {
    if (!isDown) return false
    // Teclas de mídia valem sempre.
    when (key) {
        Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> { vm.togglePlayPause(); return true }
        Key.MediaFastForward -> { vm.seekBy(PlayerFactory.SEEK_STEP_MS); return true }
        Key.MediaRewind -> { vm.seekBy(-PlayerFactory.SEEK_STEP_MS); return true }
        Key.MediaNext, Key.ChannelUp -> { if (state.isLive) vm.stepChannel(+1) else vm.playNextEpisode(); return true }
        Key.MediaPrevious, Key.ChannelDown -> { if (state.isLive) vm.stepChannel(-1); return true }
        else -> Unit
    }
    if (state.panel != PlayerPanel.NONE || state.hasError) return false
    if (state.controlsVisible) {
        // Controles visíveis: o foco navega entre os botões; só reinicia o relógio de ocultar.
        vm.showControls()
        return false
    }
    return when (key) {
        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { vm.showControls(); true }
        Key.DirectionLeft -> { if (state.isLive) vm.showControls() else vm.seekBy(-PlayerFactory.SEEK_STEP_MS); true }
        Key.DirectionRight -> { if (state.isLive) vm.showControls() else vm.seekBy(PlayerFactory.SEEK_STEP_MS); true }
        Key.DirectionUp -> { if (state.isLive) vm.stepChannel(-1) else vm.showControls(); true }
        Key.DirectionDown -> { if (state.isLive) vm.stepChannel(+1) else vm.showControls(); true }
        Key.Back, Key.Escape -> false
        else -> { vm.showControls(); true }
    }
}

@Composable
private fun TopOverlay(state: PlayerUiState, onBack: () -> Unit) {
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(colors.background.copy(alpha = 0.85f), Color.Transparent)))
            .padding(horizontal = PrimeDimens.screenMargin, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PrimeIconButton(icon = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = t("common.back"), onClick = onBack, style = PrimeButtonStyle.Ghost, size = 44.dp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.isLive) LiveBadge(t("common.live"))
                Text(
                    if (state.isLive && state.channel != null) "${state.channel.number}  ${state.title}" else state.title,
                    style = typography.subtitle, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            val sub = if (state.isLive) state.nowProgram?.title ?: state.subtitle else state.subtitle
            if (sub != null) Text(sub, style = typography.body, color = colors.textBody, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        state.badge?.let {
            Text(
                it,
                style = typography.caption,
                color = colors.textBody,
                modifier = Modifier
                    .border(1.dp, colors.dotInactive, RoundedCornerShape(4.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun BottomControls(state: PlayerUiState, viewModel: PlayerViewModel, playPauseFocus: FocusRequester, modifier: Modifier = Modifier) {
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, colors.background.copy(alpha = 0.92f))))
            .padding(start = PrimeDimens.screenMargin, end = PrimeDimens.screenMargin, top = 40.dp, bottom = 30.dp)
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!state.isLive) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(formatTime(state.positionMs), style = typography.cardBody, color = colors.textBody)
                SeekBar(progress = state.progress, buffered = state.buffered, modifier = Modifier.weight(1f))
                Text(formatTime(state.durationMs), style = typography.cardBody, color = colors.textBody)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                state.nowProgram?.let { program ->
                    Text(
                        "${t("live.now")} · ${formatClock(program.start)} – ${formatClock(program.end)}",
                        style = typography.caption, color = colors.textSecondary,
                    )
                    SeekBar(progress = program.progressAt(System.currentTimeMillis()), buffered = 0f, modifier = Modifier.weight(1f), showThumb = false)
                } ?: Text(t("player.live_hint"), style = typography.caption, color = colors.textSecondary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!state.isLive) {
                RoundButton(icon = Icons.Rounded.Replay10, description = "-10", onClick = { viewModel.seekBy(-PlayerFactory.SEEK_STEP_MS) }, size = 52.dp)
            }
            RoundButton(
                icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                description = if (state.isPlaying) "pause" else "play",
                onClick = viewModel::togglePlayPause,
                size = 60.dp,
                primary = true,
                modifier = Modifier.focusRequester(playPauseFocus),
            )
            if (!state.isLive) {
                RoundButton(icon = Icons.Rounded.Forward10, description = "+10", onClick = { viewModel.seekBy(PlayerFactory.SEEK_STEP_MS) }, size = 52.dp)
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.audioTracks.size > 1) OptionButton(t("player.audio")) { viewModel.openPanel(PlayerPanel.AUDIO) }
                if (state.subtitleTracks.isNotEmpty()) OptionButton(t("player.subtitles")) { viewModel.openPanel(PlayerPanel.SUBTITLES) }
                if (state.episodes.isNotEmpty()) OptionButton(t("player.episodes")) { viewModel.openPanel(PlayerPanel.EPISODES) }
                if (state.nextEpisode != null) OptionButton(t("player.next_episode")) { viewModel.playNextEpisode() }
            }
        }
    }
}

@Composable
private fun SeekBar(progress: Float, buffered: Float, modifier: Modifier = Modifier, showThumb: Boolean = true) {
    val colors = PrimeTheme.colors
    Box(modifier.height(18.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(6.dp).background(colors.divider, RoundedCornerShape(3.dp)))
        Box(Modifier.fillMaxWidth(buffered.coerceIn(0f, 1f)).height(6.dp).background(colors.dotInactive, RoundedCornerShape(3.dp)))
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(6.dp).background(colors.accent, RoundedCornerShape(3.dp)))
        if (showThumb) {
            var trackWidth by remember { mutableIntStateOf(0) }
            Box(Modifier.fillMaxWidth().onGloballyPositioned { trackWidth = it.size.width }) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val offsetX: Dp = with(density) { (trackWidth * progress.coerceIn(0f, 1f)).toDp() } - 9.dp
                Box(Modifier.offset(x = offsetX).size(18.dp).background(colors.text, CircleShape))
            }
        }
    }
}

@Composable
private fun RoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit, size: Dp, modifier: Modifier = Modifier, primary: Boolean = false) {
    val colors = PrimeTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = PrimeFocus.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (primary) colors.text else Color.Transparent,
            contentColor = if (primary) colors.background else colors.text,
            focusedContainerColor = if (primary) colors.text else colors.surface2,
            focusedContentColor = if (primary) colors.background else colors.text,
            pressedContainerColor = colors.accent,
            pressedContentColor = colors.onAccent,
        ),
        scale = PrimeFocus.scale(),
        border = PrimeFocus.border(CircleShape),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(if (primary) 30.dp else 26.dp))
        }
    }
}

@Composable
private fun OptionButton(label: String, onClick: () -> Unit) {
    PrimeButton(
        text = label,
        onClick = onClick,
        style = PrimeButtonStyle.Secondary,
        height = 44.dp,
        horizontalPadding = 16.dp,
        textStyle = PrimeTheme.typography.buttonSmall,
    )
}

@Composable
private fun ErrorOverlay(state: PlayerUiState, onRetry: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PrimeTheme.colors
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { retryFocus.requestFocus() } }
    Column(
        modifier.width(520.dp).background(colors.surface, PrimeTheme.shapes.panel).padding(28.dp).focusGroup(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(t(state.errorKey ?: "player.error", "detail" to (state.errorDetail ?: "")), style = PrimeTheme.typography.bodyLarge, color = colors.text)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimeButton(text = t("common.retry"), onClick = onRetry, modifier = Modifier.focusRequester(retryFocus))
            PrimeButton(text = t("common.back"), onClick = onBack, style = PrimeButtonStyle.Secondary)
        }
    }
}

@Composable
private fun NextEpisodeCard(next: Episode, seconds: Int, modifier: Modifier = Modifier) {
    val colors = PrimeTheme.colors
    Row(
        modifier.width(300.dp).background(colors.surface, PrimeTheme.shapes.panel).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(96.dp, 54.dp).clip(PrimeTheme.shapes.small)) { Artwork(next.posterUrl, colors.surface2, Modifier.fillMaxSize()) }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t("player.next_in", "n" to seconds), style = PrimeTheme.typography.captionSmall, color = colors.textSecondary)
            Text("E${next.number} · ${next.title}", style = PrimeTheme.typography.bodyStrong, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TrackPanel(
    title: String,
    options: List<TrackOption>,
    offLabel: String?,
    offSelected: Boolean,
    onSelect: (TrackOption) -> Unit,
    onOff: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = PrimeTheme.colors
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(
        modifier.width(360.dp).fillMaxHeight().background(colors.surface.copy(alpha = 0.97f)).padding(24.dp).focusGroup(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = PrimeTheme.typography.subtitle, color = colors.text, modifier = Modifier.padding(bottom = 10.dp))
        if (offLabel != null && onOff != null) {
            PanelRow(label = offLabel, selected = offSelected, onClick = onOff, modifier = Modifier.focusRequester(first))
        }
        options.forEachIndexed { index, option ->
            PanelRow(
                label = option.label,
                selected = option.selected,
                onClick = { onSelect(option) },
                modifier = if (index == 0 && offLabel == null) Modifier.focusRequester(first) else Modifier,
            )
        }
    }
}

@Composable
private fun PanelRow(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.card
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) colors.accentSoftBackground else Color.Transparent,
            contentColor = colors.text,
            focusedContainerColor = colors.selected,
            focusedContentColor = colors.text,
            pressedContainerColor = colors.selected,
            pressedContentColor = colors.text,
        ),
        scale = PrimeFocus.noScale(),
        border = PrimeFocus.border(shape),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = PrimeTheme.typography.listTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.accentSoftText, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EpisodesPanel(state: PlayerUiState, onPlay: (Episode) -> Unit, modifier: Modifier = Modifier) {
    val colors = PrimeTheme.colors
    val current = state.currentEpisode
    val season = current?.season ?: state.episodes.firstOrNull()?.season ?: 1
    val episodes = state.episodes.filter { it.season == season }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(
        modifier.width(560.dp).fillMaxHeight().background(colors.surface.copy(alpha = 0.97f)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(t("player.episodes_title", "n" to season), style = PrimeTheme.typography.subtitle, color = colors.text, modifier = Modifier.padding(bottom = 6.dp))
        LazyColumn(Modifier.fillMaxSize().focusRestorer().focusGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(episodes, key = { it.id }) { episode ->
                EpisodeRow(
                    episode = episode,
                    onClick = { onPlay(episode) },
                    stateLabel = if (episode.id == current?.id) t("player.now_playing") else null,
                    modifier = if (episode.id == (current?.id ?: episodes.firstOrNull()?.id)) Modifier.focusRequester(first) else Modifier,
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%02d:%02d", m, s)
}

private fun formatClock(epochMs: Long): String =
    java.time.Instant.ofEpochMilli(epochMs).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
