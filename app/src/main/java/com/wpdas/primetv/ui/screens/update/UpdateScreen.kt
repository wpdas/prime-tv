package com.wpdas.primetv.ui.screens.update

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.wpdas.primetv.BuildConfig
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.ui.components.LoadingSpinner
import com.wpdas.primetv.ui.components.Logo
import com.wpdas.primetv.ui.components.PrimeButton
import com.wpdas.primetv.ui.components.PrimeButtonStyle
import com.wpdas.primetv.ui.components.TagChip
import com.wpdas.primetv.ui.components.ThinProgressBar
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeTheme
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Diálogo de atualização sobre a tela escurecida: verificando, nova versão, baixando, atualizado, erro. */
@Composable
fun UpdateScreen(onClose: () -> Unit, viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    val primaryFocus = remember { FocusRequester() }
    LaunchedEffect(state::class) { runCatching { primaryFocus.requestFocus() } }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.onResumed() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize().background(colors.background.copy(alpha = 0.94f))) {
        Logo(modifier = Modifier.padding(start = PrimeDimens.screenMargin, top = 18.dp))
        Column(
            Modifier
                .align(Alignment.Center)
                .width(640.dp)
                .heightIn(max = 560.dp)
                .background(colors.surface, PrimeTheme.shapes.panelLarge)
                .padding(28.dp)
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val s = state) {
                UpdateUiState.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        LoadingSpinner()
                        Text(t("update.checking"), style = typography.sectionTitle, color = colors.text)
                    }
                    Text(t("update.checking_desc", "version" to BuildConfig.VERSION_NAME), style = typography.body, color = colors.textSecondary)
                    PrimeButton(text = t("common.cancel"), onClick = onClose, style = PrimeButtonStyle.Secondary, height = 44.dp, horizontalPadding = 18.dp, textStyle = typography.buttonSmall, modifier = Modifier.focusRequester(primaryFocus))
                }
                is UpdateUiState.Available -> {
                    TagChip(t("update.available"))
                    Text("PrimeTV ${s.update.versionName}", style = typography.dialogTitle, color = colors.text)
                    Text(
                        listOfNotNull(
                            t("update.you_have", "version" to BuildConfig.VERSION_NAME),
                            s.update.apkSizeBytes.takeIf { it > 0 }?.let { t("update.apk_size", "size" to formatSize(it)) },
                            s.update.publishedAt?.let { t("update.published", "when" to relativeDays(it)) },
                        ).joinToString(" · "),
                        style = typography.cardBody, color = colors.textSecondary,
                    )
                    if (s.update.changelog.isNotBlank()) {
                        ChangelogBox(s.update.changelog, modifier = Modifier.weight(1f, fill = false))
                    }
                    if (s.needsPermission) {
                        Text(t("update.permission_needed"), style = typography.body, color = colors.accentSoftText)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PrimeButton(text = t("update.open_permission"), onClick = viewModel::openPermission, modifier = Modifier.focusRequester(primaryFocus))
                            PrimeButton(text = t("update.not_now"), onClick = onClose, style = PrimeButtonStyle.Secondary, horizontalPadding = 20.dp, textStyle = typography.listTitle)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                            PrimeButton(text = t("update.download_install"), onClick = viewModel::downloadAndInstall, modifier = Modifier.focusRequester(primaryFocus))
                            PrimeButton(text = t("update.not_now"), onClick = onClose, style = PrimeButtonStyle.Secondary, horizontalPadding = 20.dp, textStyle = typography.listTitle)
                        }
                    }
                }
                is UpdateUiState.Downloading -> {
                    Text(t("update.downloading", "version" to s.update.versionName), style = typography.title, color = colors.text)
                    ThinProgressBar(progress = s.fraction, height = 8.dp, trackColor = colors.buttonSecondary, rounded = true)
                    Text(
                        if (s.total > 0) t("update.progress", "done" to formatSize(s.downloaded), "total" to formatSize(s.total)) else formatSize(s.downloaded),
                        style = typography.body, color = colors.textSecondary,
                    )
                    PrimeButton(text = t("common.cancel"), onClick = viewModel::cancelDownload, style = PrimeButtonStyle.Secondary, height = 44.dp, horizontalPadding = 18.dp, textStyle = typography.buttonSmall, modifier = Modifier.focusRequester(primaryFocus))
                }
                is UpdateUiState.ReadyToInstall -> {
                    Text(t("update.ready", "version" to s.update.versionName), style = typography.title, color = colors.text)
                    Text(t("update.ready_desc"), style = typography.body, color = colors.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimeButton(text = t("update.install"), onClick = viewModel::installAgain, modifier = Modifier.focusRequester(primaryFocus))
                        PrimeButton(text = t("common.close"), onClick = onClose, style = PrimeButtonStyle.Secondary, horizontalPadding = 18.dp, textStyle = typography.buttonSmall)
                    }
                }
                UpdateUiState.UpToDate -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.accent, modifier = Modifier.size(28.dp))
                        Text(t("update.up_to_date"), style = typography.subtitle, color = colors.text)
                    }
                    Text(t("update.up_to_date_desc", "version" to BuildConfig.VERSION_NAME), style = typography.body, color = colors.textSecondary)
                    PrimeButton(text = t("common.close"), onClick = onClose, style = PrimeButtonStyle.Secondary, height = 44.dp, horizontalPadding = 18.dp, textStyle = typography.buttonSmall, modifier = Modifier.focusRequester(primaryFocus))
                }
                is UpdateUiState.Error -> {
                    Text(t("update.error_title"), style = typography.subtitle, color = colors.text)
                    Text(t(s.messageKey, "detail" to (s.detail ?: "")), style = typography.body, color = colors.textBody)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimeButton(text = t("common.retry"), onClick = viewModel::check, modifier = Modifier.focusRequester(primaryFocus))
                        PrimeButton(text = t("common.close"), onClick = onClose, style = PrimeButtonStyle.Secondary, horizontalPadding = 18.dp, textStyle = typography.buttonSmall)
                    }
                }
            }
        }
    }
}

