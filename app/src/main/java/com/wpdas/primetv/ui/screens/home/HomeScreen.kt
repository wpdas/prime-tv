package com.wpdas.primetv.ui.screens.home

import androidx.compose.runtime.mutableStateOf
import com.wpdas.primetv.ui.components.throttleHeldDpad
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
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
import com.wpdas.primetv.ui.components.tryRequestFocus
import com.wpdas.primetv.ui.components.TopBar
import com.wpdas.primetv.ui.components.WideCard
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeTheme

@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenLive: () -> Unit,
    onOpenFootball: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onPlay: (String, String?) -> Unit,
    onOpenAppMenu: () -> Unit,
    onOpenSettings: () -> Unit,
    onSeeAll: (CategoryRow) -> Unit,
    onOpenUpdate: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Versão nova detectada: abre o diálogo de atualização uma vez por sessão.
    LaunchedEffect(state.updateVersion) {
        if (state.updateVersion != null && !viewModel.updatePromptShown) {
            viewModel.updatePromptShown = true
            onOpenUpdate()
        }
    }
    HomeContent(
        state = state,
        viewModel = viewModel,
        onTabSelected = { tab ->
            when (tab) {
                HomeTab.LIVE -> onOpenLive()
                HomeTab.FOOTBALL -> onOpenFootball()
                else -> viewModel.onTabSelected(tab)
            }
        },
        onTopBarFocused = viewModel::onTopBarFocused,
        onCardFocused = viewModel::onCardFocused,
        onWatch = { onPlay(it.id, null) },
        onContinue = { onPlay(it.item.id, it.episodeId) },
        onMoreInfo = { if (it.type == MediaType.CHANNEL) onPlay(it.id, null) else onOpenDetails(it.id) },
        onPlayChannel = { onPlay(it, null) },
        onSeeAll = onSeeAll,
        onToggleMyList = viewModel::toggleMyList,
        onOpenSearch = onOpenSearch,
        onOpenAppMenu = onOpenAppMenu,
        onOpenSettings = onOpenSettings,
        onOpenLive = onOpenLive,
        onOpenFootball = onOpenFootball,
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    viewModel: HomeViewModel? = null,
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
    onOpenFootball: () -> Unit = {},
    onPlayChannel: (String) -> Unit = {},
    onSeeAll: (CategoryRow) -> Unit = {},
) {
    val colors = PrimeTheme.colors
    val watchFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Um FocusRequester por linha e um para o card que estava em foco, para voltar exatamente lá.
    val rowRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val rowStates = remember { mutableMapOf<String, androidx.compose.foundation.lazy.LazyListState>() }
    fun rowRequester(key: String) = rowRequesters.getOrPut(key) { FocusRequester() }
    val restoreItemId = viewModel?.lastFocusedItemId
    val restoreCardRequester = remember { FocusRequester() }
    /** Primeiro card de uma linha: ao ganhar foco, realinha a linha à margem (o scroll por foco deixa-o cortado). */
    fun cardModifier(rowKey: String, itemId: String, index: Int): Modifier {
        var m: Modifier = Modifier
        if (itemId == restoreItemId) m = m.focusRequester(restoreCardRequester)
        if (index == 0) m = m.onFocusChanged { f ->
            // O scroll automático do foco roda depois deste evento; espera-o terminar e então alinha à margem.
            if (f.isFocused) scope.launch {
                val rs = rowStates[rowKey] ?: return@launch
                kotlinx.coroutines.delay(120)
                androidx.compose.runtime.snapshotFlow { rs.isScrollInProgress }.first { !it }
                if (rs.firstVisibleItemIndex == 0 && rs.firstVisibleItemScrollOffset > 0) rs.animateScrollToItem(0)
            }
        }
        return m
    }

    // Voltar numa aba que não é Início leva para Início. Em Início: avisa e, no segundo toque, fecha o app.
    BackHandler(enabled = state.selectedTab != HomeTab.HOME) { onTabSelected(HomeTab.HOME) }
    val activity = androidx.activity.compose.LocalActivity.current
    var exitArmed by remember { mutableStateOf(false) }
    var exitJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    BackHandler(enabled = state.selectedTab == HomeTab.HOME) {
        if (exitArmed) {
            // Fecha de verdade: encerra a tarefa e, logo depois, o processo (sem isso a box mantém o app
            // em memória e a próxima abertura não é um início limpo).
            activity?.finishAndRemoveTask()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ android.os.Process.killProcess(android.os.Process.myPid()) }, EXIT_KILL_DELAY_MS)
        } else {
            exitArmed = true
            exitJob?.cancel()
            exitJob = scope.launch { kotlinx.coroutines.delay(EXIT_CONFIRM_MS); exitArmed = false }
        }
    }

    // Primeira abertura: foco no "Assistir" do destaque. Ao voltar de outra tela: a linha onde o usuário estava.
    LaunchedEffect(state.hero != null) {
        if (state.hero == null) return@LaunchedEffect
        val row = viewModel?.lastFocusedRow
        if (viewModel?.initialFocusDone != true || row == null || row == "hero") {
            // Primeira abertura: o destaque pode ter entrado acima do que já estava visível; volta ao topo.
            listState.scrollToItem(0)
            repeat(15) {
                if (watchFocus.tryRequestFocus()) { viewModel?.initialFocusDone = true; return@LaunchedEffect }
                kotlinx.coroutines.delay(50)
            }
            return@LaunchedEffect
        }
        // Voltando de outra tela: rola a linha até o card que estava em foco e foca nele; senão, a linha.
        val itemId = viewModel.lastFocusedItemId
        val index = when (row) {
            "continue" -> state.continueWatching.indexOfFirst { it.item.id == itemId }
            else -> state.rows.firstOrNull { it.id == row }?.items?.indexOfFirst { it.id == itemId } ?: -1
        }
        if (itemId != null && index >= 0) {
            repeat(15) {
                rowStates[row]?.let { rs -> runCatching { rs.scrollToItem(index) } }
                kotlinx.coroutines.delay(50) // o card entra na composição no quadro seguinte ao scroll
                if (restoreCardRequester.tryRequestFocus()) return@LaunchedEffect
            }
        }
        repeat(10) {
            if (rowRequesters[row]?.tryRequestFocus() == true) return@LaunchedEffect
            kotlinx.coroutines.delay(50)
        }
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            // Aba muda ao ficar em foco por um instante (passar por ela a caminho de outra não dispara).
            var tabFocusJob by remember { androidx.compose.runtime.mutableStateOf<kotlinx.coroutines.Job?>(null) }
            TopBar(
                selectedTab = state.selectedTab,
                tabs = state.tabs,
                onTabSelected = onTabSelected,
                onTabFocused = { tab ->
                    tabFocusJob?.cancel()
                    // Ao vivo troca de tela, então espera mais para não disparar a caminho de Filmes/Séries.
                    val opensScreen = tab == HomeTab.LIVE || tab == HomeTab.FOOTBALL
                    if (tab != state.selectedTab) tabFocusJob = scope.launch { kotlinx.coroutines.delay(if (opensScreen) LIVE_TAB_FOCUS_DELAY_MS else TAB_FOCUS_DELAY_MS); onTabSelected(tab) }
                },
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
                modifier = Modifier.fillMaxSize().throttleHeldDpad(vertical = true) { listState.isScrollInProgress },
                contentPadding = PaddingValues(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                            modifier = Modifier.padding(horizontal = PrimeDimens.screenMargin).onFocusChanged { if (it.hasFocus) viewModel?.lastFocusedRow = "hero" },
                        )
                    }
                }
                if (state.continueWatching.isNotEmpty() && state.selectedTab in setOf(HomeTab.HOME, HomeTab.MOVIES, HomeTab.SERIES)) {
                    item(key = "continue") {
                        HomeRow(title = t("home.continue_watching"), focusRequester = rowRequester("continue"), onFocused = { viewModel?.lastFocusedRow = "continue" }, rowState = rowStates.getOrPut("continue") { androidx.compose.foundation.lazy.LazyListState() }) {
                            itemsIndexed(state.continueWatching, key = { _, it -> it.item.id }) { index, cw ->
                                WideCard(
                                    item = cw.item,
                                    meta = continueMeta(cw),
                                    progress = cw.progress,
                                    onClick = { onContinue(cw) },
                                    onFocused = onCardFocused,
                                    modifier = cardModifier("continue", cw.item.id, index),
                                )
                            }
                        }
                    }
                }
                if (state.football.isNotEmpty() && state.selectedTab == HomeTab.HOME) {
                    item(key = "football") {
                        HomeRow(title = t("home.football_now"), hint = t("home.football_now_hint"), focusRequester = rowRequester("football"), onFocused = { viewModel?.lastFocusedRow = "football" }) {
                            items(state.football, key = { it.id }) { event ->
                                FootballCard(event = event, liveLabel = t("common.live"), onClick = { onPlayChannel(event.channelId) })
                            }
                            item(key = "football-guide") { SeeAllCard(label = t("football.see_guide"), onClick = onOpenFootball, modifier = Modifier.height(92.dp)) }
                        }
                    }
                }
                items(state.rows, key = { it.id }) { row ->
                    HomeRow(title = row.title, focusRequester = rowRequester(row.id), onFocused = { viewModel?.lastFocusedRow = row.id }, rowState = rowStates.getOrPut(row.id) { androidx.compose.foundation.lazy.LazyListState() }) {
                        itemsIndexed(row.items, key = { _, it -> it.id }) { index, item ->
                            PosterCard(item = item, onClick = { onMoreInfo(item) }, onFocused = onCardFocused, modifier = cardModifier(row.id, item.id, index))
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
            text = when {
                exitArmed -> t("home.press_back_again")
                else -> state.updatingSectionKey?.let { t("home.updating_section", "section" to t(it)) }
            },
            showSpinner = !exitArmed,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = PrimeDimens.screenMargin, bottom = 20.dp),
        )
    }
}

