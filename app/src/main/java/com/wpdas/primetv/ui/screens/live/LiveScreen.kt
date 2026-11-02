package com.wpdas.primetv.ui.screens.live

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.domain.model.Channel
import com.wpdas.primetv.domain.model.EpgProgram
import com.wpdas.primetv.domain.model.HomeTab
import com.wpdas.primetv.ui.components.Artwork
import com.wpdas.primetv.ui.components.CategoryRail
import com.wpdas.primetv.ui.components.throttleHeldDpad
import com.wpdas.primetv.ui.components.ChannelRow
import com.wpdas.primetv.ui.components.LiveBadge
import com.wpdas.primetv.ui.components.LoadingSpinner
import com.wpdas.primetv.ui.components.PrimeButton
import com.wpdas.primetv.ui.components.PrimeButtonStyle
import com.wpdas.primetv.ui.components.StatusToast
import com.wpdas.primetv.ui.components.tryRequestFocus
import com.wpdas.primetv.ui.components.TopBar
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun LiveScreen(
    onTabSelected: (HomeTab) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAppMenu: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlay: (Channel) -> Unit,
    viewModel: LiveViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = PrimeTheme.colors
    val firstChannelFocus = remember { FocusRequester() }
    val jumpFocus = remember { FocusRequester() }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val channels = state.channels
    LaunchedEffect(channels?.firstOrNull()?.id, state.selectedRailId) {
        if (!channels.isNullOrEmpty() && state.focused == null && state.jumpToChannelId == null) firstChannelFocus.tryRequestFocus()
    }
    // Voltando do player: a tela entra de novo na composição; foca o canal que estava tocando.
    LaunchedEffect(Unit) { viewModel.focusLastPlayed() }
    // Número digitado: rola até o canal e foca.
    LaunchedEffect(state.jumpToChannelId, channels) {
        val id = state.jumpToChannelId ?: return@LaunchedEffect
        val index = channels?.indexOfFirst { it.id == id } ?: -1
        if (index < 0) return@LaunchedEffect
        // Rola, espera o item entrar na composição e foca; só desiste depois de ~2 s. O pedido
        // continua pendente se a lista trocar no meio (ex.: restauração da categoria ao abrir).
        repeat(25) {
            runCatching { listState.scrollToItem(index) }
            kotlinx.coroutines.delay(80)
            if (jumpFocus.tryRequestFocus()) {
                viewModel.onJumpConsumed()
                return@LaunchedEffect
            }
        }
        viewModel.onJumpConsumed()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val digit = digitOf(event.key)
                when {
                    digit != null -> { viewModel.onDigit(digit); true }
                    state.channelInput != null && (event.key == Key.DirectionCenter || event.key == Key.Enter) -> { viewModel.commitChannelInput(); true }
                    else -> false
                }
            },
    ) {
        val tabScope = androidx.compose.runtime.rememberCoroutineScope()
        var tabFocusJob by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<kotlinx.coroutines.Job?>(null) }
        TopBar(
            selectedTab = HomeTab.LIVE,
            onTabSelected = onTabSelected,
            onTabFocused = { tab ->
                tabFocusJob?.cancel()
                if (tab != HomeTab.LIVE) tabFocusJob = tabScope.launch { kotlinx.coroutines.delay(700); onTabSelected(tab) }
            },
            onLogoClick = onOpenAppMenu,
            onSearchClick = onOpenSearch,
            onSettingsClick = onOpenSettings,
        )
        Row(
            Modifier.fillMaxSize().padding(start = PrimeDimens.screenMargin, end = PrimeDimens.screenMargin, top = 8.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CategoryRail(
                categories = state.rail,
                selectedId = state.selectedRailId,
                onSelect = { viewModel.selectRail(it.id) },
                modifier = Modifier.fillMaxHeight(),
            )
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    channels == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingSpinner(size = 32.dp) }
                    channels.isEmpty() -> Text(t("live.empty"), style = PrimeTheme.typography.bodyLarge, color = colors.textSecondary, modifier = Modifier.padding(12.dp))
                    else -> LazyColumn(
                        Modifier.fillMaxSize().focusRestorer().focusGroup()
                            .throttleHeldDpad(vertical = true) { listState.isScrollInProgress },
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp),
                    ) {
                        items(channels, key = { it.id }) { channel ->
                            val now = channel.epgKey?.let { state.nowPlaying[it] }
                            ChannelRow(
                                number = channel.number.toString(),
                                name = channel.displayName,
                                logoUrl = channel.logoUrl,
                                nowTitle = now?.title,
                                timeRange = now?.let { timeRange(it) },
                                progress = now?.progressAt(state.now) ?: 0f,
                                onClick = { viewModel.stopPreview(); viewModel.markReturnFocus(); onPlay(channel) },
                                onLongClick = { viewModel.toggleFavorite(channel) },
                                onFocused = { viewModel.onChannelFocused(channel) },
                                qualityLabel = channel.qualityLabel,
                                modifier = when (channel.id) {
                                    state.jumpToChannelId -> Modifier.focusRequester(jumpFocus)
                                    channels.first().id -> Modifier.focusRequester(firstChannelFocus)
                                    else -> Modifier
                                },
                            )
                        }
                    }
                }
            }
            PreviewPanel(
                state = state,
                onPlay = { viewModel.stopPreview(); viewModel.markReturnFocus(); onPlay(it) },
                onToggleFavorite = viewModel::toggleFavorite,
                playerFactory = viewModel.playerFactory,
                modifier = Modifier.width(380.dp).fillMaxHeight(),
            )
        }
    }
    Box(Modifier.fillMaxSize()) {
        state.channelInput?.let { input ->
            Text(
                "$input\u2009_",
                style = PrimeTheme.typography.heroTitle,
                color = colors.text,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 90.dp, end = PrimeDimens.screenMargin)
                    .background(colors.surface2, PrimeTheme.shapes.card).padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        StatusToast(
            text = state.toastKey?.let { t(it, "value" to (state.channelInput ?: "")) },
            showSpinner = false,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = PrimeDimens.screenMargin, bottom = 20.dp),
        )
    }
}

