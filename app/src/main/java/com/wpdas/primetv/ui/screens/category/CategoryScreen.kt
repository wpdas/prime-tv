package com.wpdas.primetv.ui.screens.category

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.ui.components.BackLink
import com.wpdas.primetv.ui.components.LoadingSpinner
import com.wpdas.primetv.ui.components.PosterCard
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeTheme

/** Grade completa de uma categoria (7 colunas de pôsteres). */
@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    onOpenItem: (MediaItem) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = PrimeTheme.colors
    val firstFocus = remember { FocusRequester() }
    val items = state.items
    LaunchedEffect(items?.isNotEmpty()) { if (items?.isNotEmpty() == true) runCatching { firstFocus.requestFocus() } }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.padding(horizontal = PrimeDimens.screenMargin).padding(top = 28.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BackLink(onClick = onBack)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(state.title, style = PrimeTheme.typography.screenTitle, color = colors.text)
                if (items != null) Text(t("category.count", "n" to items.size), style = PrimeTheme.typography.body, color = colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
        when {
            items == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingSpinner(size = 32.dp) }
            items.isEmpty() -> Text(t("category.empty"), style = PrimeTheme.typography.bodyLarge, color = colors.textSecondary, modifier = Modifier.padding(horizontal = PrimeDimens.screenMargin))
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxSize().focusRestorer().focusGroup(),
                contentPadding = PaddingValues(start = PrimeDimens.screenMargin, end = PrimeDimens.screenMargin, top = 8.dp, bottom = 32.dp),
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
