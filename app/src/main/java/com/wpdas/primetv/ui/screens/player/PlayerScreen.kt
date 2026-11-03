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
import androidx.compose.ui.focus.onFocusChanged
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
import com.wpdas.primetv.ui.components.tryRequestFocus
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
    // Sair de propósito: desmarca o canal "em exibição" (o app não reabre nele).
    val leave: () -> Unit = { viewModel.onLeave(); onBack() }

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

    LaunchedEffect(state.finished) { if (state.finished) leave() }

    BackHandler {
        when {
            state.panel != PlayerPanel.NONE -> viewModel.closePanel()
            state.liveMenu -> viewModel.closeLiveMenu()
            state.controlsVisible && !state.hasError && !state.isBuffering -> viewModel.hideControls()
            else -> leave()
        }
    }

    // Foco: controles visíveis -> botão play/pause; escondidos -> a própria tela recebe as teclas.
    LaunchedEffect(state.controlsVisible, state.panel, state.loaded, state.isLive, state.liveMenu) {
        if (!state.loaded) return@LaunchedEffect
        if (state.panel != PlayerPanel.NONE) return@LaunchedEffect
        when {
            state.isLive && !state.liveMenu -> runCatching { rootFocus.requestFocus() }
            state.isLive && state.liveMenu -> runCatching { playPauseFocus.requestFocus() } // primeiro botão do menu
            state.controlsVisible -> runCatching { playPauseFocus.requestFocus() }
            else -> runCatching { rootFocus.requestFocus() }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event -> handleKey(event, state, viewModel) },
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
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                LoadingSpinner(size = 44.dp, strokeWidth = 4.dp)
                // Velocidade do download do stream (não é teste de velocidade): explica um travamento.
                state.bitrateBps?.let { bps ->
                    Text(
                        com.wpdas.primetv.core.util.Bitrate.format(bps),
                        style = PrimeTheme.typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 10.dp).background(colors.background.copy(alpha = 0.6f), PrimeTheme.shapes.small).padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }

        AnimatedVisibility(visible = state.controlsVisible || state.hasError, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                TopOverlay(state = state, onBack = leave)
                if (state.hasError) {
                    ErrorOverlay(state = state, onRetry = viewModel::retry, onBack = leave, modifier = Modifier.align(Alignment.Center))
                } else {
                    BottomControls(state = state, viewModel = viewModel, playPauseFocus = playPauseFocus, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }

        state.channelInput?.let { input ->
            Text(
                "$input\u2009_",
                style = PrimeTheme.typography.heroTitle,
                color = colors.text,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 90.dp, end = PrimeDimens.screenMargin)
                    .background(colors.background.copy(alpha = 0.8f), PrimeTheme.shapes.card).padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        state.toastKey?.let { key ->
            Text(
                t(key, "value" to (state.toastArg ?: "")),
                style = PrimeTheme.typography.body,
                color = colors.text,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = PrimeDimens.screenMargin, bottom = 170.dp)
                    .background(colors.surface2, PrimeTheme.shapes.card).padding(horizontal = 14.dp, vertical = 10.dp),
            )
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
                onClose = viewModel::closePanel,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
            PlayerPanel.SUBTITLES -> TrackPanel(
                title = t("player.subtitles"),
                options = state.subtitleTracks,
                offLabel = t("player.subtitles_off"),
                offSelected = state.subtitlesOff,
                onSelect = { viewModel.selectSubtitle(it) },
                onOff = { viewModel.selectSubtitle(null) },
                onClose = viewModel::closePanel,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
            PlayerPanel.EPISODES -> EpisodesPanel(state = state, onPlay = viewModel::playEpisode, modifier = Modifier.align(Alignment.CenterEnd))
            PlayerPanel.QUALITY -> QualityPanel(options = state.qualityOptions, onSelect = viewModel::selectQuality, onClose = viewModel::closePanel, modifier = Modifier.align(Alignment.CenterEnd))
            PlayerPanel.CHANNELS -> ChannelsPanel(state = state, onSelect = viewModel::selectChannel, modifier = Modifier.align(Alignment.CenterStart))
            PlayerPanel.NONE -> Unit
        }
    }
}

/**
 * Teclas no player.
 * - Ao vivo: cima/baixo sempre trocam de canal, OK abre a lista de canais à esquerda (ou retoma
 *   se estiver pausado pela tecla de mídia), números escolhem o canal, esquerda/direita abrem a
 *   linha de opções (Áudio/Legendas/Qualidade).
 * - Filmes/séries com controles escondidos: qualquer tecla mostra os controles; esquerda/direita só
 *   avançam na barra de progresso (segurar acelera), nunca com a barra escondida.
 */
private fun handleKey(event: androidx.compose.ui.input.key.KeyEvent, state: PlayerUiState, vm: PlayerViewModel): Boolean {
    val key = event.key
    // Voltar com um painel aberto fecha o painel de uma vez (consome down e up, para o
    // sistema não tratar o segundo evento como outro "voltar").
    if (key == Key.Back || key == Key.Escape) {
        val hasOverlay = state.panel != PlayerPanel.NONE || state.liveMenu
        if (!hasOverlay) return false
        if (event.type == KeyEventType.KeyUp) {
            if (state.panel != PlayerPanel.NONE) vm.closePanel() else vm.closeLiveMenu()
        }
        return true
    }
    if (event.type != KeyEventType.KeyDown) return false
    val repeat = event.nativeKeyEvent.repeatCount
    // Teclas de mídia valem sempre.
    when (key) {
        Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> { vm.togglePlayPause(); return true }
        Key.MediaFastForward -> { vm.seekBy(vm.seekStep(repeat)); return true }
        Key.MediaRewind -> { vm.seekBy(-vm.seekStep(repeat)); return true }
        Key.MediaNext, Key.ChannelUp -> { if (state.isLive) vm.stepChannel(+1) else vm.playNextEpisode(); return true }
        Key.MediaPrevious, Key.ChannelDown -> { if (state.isLive) vm.stepChannel(-1); return true }
        else -> Unit
    }
    if (state.panel != PlayerPanel.NONE || state.hasError) return false
    if (state.isLive) {
        digitOf(key)?.let { vm.onDigit(it); return true }
        if (state.channelInput != null && (key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter)) {
            vm.commitChannelInput(); return true
        }
        when (key) {
            Key.DirectionUp -> { if (repeat == 0) vm.stepChannel(-1); return true }
            Key.DirectionDown -> { if (repeat == 0) vm.stepChannel(+1); return true }
            else -> Unit
        }
        if (state.liveMenu) return false // foco navega entre Áudio/Legendas
        return when (key) {
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                // Pausado pela tecla de mídia: OK retoma. Tocando: OK abre a lista de canais.
                if (!state.isPlaying && !state.isBuffering) vm.togglePlayPause() else vm.openChannels()
                true
            }
            Key.DirectionLeft, Key.DirectionRight -> { vm.openLiveMenu(); true }
            Key.Back, Key.Escape -> false
            else -> { vm.showControls(); true }
        }
    }
    if (state.controlsVisible) {
        // Controles visíveis: o foco navega entre os botões; só reinicia o relógio de ocultar.
        vm.showControls()
        return false
    }
    // Controles escondidos: qualquer tecla só os mostra. Avançar/voltar fica na barra de progresso,
    // para um toque acidental no controle não pular o filme.
    return when (key) {
        Key.Back, Key.Escape -> false
        else -> { vm.showControls(); true }
    }
}

private fun digitOf(key: Key): Int? = when (key) {
    Key.Zero, Key.NumPad0 -> 0
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
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
    val seekFocus = remember { FocusRequester() }
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, colors.background.copy(alpha = 0.92f))))
            .padding(start = PrimeDimens.screenMargin, end = PrimeDimens.screenMargin, top = 40.dp, bottom = 30.dp)
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.isLive) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                state.nowProgram?.let { program ->
                    Text(
                        "${t("live.now")} · ${formatClock(program.start)} – ${formatClock(program.end)}",
                        style = typography.caption, color = colors.textSecondary,
                    )
                    SeekBar(progress = program.progressAt(System.currentTimeMillis()), buffered = 0f, modifier = Modifier.weight(1f), showThumb = false)
                } ?: Text(t("player.live_hint"), style = typography.caption, color = colors.textSecondary, modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Carregando conta como "tocando": só a pausa de verdade (tecla de mídia) muda a dica.
                val active = state.isPlaying || state.isBuffering
                Icon(
                    if (active) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null, tint = colors.text, modifier = Modifier.size(28.dp),
                )
                Text(t(if (active) "player.live_keys" else "player.live_paused"), style = typography.cardBody, color = colors.textBody, modifier = Modifier.weight(1f))
                if (state.liveMenu) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OptionButton(t("player.audio"), modifier = Modifier.focusRequester(playPauseFocus)) { viewModel.openPanel(PlayerPanel.AUDIO) }
                        OptionButton(t("player.subtitles")) { viewModel.openPanel(PlayerPanel.SUBTITLES) }
                        OptionButton(t("player.quality")) { viewModel.openPanel(PlayerPanel.QUALITY) }
                    }
                } else {
                    Text(t("player.live_menu_hint"), style = typography.caption, color = colors.textSecondary)
                }
            }
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(formatTime(state.positionMs), style = typography.cardBody, color = colors.textBody)
            SeekBar(
                progress = state.progress,
                buffered = state.buffered,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(seekFocus)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val repeat = event.nativeKeyEvent.repeatCount
                        when (event.key) {
                            Key.DirectionLeft -> { viewModel.seekBy(-viewModel.seekStep(repeat)); true }
                            Key.DirectionRight -> { viewModel.seekBy(viewModel.seekStep(repeat)); true }
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { viewModel.togglePlayPause(); true }
                            else -> false
                        }
                    },
                focusable = true,
            )
            Text(formatTime(state.durationMs), style = typography.cardBody, color = colors.textBody)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundButton(icon = Icons.Rounded.Replay10, description = "-10", onClick = { viewModel.seekBy(-PlayerFactory.SEEK_STEP_MS) }, size = 52.dp)
            RoundButton(
                icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                description = if (state.isPlaying) "pause" else "play",
                onClick = viewModel::togglePlayPause,
                size = 60.dp,
                primary = true,
                modifier = Modifier.focusRequester(playPauseFocus),
            )
            RoundButton(icon = Icons.Rounded.Forward10, description = "+10", onClick = { viewModel.seekBy(PlayerFactory.SEEK_STEP_MS) }, size = 52.dp)
            Text(t("player.seek_hint"), style = typography.caption, color = colors.textSecondary, modifier = Modifier.padding(start = 8.dp))
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionButton(t("player.audio")) { viewModel.openPanel(PlayerPanel.AUDIO) }
                OptionButton(t("player.subtitles")) { viewModel.openPanel(PlayerPanel.SUBTITLES) }
                OptionButton(t("player.quality")) { viewModel.openPanel(PlayerPanel.QUALITY) }
                if (state.episodes.isNotEmpty()) OptionButton(t("player.episodes")) { viewModel.openPanel(PlayerPanel.EPISODES) }
                if (state.nextEpisode != null) OptionButton(t("player.next_episode")) { viewModel.playNextEpisode() }
            }
        }
    }
}