/** Prévia do canal em foco: arte, Agora / A seguir e ações. O player pequeno entra na fase 4. */
@Composable
private fun PreviewPanel(
    state: LiveUiState,
    onPlay: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    playerFactory: com.wpdas.primetv.data.player.PlayerFactory,
    modifier: Modifier = Modifier,
) {
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    val channel = state.focused
    Column(modifier.focusGroup(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().height(214.dp).clip(PrimeTheme.shapes.panel).background(colors.surface2)) {
            if (channel != null) {
                Box(Modifier.align(Alignment.Center).size(160.dp, 90.dp).clip(PrimeTheme.shapes.card)) {
                    Artwork(channel.logoUrl, colors.surface, Modifier.fillMaxSize(), channel.name, contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                }
                LivePreview(url = state.previewUrl, playerFactory = playerFactory, modifier = Modifier.fillMaxSize(), withSound = state.previewSound)
                LiveBadge(t("common.live"), Modifier.align(Alignment.TopStart).padding(12.dp))
                Text(
                    listOfNotNull(channel.displayName, channel.qualityLabel).joinToString(" · "),
                    style = typography.captionStrong,
                    color = colors.text,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).background(colors.background, PrimeTheme.shapes.small).padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        val now = state.focusedNowNext.now
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (channel == null) {
                Text(t("live.now"), style = typography.caption, color = colors.textSecondary)
            } else if (now != null) {
                Text("${t("live.now")} · ${timeRange(now)}", style = typography.caption, color = colors.textSecondary)
                Text(now.title, style = typography.subtitle, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                now.description?.let { Text(it, style = typography.cardBody, color = colors.textBody, maxLines = 3, overflow = TextOverflow.Ellipsis) }
            } else {
                Text(channel.displayName, style = typography.subtitle, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(t("live.no_epg"), style = typography.cardBody, color = colors.textSecondary)
            }
        }
        if (state.focusedNowNext.next.isNotEmpty()) {
            Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.surface2))
                Text(t("live.next"), style = typography.caption, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
                state.focusedNowNext.next.forEach { p ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(startTime(p), style = typography.cardBody, color = colors.textSecondary, modifier = Modifier.width(44.dp))
                        Text(p.title, style = typography.cardBody, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (channel != null) {
            val isFavorite = channel.id in state.favoriteIds
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimeButton(text = t("live.watch"), onClick = { onPlay(channel) }, height = 44.dp, horizontalPadding = 20.dp, textStyle = typography.buttonMedium)
                PrimeButton(
                    text = t(if (isFavorite) "live.unfavorite" else "live.favorite"),
                    onClick = { onToggleFavorite(channel) },
                    style = PrimeButtonStyle.Secondary,
                    height = 44.dp,
                    horizontalPadding = 16.dp,
                    textStyle = typography.buttonSmall,
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(HOUR_MINUTE)
private fun timeRange(p: EpgProgram) = "${formatTime(p.start)} – ${formatTime(p.end)}"
private fun startTime(p: EpgProgram) = formatTime(p.start)

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
