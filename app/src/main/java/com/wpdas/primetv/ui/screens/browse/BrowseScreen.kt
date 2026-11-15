package com.wpdas.primetv.ui.screens.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.tv.material3.Text
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.data.repository.toMediaItem
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.HomeTab
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.repository.CatalogRepository
import com.wpdas.primetv.domain.repository.HomeRepository
import com.wpdas.primetv.ui.components.CategoryRail
import com.wpdas.primetv.ui.components.DoubleBackToExit
import com.wpdas.primetv.ui.components.LoadingSpinner
import com.wpdas.primetv.ui.components.PosterCard
import com.wpdas.primetv.ui.components.RailCategory
import com.wpdas.primetv.ui.components.TopBar
import com.wpdas.primetv.ui.components.throttleHeldDpad
import com.wpdas.primetv.ui.components.tryRequestFocus
import com.wpdas.primetv.ui.navigation.BrowseRoute
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowseUiState(
    val tab: HomeTab = HomeTab.MOVIES,
    val tabs: List<HomeTab> = emptyList(),
    val rail: List<RailCategory> = emptyList(),
    val selectedId: String? = null,
    /** null enquanto carrega. */
    val items: List<MediaItem>? = null,
)

/** Modo simples: Filmes ou Séries com as categorias e a ordem da lista, sem juntar versões. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BrowseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    catalog: CatalogRepository,
    home: HomeRepository,
) : ViewModel() {
    private val section = if (savedStateHandle.toRoute<BrowseRoute>().section == ContentSection.SERIES.name) ContentSection.SERIES else ContentSection.MOVIES
    private val _state = MutableStateFlow(BrowseUiState(tab = if (section == ContentSection.SERIES) HomeTab.SERIES else HomeTab.MOVIES))
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()
    private val selected = MutableStateFlow<String?>(null)

    init {
        home.availableTabs().onEach { tabs -> _state.update { it.copy(tabs = tabs) } }.launchIn(viewModelScope)
        combine(catalog.categories(section), catalog.categoryCounts(section)) { categories, counts ->
            val byId = counts.associate { it.categoryId to it.count }
            categories.map { RailCategory(it.id, it.name, byId[it.id] ?: 0) }
        }.onEach { rail ->
            _state.update { it.copy(rail = rail) }
            if (rail.isNotEmpty() && rail.none { it.id == selected.value }) select(rail.first().id)
        }.launchIn(viewModelScope)
        selected.filterNotNull()
            .flatMapLatest { id ->
                if (section == ContentSection.SERIES) catalog.seriesInListOrder(id).map { l -> l.map { it.toMediaItem() } }
                else catalog.moviesInListOrder(id).map { l -> l.map { it.toMediaItem() } }
            }
            .onEach { items -> _state.update { it.copy(items = items) } }
            .launchIn(viewModelScope)
    }

    fun select(id: String) {
        if (selected.value == id) return
        selected.value = id
        _state.update { it.copy(selectedId = id, items = null) }
    }
}

@Composable
fun BrowseScreen(
    onTabSelected: (HomeTab) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAppMenu: () -> Unit,
    onOpenItem: (MediaItem) -> Unit,
    isRoot: Boolean = false,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = PrimeTheme.colors
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val tabFocus = remember { FocusRequester() }
    var tabFocusJob by remember { mutableStateOf<Job?>(null) }
    // Chegando pela barra, o foco fica na aba; a grade só recebe foco quando o usuário desce.
    LaunchedEffect(Unit) {
        repeat(10) {
            if (tabFocus.tryRequestFocus()) return@LaunchedEffect
            delay(50)
        }
    }
    LaunchedEffect(state.selectedId) { if (state.items != null) gridState.scrollToItem(0) }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(colors.background)) {
        TopBar(
            selectedTab = state.tab,
            tabs = state.tabs,
            onTabSelected = onTabSelected,
            selectedTabFocus = tabFocus,
            onTabFocused = { tab ->
                tabFocusJob?.cancel()
                if (tab != null && tab != state.tab) tabFocusJob = scope.launch { delay(TAB_FOCUS_DELAY_MS); onTabSelected(tab) }
            },
            onSearchClick = onOpenSearch,
            onMenuClick = onOpenAppMenu,
        )
        Row(
            Modifier.fillMaxSize().padding(start = PrimeDimens.screenMargin, end = PrimeDimens.screenMargin, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CategoryRail(
                categories = state.rail,
                selectedId = state.selectedId,
                onSelect = { viewModel.select(it.id) },
                modifier = Modifier.fillMaxHeight().padding(bottom = 32.dp),
            )
            val items = state.items
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    items == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingSpinner(size = 32.dp) }
                    items.isEmpty() -> Text(t("browse.empty"), style = PrimeTheme.typography.bodyLarge, color = colors.textSecondary, modifier = Modifier.padding(12.dp))
                    // Grade preguiçosa: só as capas visíveis (e a linha seguinte) são carregadas.
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .throttleHeldDpad(vertical = true) { gridState.isScrollInProgress }
                            .onFocusChanged { focus ->
                                if (focus.hasFocus && gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset > 0) {
                                    scope.launch { gridState.animateScrollToItem(0) }
                                }
                            }
                            .focusRestorer()
                            .focusGroup(),
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(PrimeDimens.rowGap),
                        verticalArrangement = Arrangement.spacedBy(PrimeDimens.rowGap),
                    ) {
                        items(items, key = { it.id }) { item -> PosterCard(item = item, onClick = { onOpenItem(item) }) }
                    }
                }
            }
        }
    }
    DoubleBackToExit(enabled = isRoot)
    }
}

private const val TAB_FOCUS_DELAY_MS = 350L
