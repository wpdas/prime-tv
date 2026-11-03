package com.wpdas.primetv.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.PlaylistSource
import com.wpdas.primetv.domain.model.SyncState
import com.wpdas.primetv.ui.components.BackLink
import com.wpdas.primetv.ui.components.PrimeButton
import com.wpdas.primetv.ui.components.PrimeButtonStyle
import com.wpdas.primetv.ui.components.PrimeTextField
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onChangePlaylist: () -> Unit,
    onReceiveLink: () -> Unit,
    onAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Row(
        Modifier.fillMaxSize().background(colors.background).padding(horizontal = PrimeDimens.screenMargin, vertical = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Column(Modifier.width(300.dp).fillMaxHeight().focusGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BackLink(onClick = onBack)
            Text(t("screen.settings"), style = typography.screenTitle.copy(fontSize = androidx.compose.ui.unit.TextUnit(30f, androidx.compose.ui.unit.TextUnitType.Sp)), color = colors.text, modifier = Modifier.padding(top = 8.dp, bottom = 14.dp))
            SettingsSection.entries.forEachIndexed { index, section ->
                SectionButton(
                    label = t(section.labelKey),
                    selected = section == state.section,
                    onClick = { if (section == SettingsSection.ABOUT) onAbout() else viewModel.select(section) },
                    // Focar já abre a seção (Sobre é outra tela: continua exigindo OK).
                    modifier = (if (index == 0) Modifier.focusRequester(firstFocus) else Modifier)
                        .onFocusChanged { if (it.isFocused && section != SettingsSection.ABOUT) viewModel.select(section) },
                )
            }
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().padding(top = 82.dp).verticalScroll(rememberScrollState()).padding(6.dp).focusGroup(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (state.section) {
                SettingsSection.PLAYLIST -> PlaylistPane(state, onChangePlaylist, onReceiveLink, viewModel::syncAllNow)
                SettingsSection.UPDATES -> UpdatesPane(state, viewModel)
                SettingsSection.FOOTBALL -> FootballPane(state, viewModel)
                SettingsSection.PLAYER -> PlayerPane(state, viewModel)
                SettingsSection.LANGUAGE -> LanguagePane(state, viewModel)
                SettingsSection.ABOUT -> Unit
            }
        }
    }
}

@Composable
private fun SectionButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.card
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) colors.selected else Color.Transparent,
            contentColor = if (selected) colors.text else colors.textBody,
            focusedContainerColor = colors.selected,
            focusedContentColor = colors.text,
            pressedContainerColor = colors.selected,
            pressedContentColor = colors.text,
        ),
        scale = PrimeFocus.noScale(),
        border = PrimeFocus.border(shape),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
            Text(label, style = if (selected) PrimeTheme.typography.bodyStrong else PrimeTheme.typography.buttonSmall)
        }
    }
}

@Composable
private fun PaneTitle(title: String, description: String) {
    val colors = PrimeTheme.colors
    Text(title, style = PrimeTheme.typography.title, color = colors.text)
    Text(description, style = PrimeTheme.typography.body, color = colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
}

/** Linha "chave · valor" num cartão de 64 de altura, opcionalmente clicável. */
@Composable
private fun SettingRow(title: String, value: String?, onClick: (() -> Unit)? = null, subtitle: String? = null) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.panel
    val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = PrimeTheme.typography.tabSelected, color = colors.text)
                if (subtitle != null) Text(subtitle, style = PrimeTheme.typography.caption, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (value != null) {
                Text(
                    value,
                    style = PrimeTheme.typography.bodyStrong,
                    color = colors.text,
                    modifier = Modifier.padding(start = 12.dp).background(colors.surface2, PrimeTheme.shapes.card).padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
    if (onClick == null) {
        Box(Modifier.fillMaxWidth().background(colors.surface, shape)) { content() }
    } else {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = PrimeFocus.shape(shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = colors.surface, contentColor = colors.text,
                focusedContainerColor = colors.selected, focusedContentColor = colors.text,
                pressedContainerColor = colors.selected, pressedContentColor = colors.text,
            ),
            scale = PrimeFocus.noScale(),
            border = PrimeFocus.border(shape),
        ) { content() }
    }
}

@Composable
private fun PlaylistPane(state: SettingsUiState, onChangePlaylist: () -> Unit, onReceiveLink: () -> Unit, onSyncAll: () -> Unit) {
    PaneTitle(t("settings.playlist"), t("settings.playlist_desc"))
    val source = state.source
    val typeLabel = source?.let { t(it.type.labelKey) } ?: "—"
    val host = source?.let { hostOf(it) } ?: "—"
    SettingRow(t("settings.playlist_type"), typeLabel)
    SettingRow(t("settings.playlist_server"), host)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 6.dp)) {
        PrimeButton(text = t("settings.change_url"), onClick = onChangePlaylist, height = 46.dp, horizontalPadding = 20.dp, textStyle = PrimeTheme.typography.buttonMedium)
        PrimeButton(text = t("setup.receive_link"), onClick = onReceiveLink, style = PrimeButtonStyle.Secondary, height = 46.dp, horizontalPadding = 18.dp, textStyle = PrimeTheme.typography.buttonSmall)
        PrimeButton(text = t("settings.update_all"), onClick = onSyncAll, style = PrimeButtonStyle.Secondary, height = 46.dp, horizontalPadding = 18.dp, textStyle = PrimeTheme.typography.buttonSmall, enabled = !state.isSyncing)
    }
}

