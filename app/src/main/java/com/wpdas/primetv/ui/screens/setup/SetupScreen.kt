package com.wpdas.primetv.ui.screens.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.PlaylistError
import com.wpdas.primetv.domain.model.SyncState
import com.wpdas.primetv.ui.components.LoadingSpinner
import com.wpdas.primetv.ui.components.Logo
import com.wpdas.primetv.ui.components.PrimeButton
import com.wpdas.primetv.ui.components.PrimeButtonStyle
import com.wpdas.primetv.ui.components.PrimeTextField
import com.wpdas.primetv.ui.components.TagChip
import com.wpdas.primetv.ui.theme.PrimeTheme
import kotlinx.coroutines.delay

private const val FOCUS_DELAY_MS = 150L

@Composable
fun SetupScreen(
    onStartWatching: () -> Unit,
    onReceiveLink: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SetupContent(
        state = state,
        onUrlChange = viewModel::onUrlChange,
        onConnect = viewModel::connect,
        onRetryLive = viewModel::retryLive,
        onStartWatching = onStartWatching,
        onReceiveLink = onReceiveLink,
    )
}

@Composable
fun SetupContent(
    state: SetupUiState,
    onUrlChange: (String) -> Unit,
    onConnect: () -> Unit,
    onRetryLive: () -> Unit,
    onStartWatching: () -> Unit,
    onReceiveLink: () -> Unit,
) {
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    val fieldFocus = remember { FocusRequester() }
    val connectFocus = remember { FocusRequester() }
    val startFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { (if (state.url.isBlank()) fieldFocus else connectFocus).requestFocus() } }

    Row(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 96.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(72.dp),
    ) {
        Column(
            Modifier.weight(1f).focusGroup(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Logo(markSize = 56.dp, fontSize = 32, gap = 14.dp)
            Text(t("setup.title"), style = typography.screenTitle, color = colors.text, modifier = Modifier.padding(top = 6.dp))
            Text(t("setup.step"), style = typography.bodyLarge, color = colors.textSecondary)

            Text(t("setup.url_label"), style = typography.bodyStrong, color = colors.text, modifier = Modifier.padding(top = 4.dp))
            PrimeTextField(
                value = state.url,
                onValueChange = onUrlChange,
                placeholder = t("setup.url_placeholder"),
                keyboardType = KeyboardType.Uri,
                onDone = onConnect,
                enabled = !state.connected,
                // Para baixo do campo largo, o foco vai sempre para "Conectar" (e não para o botão mais próximo geometricamente).
                modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus).focusProperties { down = connectFocus },
            )
            Row(
                Modifier.fillMaxWidth().background(colors.surface, PrimeTheme.shapes.card).padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = colors.accentSoftText, modifier = Modifier.size(20.dp))
                Text(t("setup.hint"), style = typography.body, color = colors.textBody)
            }

            val typeLabel = (state.detected ?: state.guess)?.let { t(it.labelKey) }
            if (typeLabel != null || state.error != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (typeLabel != null) {
                        TagChip(if (state.detected != null) t("setup.detected", "type" to typeLabel) else t("setup.looks_like", "type" to typeLabel))
                    }
                    when {
                        state.error != null -> Text(errorText(state.error), style = typography.cardBody, color = colors.live)
                        state.hasEpg == true -> Text(t("setup.epg_found"), style = typography.cardBody, color = colors.textSecondary)
                        state.hasEpg == false -> Text(t("setup.epg_missing"), style = typography.cardBody, color = colors.textSecondary)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 6.dp)) {
                if (!state.connected) {
                    PrimeButton(
                        text = when {
                            state.isDetecting -> t("setup.connecting")
                            state.error != null -> t("common.retry")
                            else -> t("setup.connect")
                        },
                        onClick = onConnect,
                        enabled = state.canConnect,
                        height = 52.dp,
                        horizontalPadding = 28.dp,
                        modifier = Modifier.focusRequester(connectFocus),
                    )
                    PrimeButton(
                        text = t("setup.receive_link"),
                        onClick = onReceiveLink,
                        style = PrimeButtonStyle.Secondary,
                        icon = Icons.Rounded.PhoneAndroid,
                        height = 52.dp,
                        horizontalPadding = 20.dp,
                        textStyle = typography.bodyLarge,
                    )
                }
            }
        }

        if (state.connected) {
            ProgressPanel(state = state, onRetryLive = onRetryLive, onStartWatching = onStartWatching, startFocus = startFocus)
        }
    }
}

