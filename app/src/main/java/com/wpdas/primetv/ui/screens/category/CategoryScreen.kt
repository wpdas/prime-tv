package com.wpdas.primetv.ui.screens.category

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.ui.components.BackLink
import com.wpdas.primetv.ui.components.LoadingSpinner
import com.wpdas.primetv.ui.components.PosterCard
import com.wpdas.primetv.ui.components.PrimeTextField
import com.wpdas.primetv.ui.components.SelectableChip
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeTheme
import kotlinx.coroutines.launch

/** Grade completa de uma categoria, com busca dentro dela, ordenação e gêneros (quando a lista traz). */
@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    onOpenItem: (MediaItem) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    val firstFocus = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val items = state.items
    LaunchedEffect(items?.isNotEmpty()) { if (items?.isNotEmpty() == true) runCatching { firstFocus.requestFocus() } }

    Row(
        Modifier.fillMaxSize().background(colors.background).padding(start = PrimeDimens.screenMargin - 6.dp, end = PrimeDimens.screenMargin, top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // ---------------- trilho: voltar, título, busca, ordenação, gêneros ----------------
        Column(
            Modifier.width(272.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 24.dp).focusGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BackLink(onClick = onBack)
            Text(state.title, style = typography.title, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                if (items != null && items.size != state.total) t("category.count_filtered", "n" to items.size, "total" to state.total) else t("category.count", "n" to state.total),
                style = typography.caption, color = colors.textSecondary,
            )
            PrimeTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = t("category.search_here"),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
            Text(t("category.sort"), style = typography.captionStrong, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CategorySort.entries.filter { !(state.isLive && it == CategorySort.YEAR) }.forEach { option ->
                    SelectableChip(text = t(option.labelKey), selected = option == state.sort, onClick = { viewModel.setSort(option) })
                }
            }
            if (state.genres.isNotEmpty()) {
                Text(t("category.genres"), style = typography.captionStrong, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.genres.forEach { g ->
                        SelectableChip(text = g, selected = g == state.genre, onClick = { viewModel.setGenre(g) })
                    }
                }
            } else if (!state.isLive) {
                Text(t("category.no_genres"), style = typography.caption, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
            }
        }

        // ---------------- grade ----------------
        when {
            items == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingSpinner(size = 32.dp) }
            items.isEmpty() -> Text(
                if (state.query.isNotBlank() || state.genre != null) t("category.empty_filtered") else t("category.empty"),
                style = typography.bodyLarge, color = colors.textSecondary, modifier = Modifier.padding(top = 60.dp),
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged { focus ->
                        // Ao voltar para a primeira linha, ela fica alinhada ao topo (sem cortar o anel de foco).
                        if (focus.hasFocus && gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset > 0) {
                            scope.launch { gridState.animateScrollToItem(0) }
                        }
                    }
                    .focusRestorer()
                    .focusGroup(),
                // Folga para o anel de foco e a escala nas bordas da grade.
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 56.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(PrimeDimens.rowGap),
                verticalArrangement = Arrangement.spacedBy(PrimeDimens.rowGap),
            ) {
                items(items, key = { it.id }) { item ->
                    PosterCard(
                        item = item,
                        onClick = { onOpenItem(item) },
                        modifier = if (item.id == items.first().id) Modifier.focusRequester(firstFocus) else Modifier,
                    )
                }
            }
        }
    }
}