private fun hostOf(source: PlaylistSource): String = runCatching {
    val url = when (source) {
        is PlaylistSource.Xtream -> source.baseUrl
        is PlaylistSource.M3u -> source.url
    }
    java.net.URI(url).let { listOfNotNull(it.host, it.port.takeIf { p -> p > 0 }).joinToString(":") }
}.getOrDefault("—")

@Composable
private fun UpdatesPane(state: SettingsUiState, viewModel: SettingsViewModel) {
    PaneTitle(t("settings.updates"), t("settings.updates_desc"))
    listOf(ContentSection.LIVE, ContentSection.MOVIES, ContentSection.SERIES, ContentSection.EPG).forEach { section ->
        val interval = state.settings.interval(section)
        SettingRow(
            title = t(section.labelKey),
            subtitle = lastUpdateText(section, state.sync[section]),
            value = "${t("settings.every")} ${t(interval.labelKey)}",
            onClick = { viewModel.cycleInterval(section) },
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 6.dp)) {
        PrimeButton(text = t("settings.update_all"), onClick = viewModel::syncAllNow, height = 46.dp, horizontalPadding = 20.dp, textStyle = PrimeTheme.typography.buttonMedium, enabled = !state.isSyncing)
        PrimeButton(
            text = t(if (state.settings.unmeteredOnly) "settings.unmetered_on" else "settings.unmetered_off"),
            onClick = viewModel::toggleUnmetered,
            style = PrimeButtonStyle.Secondary,
            height = 46.dp,
            horizontalPadding = 18.dp,
            textStyle = PrimeTheme.typography.buttonSmall,
        )
    }
    Text(t("settings.interval_hint"), style = PrimeTheme.typography.caption, color = PrimeTheme.colors.textSecondary)
}

@Composable
private fun lastUpdateText(section: ContentSection, state: SyncState?): String = when (state) {
    is SyncState.Done -> {
        val count = when (section) {
            ContentSection.LIVE -> t("sync.done_live", "n" to state.count)
            ContentSection.MOVIES -> t("sync.done_movies", "n" to state.count)
            ContentSection.SERIES -> t("sync.done_series", "n" to state.count)
            ContentSection.EPG -> t("sync.done_epg")
        }
        "${t("settings.last_update", "when" to relativeTime(state.finishedAt))} · $count"
    }
    is SyncState.Running -> if (state.count == 0) t("sync.starting") else t("sync.downloading", "n" to state.count)
    is SyncState.Error -> t("settings.last_failed", "when" to relativeTime(state.at))
    is SyncState.Skipped -> t(state.reasonKey)
    else -> t("settings.never_updated")
}

/** "hoje 19:20", "ontem 08:05" ou "12/03 14:00". */
@Composable
private fun relativeTime(epochMs: Long): String {
    val zone = ZoneId.systemDefault()
    val dt = Instant.ofEpochMilli(epochMs).atZone(zone)
    val today = LocalDate.now(zone)
    val time = dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (dt.toLocalDate()) {
        today -> t("time.today", "time" to time)
        today.minusDays(1) -> t("time.yesterday", "time" to time)
        else -> dt.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FootballPane(state: SettingsUiState, viewModel: SettingsViewModel) {
    val colors = PrimeTheme.colors
    PaneTitle(t("settings.football"), t("settings.football_desc"))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        state.settings.footballKeywords.forEach { keyword ->
            KeywordChip(keyword) { viewModel.removeKeyword(keyword) }
        }
        if (!state.addingKeyword) {
            Surface(
                onClick = viewModel::startAddKeyword,
                modifier = Modifier.height(38.dp),
                shape = PrimeFocus.shape(PrimeTheme.shapes.pill),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent, contentColor = colors.textBody,
                    focusedContainerColor = colors.selected, focusedContentColor = colors.text,
                    pressedContainerColor = colors.selected, pressedContentColor = colors.text,
                ),
                scale = PrimeFocus.noScale(),
                border = ClickableSurfaceDefaults.border(
                    border = Border(BorderStroke(1.dp, colors.dotInactive), shape = PrimeTheme.shapes.pill),
                    focusedBorder = Border(BorderStroke(3.dp, colors.accent), inset = 3.dp, shape = PrimeTheme.shapes.pill),
                ),
            ) {
                Box(Modifier.fillMaxHeight().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Text(t("settings.add_keyword"), style = PrimeTheme.typography.buttonSmall)
                }
            }
        }
    }
    if (state.addingKeyword) {
        val fieldFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { fieldFocus.requestFocus() } }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimeTextField(
                value = state.keywordInput,
                onValueChange = viewModel::onKeywordInput,
                placeholder = t("settings.keyword_placeholder"),
                onDone = viewModel::confirmAddKeyword,
                modifier = Modifier.widthIn(min = 260.dp, max = 420.dp).focusRequester(fieldFocus),
            )
            PrimeButton(text = t("settings.add"), onClick = viewModel::confirmAddKeyword, height = 44.dp, horizontalPadding = 18.dp, textStyle = PrimeTheme.typography.buttonMedium, enabled = state.keywordInput.isNotBlank())
            PrimeButton(text = t("common.cancel"), onClick = viewModel::cancelAddKeyword, style = PrimeButtonStyle.Secondary, height = 44.dp, horizontalPadding = 16.dp, textStyle = PrimeTheme.typography.buttonSmall)
        }
    }
    Text(
        t("settings.football_counts", "channels" to state.footballChannels, "vod" to state.footballVod),
        style = PrimeTheme.typography.cardBody,
        color = colors.textSecondary,
        modifier = Modifier.padding(top = 6.dp),
    )
    Text(t("settings.remove_hint"), style = PrimeTheme.typography.caption, color = colors.textSecondary)
}

