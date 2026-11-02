package com.wpdas.primetv.ui.screens.football

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.domain.football.MatchEvent
import com.wpdas.primetv.domain.football.MatchStatus
import com.wpdas.primetv.domain.model.CategoryRow
import com.wpdas.primetv.domain.model.HomeTab
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.ui.components.CategoryRail
import com.wpdas.primetv.ui.components.ChannelRow
import com.wpdas.primetv.ui.components.LoadingSpinner
import com.wpdas.primetv.ui.components.MatchRow
import com.wpdas.primetv.ui.components.PosterCard
import com.wpdas.primetv.ui.components.PrimeButton
import com.wpdas.primetv.ui.components.PrimeButtonStyle
import com.wpdas.primetv.ui.components.StatusToast
import com.wpdas.primetv.ui.components.TopBar
import com.wpdas.primetv.ui.components.throttleHeldDpad
import com.wpdas.primetv.ui.components.tryRequestFocus
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")
private val LONG_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR"))
private val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Guia de jogos: trilho (Jogos · Ao vivo agora · Canais · Vídeos) e lista de partidas com horário, escudos e canal. */
@Composable
fun FootballScreen(
    onTabSelected: (HomeTab) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAppMenu: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlayChannel: (String) -> Unit,
    onOpenItem: (MediaItem) -> Unit,
    viewModel: FootballViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = PrimeTheme.colors
    val scope = rememberCoroutineScope()
    val firstFocus = remember { FocusRequester() }
    val jumpFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()

    val visible = remember(state.events, state.selectedRailId, state.now) {
        when (state.selectedRailId) {
            FootballUiState.RAIL_LIVE -> state.events?.filter { it.status(state.now) == MatchStatus.LIVE }
            else -> state.events
        }
    }

    LaunchedEffect(Unit) { viewModel.focusLastPlayed() }
    LaunchedEffect(state.focusEventKey, visible) {
        val key = state.focusEventKey ?: return@LaunchedEffect
        val index = visible?.indexOfFirst { it.key == key } ?: -1
        if (index < 0) { viewModel.onFocusConsumed(); return@LaunchedEffect }
        repeat(25) {
            runCatching { listState.scrollToItem(index) }
            delay(80)
            if (jumpFocus.tryRequestFocus()) { viewModel.onFocusConsumed(); return@LaunchedEffect }
        }
        viewModel.onFocusConsumed()
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        var tabFocusJob by remember { mutableStateOf<Job?>(null) }
        TopBar(
            selectedTab = HomeTab.FOOTBALL,
            tabs = state.tabs,
            onTabSelected = onTabSelected,
            onTabFocused = { tab ->
                tabFocusJob?.cancel()
                if (tab != HomeTab.FOOTBALL) tabFocusJob = scope.launch { delay(TAB_FOCUS_DELAY_MS); onTabSelected(tab) }
            },
            onSearchClick = onOpenSearch,
            onMenuClick = onOpenAppMenu,
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
                when (state.selectedRailId) {
                    FootballUiState.RAIL_CHANNELS -> ChannelList(state, onPlayChannel)
                    FootballUiState.RAIL_VIDEOS -> VideoRows(state.videos, onOpenItem)
                    else -> MatchList(
                        events = visible,
                        now = state.now,
                        listState = listState,
                        firstFocus = firstFocus,
                        jumpFocus = jumpFocus,
                        jumpKey = state.focusEventKey,
                        liveOnly = state.selectedRailId == FootballUiState.RAIL_LIVE,
                        onRefresh = viewModel::refresh,
                        onShowChannels = { viewModel.selectRail(FootballUiState.RAIL_CHANNELS) },
                        onPlay = { event -> viewModel.markPlayed(event); onPlayChannel(event.channelId) },
                    )
                }
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        StatusToast(
            text = if (state.refreshing) t("football.refreshing") else null,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = PrimeDimens.screenMargin, bottom = 20.dp),
        )
    }
}

