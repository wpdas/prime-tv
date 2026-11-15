package com.wpdas.primetv.ui.screens.uimode

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.domain.model.UiMode
import com.wpdas.primetv.domain.repository.SettingsRepository
import com.wpdas.primetv.ui.components.LogoWordmark
import com.wpdas.primetv.ui.components.TagChip
import com.wpdas.primetv.ui.components.tryRequestFocus
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UiModeViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    /** Modo em uso, se a pergunta já foi respondida (ao abrir por Configurações). */
    val current: StateFlow<UiMode?> = settings.settings
        .map { if (it.uiModeChosen) it.uiMode else null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settings.settings.value.let { if (it.uiModeChosen) it.uiMode else null })

    fun choose(mode: UiMode, then: () -> Unit) {
        viewModelScope.launch {
            settings.setUiMode(mode)
            then()
        }
    }
}

/**
 * Pergunta única (depois da primeira lista, e de novo em Configurações › Interface): interface Completa
 * ou Simples. [onChosen] leva à tela inicial do modo escolhido.
 */
@Composable
fun UiModeScreen(onChosen: (UiMode) -> Unit, viewModel: UiModeViewModel = hiltViewModel()) {
    val current by viewModel.current.collectAsStateWithLifecycle()
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    val completeFocus = remember { FocusRequester() }
    val simpleFocus = remember { FocusRequester() }
    // A vinheta de abertura cobre esta tela e segura o foco por alguns segundos: insiste até o cartão ficar focado.
    var cardsFocused by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(Unit) {
        repeat(120) {
            if (cardsFocused) return@LaunchedEffect
            (if (current == UiMode.SIMPLE) simpleFocus else completeFocus).tryRequestFocus()
            kotlinx.coroutines.delay(100)
        }
    }
    Box(Modifier.fillMaxSize().background(colors.background)) {
        LogoWordmark(Modifier.padding(start = 48.dp, top = 28.dp))
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(t("uimode.title"), style = typography.screenTitle, color = colors.text, textAlign = TextAlign.Center)
            Text(t("uimode.subtitle"), style = typography.bodyLarge, color = colors.textSecondary, textAlign = TextAlign.Center)
            Row(Modifier.padding(top = 28.dp).onFocusChanged { cardsFocused = it.hasFocus }.focusGroup(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                ModeCard(
                    icon = Icons.Rounded.Dashboard,
                    title = t("uimode.complete"),
                    description = t("uimode.complete_desc"),
                    current = current == UiMode.COMPLETE,
                    onClick = { viewModel.choose(UiMode.COMPLETE) { onChosen(UiMode.COMPLETE) } },
                    modifier = Modifier.focusRequester(completeFocus),
                )
                ModeCard(
                    icon = Icons.Rounded.ViewList,
                    title = t("uimode.simple"),
                    description = t("uimode.simple_desc"),
                    current = current == UiMode.SIMPLE,
                    onClick = { viewModel.choose(UiMode.SIMPLE) { onChosen(UiMode.SIMPLE) } },
                    modifier = Modifier.focusRequester(simpleFocus),
                )
            }
        }
    }
}

@Composable
private fun ModeCard(icon: ImageVector, title: String, description: String, current: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    val shape = PrimeTheme.shapes.panelLarge
    Surface(
        onClick = onClick,
        modifier = modifier.width(400.dp).height(250.dp),
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.surface,
            contentColor = colors.text,
            focusedContainerColor = colors.surface2,
            focusedContentColor = colors.text,
            pressedContainerColor = colors.surface2,
            pressedContentColor = colors.text,
        ),
        scale = PrimeFocus.scale(),
        border = PrimeFocus.border(shape),
    ) {
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.size(48.dp).background(colors.accentSoftBackground, PrimeTheme.shapes.card), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = colors.accentSoftText, modifier = Modifier.size(28.dp))
                }
                Text(title, style = typography.title, color = colors.text, modifier = Modifier.weight(1f))
                if (current) TagChip(t("uimode.current"))
            }
            Text(description, style = typography.body, color = colors.textBody)
        }
    }
}