@Composable
private fun KeywordChip(keyword: String, onRemove: () -> Unit) {
    val colors = PrimeTheme.colors
    Surface(
        onClick = onRemove,
        modifier = Modifier.height(38.dp),
        shape = PrimeFocus.shape(PrimeTheme.shapes.pill),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.surface2, contentColor = colors.text,
            focusedContainerColor = colors.selected, focusedContentColor = colors.text,
            pressedContainerColor = colors.live, pressedContentColor = colors.onAccent,
        ),
        scale = PrimeFocus.noScale(),
        border = PrimeFocus.border(PrimeTheme.shapes.pill),
    ) {
        Row(Modifier.fillMaxHeight().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(keyword, style = PrimeTheme.typography.buttonSmall)
            Text("×", style = PrimeTheme.typography.caption, color = colors.textSecondary)
        }
    }
}

@Composable
private fun PlayerPane(state: SettingsUiState, viewModel: SettingsViewModel) {
    PaneTitle(t("settings.player"), t("settings.player_desc"))
    SettingRow(
        title = t("settings.decoder"),
        subtitle = t("settings.decoder_hint"),
        value = t(if (state.settings.preferSoftwareDecoder) "settings.decoder_software" else "settings.decoder_hardware"),
        onClick = viewModel::toggleSoftwareDecoder,
    )
    SettingRow(
        title = t("settings.buffer"),
        subtitle = t("settings.buffer_hint"),
        value = t(state.settings.bufferProfile.labelKey),
        onClick = viewModel::cycleBuffer,
    )
    Text(t("settings.player_restart_hint"), style = PrimeTheme.typography.caption, color = PrimeTheme.colors.textSecondary)
    SettingRow(
        title = t("settings.overscan"),
        subtitle = t("settings.overscan_hint"),
        value = if (state.settings.overscanPercent == 0) t("settings.overscan_off") else "${state.settings.overscanPercent}%",
        onClick = viewModel::cycleOverscan,
    )
    SettingRow(
        title = t("settings.preview_sound"),
        subtitle = t("settings.preview_sound_hint"),
        value = t(if (state.settings.previewSound) "settings.on" else "settings.off"),
        onClick = viewModel::togglePreviewSound,
    )
    SettingRow(
        title = t("settings.resume_live"),
        subtitle = t("settings.resume_live_hint"),
        value = t(if (state.settings.resumeLive) "settings.on" else "settings.off"),
        onClick = viewModel::toggleResumeLive,
    )
}

@Composable
private fun LanguagePane(state: SettingsUiState, viewModel: SettingsViewModel) {
    PaneTitle(t("settings.language"), t("settings.language_desc"))
    state.languages.forEach { lang ->
        val selected = lang == state.settings.language
        SettingRow(
            title = languageName(lang),
            value = if (selected) t("settings.in_use") else null,
            onClick = { viewModel.selectLanguage(lang) },
        )
    }
    listOf("en" to "English", "es" to "Español").filter { (code, _) -> code !in state.languages }.forEach { (_, name) ->
        SettingRow(title = "$name · ${t("settings.coming_soon")}", value = null)
    }
}

private fun languageName(code: String): String = when (code) {
    "pt-BR" -> "Português (Brasil)"
    "en" -> "English"
    "es" -> "Español"
    else -> code
}
