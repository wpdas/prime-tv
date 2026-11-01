package com.wpdas.primetv.ui.screens.search

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.core.util.TextNormalizer
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.model.MediaType
import com.wpdas.primetv.ui.components.BackLink
import com.wpdas.primetv.ui.components.OnScreenKeyboard
import com.wpdas.primetv.ui.components.PosterArtwork
import com.wpdas.primetv.ui.components.TagChip
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenItem: (MediaItem) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    val context = LocalContext.current
    val firstKey = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstKey.requestFocus() } }

    val voiceIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
    }
    val hasVoice = remember { voiceIntent.resolveActivity(context.packageManager) != null }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { viewModel.setQuery(it) }
        }
    }

    Row(
        Modifier.fillMaxSize().background(colors.background).padding(horizontal = PrimeDimens.screenMargin, vertical = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        // ---------------- esquerda: campo + teclado ----------------
        Column(Modifier.width(380.dp).fillMaxHeight().focusGroup(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BackLink(onClick = onBack)
            Text(t("search.label"), style = typography.caption, color = colors.textSecondary)
            Row(
                Modifier.fillMaxWidth().height(52.dp).background(colors.selected, PrimeTheme.shapes.card).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                Text(
                    state.query.ifEmpty { t("search.placeholder") },
                    style = typography.subtitle.copy(fontWeight = FontWeight.Normal),
                    color = if (state.query.isEmpty()) colors.textSecondary else colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OnScreenKeyboard(
                onKey = viewModel::append,
                onSpace = viewModel::space,
                onBackspace = viewModel::backspace,
                onClear = viewModel::clear,
                spaceLabel = t("search.space"),
                backspaceLabel = t("search.backspace"),
                clearLabel = t("search.clear"),
                firstKeyFocus = firstKey,
                extraKeys = if (hasVoice) listOf(t("search.voice") to { voiceLauncher.launch(voiceIntent) }) else emptyList(),
            )
            Text(t("search.hint"), style = typography.caption, color = colors.textSecondary)
        }

        // ---------------- direita: sugestões + resultados ----------------
        Column(Modifier.weight(1f).fillMaxHeight().padding(top = 48.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            when {
                state.query.isBlank() -> Text(t("search.start"), style = typography.bodyLarge, color = colors.textSecondary)
                state.searched && state.results.isEmpty() -> Text(t("search.empty", "q" to state.query), style = typography.bodyLarge, color = colors.textSecondary)
                state.suggestions.isNotEmpty() -> Column(
                    Modifier.fillMaxWidth().background(colors.surface, PrimeTheme.shapes.panel).padding(8.dp).focusRestorer().focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    state.suggestions.forEach { item -> SuggestionRow(item = item, query = state.query, onClick = { onOpenItem(item) }) }
                }
            }
            if (state.results.isNotEmpty()) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(t("search.results"), style = typography.sectionTitle, color = colors.text)
                    Text("· ${state.results.size}", style = typography.sectionTitle.copy(fontWeight = FontWeight.Medium), color = colors.textSecondary)
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxSize().focusRestorer().focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(PrimeDimens.rowGap),
                    verticalArrangement = Arrangement.spacedBy(PrimeDimens.rowGap),
                    contentPadding = PaddingValues(6.dp),
                ) {
                    items(state.results, key = { it.id }) { item -> ResultCard(item = item, onClick = { onOpenItem(item) }) }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(item: MediaItem, query: String, onClick: () -> Unit) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.card
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = colors.text,
            focusedContainerColor = colors.selected,
            focusedContentColor = colors.text,
            pressedContainerColor = colors.selected,
            pressedContentColor = colors.text,
        ),
        scale = PrimeFocus.noScale(),
        border = PrimeFocus.border(shape),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(highlight(item.title, query, colors.text, colors.textSecondary), style = PrimeTheme.typography.tab.copy(fontWeight = FontWeight.Normal), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            TagChip(typeLabel(item.type), modifier = Modifier.padding(start = 12.dp))
        }
    }
}

/** Card de resultado: arte com título e tipo por cima, 200 de altura. */
@Composable
private fun ResultCard(item: MediaItem, onClick: () -> Unit) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.card
    Surface(
        onClick = onClick,
        modifier = Modifier.height(200.dp),
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(containerColor = colors.surface, contentColor = colors.text, focusedContainerColor = colors.surface, focusedContentColor = colors.text, pressedContainerColor = colors.surface, pressedContentColor = colors.text),
        scale = PrimeFocus.scale(),
        border = PrimeFocus.border(shape),
    ) {
        PosterArtwork(item, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to colors.background.copy(alpha = 0.9f))))
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, style = PrimeTheme.typography.cardTitle, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(typeLabel(item.type), style = PrimeTheme.typography.captionSmall, color = colors.textBody)
        }
    }
}

/** Destaca o trecho digitado dentro do título (comparação sem acento/caixa, mesmo comprimento). */
private fun highlight(title: String, query: String, strong: Color, weak: Color) = buildAnnotatedString {
    val hay = TextNormalizer.stripAccents(title.lowercase())
    val needle = TextNormalizer.stripAccents(query.lowercase().trim())
    val start = if (hay.length == title.length && needle.isNotEmpty()) hay.indexOf(needle) else -1
    if (start < 0) {
        withStyle(SpanStyle(color = strong)) { append(title) }
    } else {
        withStyle(SpanStyle(color = weak)) { append(title.substring(0, start)) }
        withStyle(SpanStyle(color = strong, fontWeight = FontWeight.Bold)) { append(title.substring(start, start + needle.length)) }
        withStyle(SpanStyle(color = weak)) { append(title.substring(start + needle.length)) }
    }
}

@Composable
private fun typeLabel(type: MediaType): String = when (type) {
    MediaType.MOVIE -> t("meta.movie")
    MediaType.SERIES -> t("meta.series")
    MediaType.CHANNEL -> t("meta.channel")
}