/**
 * Changelog da release com rolagem pelo D-pad: focável, cima/baixo rolam; os botões
 * continuam embaixo. Markdown simples: títulos "##" viram seções, "- " vira marcador.
 */
@Composable
private fun ChangelogBox(body: String, modifier: Modifier = Modifier) {
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var focused by remember { mutableStateOf(false) }
    val step = with(LocalDensity.current) { 72.dp.toPx() }
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.background.copy(alpha = 0.5f), PrimeTheme.shapes.card)
            .then(if (focused) Modifier.border(2.dp, colors.accent, PrimeTheme.shapes.card) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> if (scroll.value < scroll.maxValue) { scope.launch { scroll.animateScrollBy(step) }; true } else false
                    Key.DirectionUp -> if (scroll.value > 0) { scope.launch { scroll.animateScrollBy(-step) }; true } else false
                    else -> false
                }
            }
            .verticalScroll(scroll)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        changelogLines(body).forEach { (text, heading) ->
            Text(
                text,
                style = if (heading) typography.bodyStrong else typography.body,
                color = if (heading) colors.text else colors.textBody,
                modifier = if (heading) Modifier.padding(top = 6.dp) else Modifier,
            )
        }
    }
}

/** Markdown simples da release -> (linha, éTítulo). */
private fun changelogLines(body: String): List<Pair<String, Boolean>> = body.lines()
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .map { line ->
        val heading = line.startsWith("#")
        val text = when {
            heading -> line.trimStart('#').trim()
            line.startsWith("- ") || line.startsWith("* ") -> "• " + line.drop(2)
            else -> line
        }
        text.replace(Regex("""\*\*(.*?)\*\*"""), "$1").replace(Regex("""`(.*?)`"""), "$1") to heading
    }
    .take(60)

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format(Locale("pt", "BR"), "%.1f MB", bytes / 1_048_576.0)
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

@Composable
private fun relativeDays(epochMs: Long): String {
    val days = ChronoUnit.DAYS.between(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate(), java.time.LocalDate.now()).toInt()
    return when {
        days <= 0 -> t("time.today_plain")
        days == 1 -> t("time.yesterday_plain")
        else -> t("time.days_ago", "n" to days)
    }
}