@Composable
private fun MatchList(
    events: List<MatchEvent>?,
    now: Long,
    listState: androidx.compose.foundation.lazy.LazyListState,
    firstFocus: FocusRequester,
    jumpFocus: FocusRequester,
    jumpKey: String?,
    liveOnly: Boolean,
    onRefresh: () -> Unit,
    onShowChannels: () -> Unit,
    onPlay: (MatchEvent) -> Unit,
) {
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    when {
        events == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingSpinner(size = 32.dp) }
        events.isEmpty() -> EmptyGuide(liveOnly, onRefresh, onShowChannels)
        else -> Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 6.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t("football.title"), style = typography.sectionTitle, color = colors.text)
                Text("· ${today.format(LONG_DAY)} · ${t("football.hint_ok")}", style = typography.cardBody, color = colors.textSecondary)
            }
            LaunchedEffect(events.firstOrNull()?.key, liveOnly) {
                if (jumpKey == null) firstFocus.tryRequestFocus()
            }
            LazyColumn(
                Modifier.fillMaxSize().focusRestorer().focusGroup().throttleHeldDpad(vertical = true) { listState.isScrollInProgress },
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp),
            ) {
                var previousDay: LocalDate? = null
                events.forEachIndexed { index, event ->
                    val day = Instant.ofEpochMilli(event.start).atZone(zone).toLocalDate()
                    if (day != previousDay && day != today) {
                        previousDay = day
                        item(key = "day-$day") {
                            Text(
                                if (day == today.plusDays(1)) t("football.tomorrow") else day.format(LONG_DAY),
                                style = typography.captionStrong, color = colors.textSecondary,
                                modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 2.dp),
                            )
                        }
                    } else if (previousDay == null) previousDay = day
                    item(key = event.key) {
                        val alt = event.altChannelIds.size
                        MatchRow(
                            event = event,
                            status = event.status(now),
                            liveLabel = t("common.live"),
                            finishedLabel = t("football.finished"),
                            dateLabel = if (day == today) t("football.today") else day.format(DAY_MONTH),
                            moreChannelsLabel = if (alt > 0) t("football.more_channels", "n" to alt) else null,
                            onClick = { onPlay(event) },
                            modifier = when {
                                event.key == jumpKey -> Modifier.focusRequester(jumpFocus)
                                index == 0 -> Modifier.focusRequester(firstFocus)
                                else -> Modifier
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGuide(liveOnly: Boolean, onRefresh: () -> Unit, onShowChannels: () -> Unit) {
    val colors = PrimeTheme.colors
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.tryRequestFocus() }
    Column(Modifier.fillMaxSize().padding(12.dp).focusGroup(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(t(if (liveOnly) "football.empty_live" else "football.empty"), style = PrimeTheme.typography.bodyLarge, color = colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimeButton(text = t("football.refresh"), onClick = onRefresh, modifier = Modifier.focusRequester(first))
            PrimeButton(text = t("football.see_channels"), onClick = onShowChannels, style = PrimeButtonStyle.Secondary)
        }
    }
}

@Composable
private fun ChannelList(state: FootballUiState, onPlayChannel: (String) -> Unit) {
    val colors = PrimeTheme.colors
    val channels = state.channels
    val listState = rememberLazyListState()
    val first = remember { FocusRequester() }
    LaunchedEffect(channels?.firstOrNull()?.id) { if (!channels.isNullOrEmpty()) first.tryRequestFocus() }
    when {
        channels == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingSpinner(size = 32.dp) }
        channels.isEmpty() -> Text(t("live.empty"), style = PrimeTheme.typography.bodyLarge, color = colors.textSecondary, modifier = Modifier.padding(12.dp))
        else -> LazyColumn(
            Modifier.fillMaxSize().focusRestorer().focusGroup().throttleHeldDpad(vertical = true) { listState.isScrollInProgress },
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
                    timeRange = now?.let { "${formatTime(it.start)} – ${formatTime(it.end)}" },
                    progress = now?.progressAt(state.now) ?: 0f,
                    onClick = { onPlayChannel(channel.id) },
                    qualityLabel = channel.qualityLabel,
                    modifier = if (channel.id == channels.first().id) Modifier.focusRequester(first) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun VideoRows(rows: List<CategoryRow>, onOpenItem: (MediaItem) -> Unit) {
    val colors = PrimeTheme.colors
    if (rows.isEmpty()) {
        Text(t("football.no_videos"), style = PrimeTheme.typography.bodyLarge, color = colors.textSecondary, modifier = Modifier.padding(12.dp))
        return
    }
    LazyColumn(Modifier.fillMaxSize().focusRestorer().focusGroup(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        items(rows, key = { it.id }) { row ->
            Column {
                Text(row.title, style = PrimeTheme.typography.sectionTitle, color = colors.text, modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
                val rowState = rememberLazyListState()
                LazyRow(
                    state = rowState,
                    modifier = Modifier.fillMaxWidth().focusRestorer().focusGroup().throttleHeldDpad(vertical = false) { rowState.isScrollInProgress },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(PrimeDimens.rowGap),
                ) {
                    items(row.items, key = { it.id }) { item -> PosterCard(item = item, onClick = { onOpenItem(item) }) }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(HOUR_MINUTE)

private const val TAB_FOCUS_DELAY_MS = 700L