@Composable
private fun ProgressPanel(
    state: SetupUiState,
    onRetryLive: () -> Unit,
    onStartWatching: () -> Unit,
    startFocus: FocusRequester,
) {
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    // O foco vai para o botão assim que o painel aparece; ele "acende" (primário) quando Ao vivo termina.
    val focusManager = LocalFocusManager.current
    LaunchedEffect(state.liveFailed) {
        // O botão "Conectar" some no mesmo quadro em que o painel aparece, e o Compose
        // passa o foco para cá antes de o botão começar a observar as interações; sem o
        // clearFocus o anel de foco não é desenhado.
        delay(FOCUS_DELAY_MS)
        focusManager.clearFocus(force = true)
        runCatching { startFocus.requestFocus() }
    }
    Column(
        Modifier
            .width(380.dp)
            .background(colors.surface, PrimeTheme.shapes.panelLarge)
            .padding(28.dp)
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(t("setup.loading_title"), style = typography.subtitle, color = colors.text)
        Text(t("setup.loading_hint"), style = typography.cardBody, color = colors.textSecondary)
        listOf(ContentSection.LIVE, ContentSection.MOVIES, ContentSection.SERIES, ContentSection.EPG).forEach { section ->
            SyncStep(section = section, state = state.sync[section] ?: SyncState.Idle)
        }
        if (state.liveFailed) {
            PrimeButton(text = t("common.retry"), onClick = onRetryLive, modifier = Modifier.fillMaxWidth().focusRequester(startFocus))
        } else {
            PrimeButton(
                text = t("setup.start"),
                onClick = { if (state.canStart) onStartWatching() },
                style = if (state.canStart) PrimeButtonStyle.Primary else PrimeButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).focusRequester(startFocus),
            )
        }
    }
}

@Composable
private fun SyncStep(section: ContentSection, state: SyncState) {
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            when (state) {
                is SyncState.Running -> LoadingSpinner(size = 18.dp, strokeWidth = 3.dp)
                is SyncState.Done -> Box(Modifier.size(14.dp).background(colors.accent, CircleShape))
                is SyncState.Skipped -> Box(Modifier.size(14.dp).background(colors.divider, CircleShape))
                is SyncState.Error -> Box(Modifier.size(14.dp).background(colors.live, CircleShape))
                SyncState.Idle -> Box(Modifier.size(14.dp).border(2.dp, colors.divider, CircleShape))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(t(section.labelKey), style = typography.listTitle, color = colors.text)
            Text(stepInfo(section, state), style = typography.caption, color = if (state is SyncState.Error) colors.live else colors.textSecondary)
        }
    }
}

@Composable
private fun stepInfo(section: ContentSection, state: SyncState): String = when (state) {
    SyncState.Idle -> t("sync.waiting")
    is SyncState.Running -> if (state.count == 0) t("sync.starting") else t("sync.downloading", "n" to state.count)
    is SyncState.Done -> when (section) {
        ContentSection.LIVE -> t("sync.done_live", "n" to state.count)
        ContentSection.MOVIES -> t("sync.done_movies", "n" to state.count)
        ContentSection.SERIES -> t("sync.done_series", "n" to state.count)
        ContentSection.EPG -> t("sync.done_epg")
    }
    is SyncState.Skipped -> t(state.reasonKey)
    is SyncState.Error -> errorText(state.error)
}

@Composable
internal fun errorText(error: PlaylistError): String = when (error) {
    is PlaylistError.Unknown -> t(error.messageKey, "detail" to error.detail)
    else -> t(error.messageKey)
}