/** Linha com título e um LazyRow que lembra o item focado ao voltar para ela. */
@Composable
private fun HomeRow(
    title: String,
    hint: String? = null,
    focusRequester: FocusRequester? = null,
    onFocused: (() -> Unit)? = null,
    rowState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val colors = PrimeTheme.colors
    val rowScope = rememberCoroutineScope()
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
            state = rowState,
            modifier = Modifier
                .fillMaxWidth()
                .throttleHeldDpad(vertical = false) { rowState.isScrollInProgress }
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focus ->
                    if (!focus.hasFocus) return@onFocusChanged
                    onFocused?.invoke()
                    // Voltando ao início da linha, o primeiro card tem de ficar alinhado à margem.
                    if (rowState.firstVisibleItemIndex == 0 && rowState.firstVisibleItemScrollOffset > 0) {
                        rowScope.launch { rowState.animateScrollToItem(0) }
                    }
                }
                .focusRestorer()
                .focusGroup(),
            // Espaço para o anel de foco e a escala de 1,05 não serem cortados pela linha.
            contentPadding = PaddingValues(horizontal = PrimeDimens.screenMargin, vertical = 14.dp),
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

private const val TAB_FOCUS_DELAY_MS = 350L
private const val EXIT_CONFIRM_MS = 2_500L
private const val EXIT_KILL_DELAY_MS = 300L
private const val LIVE_TAB_FOCUS_DELAY_MS = 700L