@Composable
private fun SeekBar(progress: Float, buffered: Float, modifier: Modifier = Modifier, showThumb: Boolean = true, focusable: Boolean = false) {
    val colors = PrimeTheme.colors
    var focused by remember { androidx.compose.runtime.mutableStateOf(false) }
    val base = if (focusable) {
        modifier
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .height(26.dp)
            .then(if (focused) Modifier.border(3.dp, colors.accent, RoundedCornerShape(13.dp)) else Modifier)
            .padding(horizontal = 6.dp)
    } else modifier.height(18.dp)
    Box(base, contentAlignment = Alignment.CenterStart) {
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
private fun OptionButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    PrimeButton(
        text = label,
        onClick = onClick,
        style = PrimeButtonStyle.Secondary,
        height = 44.dp,
        horizontalPadding = 16.dp,
        textStyle = PrimeTheme.typography.buttonSmall,
        modifier = modifier,
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
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PrimeTheme.colors
    val first = remember { FocusRequester() }
    RequestFocusWhenReady(first)
    Column(
        modifier.width(360.dp).fillMaxHeight().background(colors.surface.copy(alpha = 0.97f)).padding(24.dp).focusGroup(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = PrimeTheme.typography.subtitle, color = colors.text, modifier = Modifier.padding(bottom = 10.dp))
        if (options.isEmpty() && offLabel == null) {
            Text(t("player.single_audio"), style = PrimeTheme.typography.body, color = colors.textSecondary)
            PrimeButton(text = t("common.close"), onClick = onClose, style = PrimeButtonStyle.Secondary, height = 44.dp, horizontalPadding = 18.dp, textStyle = PrimeTheme.typography.buttonSmall, modifier = Modifier.focusRequester(first).padding(top = 8.dp))
        }
        if (options.isEmpty() && offLabel != null) {
            Text(t("player.no_subtitles"), style = PrimeTheme.typography.body, color = colors.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
        }
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

/** Menu Qualidade: seção "Resolução do vídeo" (Auto + faixas) e "Versões na lista" (4K/FHD/HD/SD). */
@Composable
private fun QualityPanel(options: List<QualityOption>, onSelect: (QualityOption) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PrimeTheme.colors
    val first = remember { FocusRequester() }
    RequestFocusWhenReady(first)
    val tracks = options.filter { it.kind != QualityKind.SOURCE }
    val sources = options.filter { it.kind == QualityKind.SOURCE }
    Column(
        modifier.width(360.dp).fillMaxHeight().background(colors.surface.copy(alpha = 0.97f)).padding(24.dp).focusGroup(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(t("player.quality"), style = PrimeTheme.typography.subtitle, color = colors.text, modifier = Modifier.padding(bottom = 10.dp))
        if (options.isEmpty()) {
            Text(t("player.single_quality"), style = PrimeTheme.typography.body, color = colors.textSecondary)
            PrimeButton(text = t("common.close"), onClick = onClose, style = PrimeButtonStyle.Secondary, height = 44.dp, horizontalPadding = 18.dp, textStyle = PrimeTheme.typography.buttonSmall, modifier = Modifier.focusRequester(first).padding(top = 8.dp))
            return@Column
        }
        if (tracks.isNotEmpty()) {
            Text(t("player.quality_tracks"), style = PrimeTheme.typography.captionStrong, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
            tracks.forEachIndexed { index, option ->
                PanelRow(label = qualityLabel(option), selected = option.selected, onClick = { onSelect(option) }, modifier = if (index == 0) Modifier.focusRequester(first) else Modifier)
            }
        }
        if (sources.isNotEmpty()) {
            Text(t("player.quality_sources"), style = PrimeTheme.typography.captionStrong, color = colors.textSecondary, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
            sources.forEachIndexed { index, option ->
                PanelRow(label = qualityLabel(option), selected = option.selected, onClick = { onSelect(option) }, modifier = if (tracks.isEmpty() && index == 0) Modifier.focusRequester(first) else Modifier)
            }
        }
    }
}

/** "FHD", "Padrão" ou "Padrão · 1080p" (variante sem tag na lista, com a resolução real quando conhecida). */
@Composable
private fun qualityLabel(option: QualityOption): String {
    val key = option.labelKey ?: return option.label
    return if (option.label.isBlank()) t(key) else "${t(key)} · ${option.label}"
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
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val currentIndex = episodes.indexOfFirst { it.id == current?.id }.coerceAtLeast(0)
    // O item precisa estar composto para receber foco: rola até ele e tenta por alguns quadros.
    LaunchedEffect(Unit) {
        listState.scrollToItem(currentIndex)
        repeat(10) {
            kotlinx.coroutines.delay(50)
            if (first.tryRequestFocus()) return@LaunchedEffect
        }
    }
    Column(
        modifier.width(560.dp).fillMaxHeight().background(colors.surface.copy(alpha = 0.97f)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(t("player.episodes_title", "n" to season), style = PrimeTheme.typography.subtitle, color = colors.text, modifier = Modifier.padding(bottom = 6.dp))
        LazyColumn(Modifier.fillMaxSize().focusRestorer().focusGroup(), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)) {
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

/**
 * Lista de canais da categoria atual, à esquerda, aberta com OK no ao vivo. O canal continua
 * tocando atrás (por isso não há prévia); o item em exibição começa focado.
 */
@Composable
private fun ChannelsPanel(state: PlayerUiState, onSelect: (com.wpdas.primetv.domain.model.Channel) -> Unit, modifier: Modifier = Modifier) {
    val colors = PrimeTheme.colors
    val channels = state.zapList
    val currentId = state.channel?.id
    val first = remember { FocusRequester() }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val currentIndex = channels.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    LaunchedEffect(Unit) {
        listState.scrollToItem(currentIndex)
        repeat(10) {
            kotlinx.coroutines.delay(50)
            if (first.tryRequestFocus()) return@LaunchedEffect
        }
    }
    val now = System.currentTimeMillis()
    Column(
        modifier.width(480.dp).fillMaxHeight().background(colors.surface.copy(alpha = 0.97f)).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp)) {
            Text(state.subtitle ?: t("live.all"), style = PrimeTheme.typography.subtitle, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Text(t("live.channels_count", "n" to channels.size), style = PrimeTheme.typography.caption, color = colors.textSecondary)
        }
        LazyColumn(
            Modifier.fillMaxSize().focusRestorer().focusGroup(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp, horizontal = 6.dp),
        ) {
            items(channels, key = { it.id }) { channel ->
                val program = channel.epgKey?.let { state.zapNowPlaying[it] }
                com.wpdas.primetv.ui.components.ChannelRow(
                    number = channel.number.toString(),
                    name = channel.displayName,
                    logoUrl = channel.logoUrl,
                    nowTitle = program?.title,
                    timeRange = if (channel.id == currentId) t("player.now_playing") else program?.let { "${formatClock(it.start)} – ${formatClock(it.end)}" },
                    progress = program?.progressAt(now) ?: 0f,
                    onClick = { onSelect(channel) },
                    qualityLabel = channel.qualityLabel,
                    modifier = if (channel.id == (currentId ?: channels.firstOrNull()?.id)) Modifier.focusRequester(first) else Modifier,
                )
            }
        }
    }
}

/** Pede foco assim que o alvo estiver anexado (painéis que acabaram de aparecer). */
@Composable
private fun RequestFocusWhenReady(requester: FocusRequester) {
    LaunchedEffect(Unit) {
        repeat(10) {
            kotlinx.coroutines.delay(40)
            if (requester.tryRequestFocus()) return@LaunchedEffect
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
