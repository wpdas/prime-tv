package com.wpdas.primetv.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.domain.model.CategoryRow
import com.wpdas.primetv.domain.model.ContinueWatchingItem
import com.wpdas.primetv.domain.model.HomeTab
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.model.MediaType
import com.wpdas.primetv.ui.components.FootballCard
import com.wpdas.primetv.ui.components.PosterCard
import com.wpdas.primetv.ui.components.SeeAllCard
import com.wpdas.primetv.ui.components.StatusToast
import com.wpdas.primetv.ui.components.TopBar
import com.wpdas.primetv.ui.components.WideCard
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeTheme

@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenLive: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onPlay: (String, String?) -> Unit,
    onOpenAppMenu: () -> Unit,
    onOpenSettings: () -> Unit,
    onSeeAll: (CategoryRow) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onTabSelected = { tab -> if (tab == HomeTab.LIVE) onOpenLive() else viewModel.onTabSelected(tab) },
        onTopBarFocused = viewModel::onTopBarFocused,
        onCardFocused = viewModel::onCardFocused,
        onWatch = { onPlay(it.id, null) },
        onContinue = { onPlay(it.item.id, it.episodeId) },
        onMoreInfo = { if (it.type == MediaType.CHANNEL) onPlay(it.id, null) else onOpenDetails(it.id) },
        onSeeAll = onSeeAll,
        onToggleMyList = viewModel::toggleMyList,
        onOpenSearch = onOpenSearch,
        onOpenAppMenu = onOpenAppMenu,
        onOpenSettings = onOpenSettings,
        onOpenLive = onOpenLive,
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onTabSelected: (HomeTab) -> Unit,
    onTopBarFocused: () -> Unit,
    onCardFocused: (MediaItem) -> Unit,
    onWatch: (MediaItem) -> Unit,
    onContinue: (ContinueWatchingItem) -> Unit,
    onMoreInfo: (MediaItem) -> Unit,
    onToggleMyList: (MediaItem) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAppMenu: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLive: () -> Unit,
    onSeeAll: (CategoryRow) -> Unit = {},
) {
    val colors = PrimeTheme.colors
    val watchFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Voltar numa aba que não é Início leva para Início; em Início, sai do app (padrão).
    BackHandler(enabled = state.selectedTab != HomeTab.HOME) { onTabSelected(HomeTab.HOME) }

    // Foco inicial no botão "Assistir" do destaque, como no protótipo.
    LaunchedEffect(state.hero != null) {
        if (state.hero != null) runCatching { watchFocus.requestFocus() }
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                selectedTab = state.selectedTab,
                onTabSelected = onTabSelected,
                onLogoClick = onOpenAppMenu,
                onSearchClick = onOpenSearch,
                onSettingsClick = onOpenSettings,
                onFocused = {
                    onTopBarFocused()
                    // Com o foco na barra, o destaque volta a aparecer inteiro.
                    if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
                        scope.launch { listState.animateScrollToItem(0) }
                    }
                },
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                state.hero?.let { hero ->
                    item(key = "hero") {
                        HeroSection(
                            item = hero,
                            dots = state.featured.size,
                            activeDot = state.featuredIndex,
                            showDots = state.showDots,
                            onWatch = onWatch,
                            onMoreInfo = onMoreInfo,
                            onToggleMyList = onToggleMyList,
                            watchFocusRequester = watchFocus,
                            modifier = Modifier.padding(horizontal = PrimeDimens.screenMargin),
                        )
                    }
                }
                if (state.continueWatching.isNotEmpty() && state.selectedTab in setOf(HomeTab.HOME, HomeTab.MOVIES, HomeTab.SERIES)) {
                    item(key = "continue") {
                        HomeRow(title = t("home.continue_watching")) {
                            items(state.continueWatching, key = { it.item.id }) { cw ->
                                WideCard(
                                    item = cw.item,
                                    meta = continueMeta(cw),
                                    progress = cw.progress,
                                    onClick = { onContinue(cw) },
                                    onFocused = onCardFocused,
                                )
                            }
                        }
                    }
                }
                if (state.football.isNotEmpty() && state.selectedTab in setOf(HomeTab.HOME, HomeTab.FOOTBALL)) {
                    item(key = "football") {
                        HomeRow(title = t("home.football_now"), hint = t("home.football_now_hint")) {
                            items(state.football, key = { it.id }) { event ->
                                FootballCard(event = event, liveLabel = t("common.live"), onClick = onOpenLive)
                            }
                        }
                    }
                }
                items(state.rows, key = { it.id }) { row ->
                    HomeRow(title = row.title) {
                        items(row.items, key = { it.id }) { item ->
                            PosterCard(item = item, onClick = { onMoreInfo(item) }, onFocused = onCardFocused)
                        }
                        if (row.section != null) {
                            item(key = "see-all-${row.id}") { SeeAllCard(label = t("home.see_all"), onClick = { onSeeAll(row) }) }
                        }
                    }
                }
                if (state.rows.isEmpty() && state.selectedTab == HomeTab.MY_LIST) {
                    item(key = "empty") {
                        Text(
                            t("home.empty_my_list"),
                            style = PrimeTheme.typography.bodyLarge,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = PrimeDimens.screenMargin),
                        )
                    }
                }
            }
        }
        StatusToast(
            text = state.updatingSectionKey?.let { t("home.updating_section", "section" to t(it)) }
                ?: state.updateVersion?.let { t("home.update_available", "version" to it) },
            showSpinner = state.updatingSectionKey != null,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = PrimeDimens.screenMargin, bottom = 20.dp),
        )
    }
}

/** Linha com título e um LazyRow que lembra o item focado ao voltar para ela. */
@Composable
private fun HomeRow(
    title: String,
    hint: String? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val colors = PrimeTheme.colors
    Column {
        Row(
            Modifier.padding(horizontal = PrimeDimens.screenMargin, vertical = 0.dp).padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = PrimeTheme.typography.sectionTitle, color = colors.text)
            if (hint != null) Text("· $hint", style = PrimeTheme.typography.cardBody, color = colors.textSecondary)
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth().focusRestorer().focusGroup(),
            contentPadding = PaddingValues(horizontal = PrimeDimens.screenMargin, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(PrimeDimens.rowGap),
            content = content,
        )
    }
}

@Composable
private fun continueMeta(cw: ContinueWatchingItem): String {
    val time = if (cw.remainingMinutes >= 60) {
        t("meta.hours_minutes_short", "h" to cw.remainingMinutes / 60, "m" to "%02d".format(cw.remainingMinutes % 60))
    } else {
        t("meta.minutes_short", "n" to cw.remainingMinutes)
    }
    val prefix = cw.episodeLabel ?: t("meta.movie")
    return "$prefix · ${t("meta.remaining", "time" to time)}"
}
