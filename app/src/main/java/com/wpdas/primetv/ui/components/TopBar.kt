package com.wpdas.primetv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.domain.model.HomeTab
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme

/**
 * Barra superior: wordmark, Buscar, abas, relógio e o menu do app (⋯, com Configurações dentro).
 * Altura 72, margem lateral 48. Ao subir da tela para a barra, o foco cai sempre na aba ativa, para
 * não trocar de seção sem querer.
 */
@Composable
fun TopBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: (() -> Unit)? = null,
    /** Aba ganhou foco (estilo Netflix: a tela troca sem precisar de OK); null = o foco foi para Buscar ou ⋯. */
    onTabFocused: ((HomeTab?) -> Unit)? = null,
    /** Abas visíveis; Kids e Anime só entram quando a lista tem conteúdo para elas. */
    tabs: List<HomeTab> = HomeTab.entries.filter { it !in HomeTab.OPTIONAL },
    /** Quando informado, fica na aba ativa: a tela pede foco nela ao chegar pela barra. */
    selectedTabFocus: FocusRequester? = null,
) {
    val selectedRequester = selectedTabFocus ?: remember { FocusRequester() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PrimeDimens.topBarHeight)
            .padding(horizontal = PrimeDimens.screenMargin)
            .onFocusChanged { if (it.hasFocus) onFocused?.invoke() }
            // Entrando na barra (de baixo ou por foco programático), a aba ativa recebe o foco.
            .focusProperties { onEnter = { selectedRequester.requestFocus() } }
            .focusGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        LogoWordmark(Modifier.padding(end = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PrimeIconButton(
                icon = Icons.Rounded.Search,
                contentDescription = t("nav.search"),
                onClick = onSearchClick,
                style = PrimeButtonStyle.Ghost,
                size = 44.dp,
                modifier = Modifier.onFocusChanged { if (it.isFocused) onTabFocused?.invoke(null) },
            )
            // key(tab): a lista de abas chega em duas etapas (padrão e depois com Kids/Anime/Novelas). Sem a
            // chave, o Compose reaproveita os nós por posição e a aba em foco "vira" outra quando uma entra antes.
            tabs.forEach { tab ->
                androidx.compose.runtime.key(tab) {
                    TabItem(
                        label = t(tab.labelKey),
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) },
                        onFocused = onTabFocused?.let { cb -> { cb(tab) } },
                        modifier = if (tab == selectedTab) Modifier.focusRequester(selectedRequester) else Modifier,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Clock()
            PrimeIconButton(
                icon = Icons.Rounded.MoreHoriz,
                contentDescription = t("nav.app_menu"),
                onClick = onMenuClick,
                style = PrimeButtonStyle.Ghost,
                size = 44.dp,
                // Passar pelas abas a caminho do menu não pode trocar de seção.
                modifier = Modifier.onFocusChanged { if (it.isFocused) onTabFocused?.invoke(null) },
            )
        }
    }
}

/** Aba de 44 de altura; a selecionada ganha peso 600 e um traço violeta embaixo. */
@Composable
private fun TabItem(label: String, selected: Boolean, onClick: () -> Unit, onFocused: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.card
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp).onFocusChanged { if (it.isFocused) onFocused?.invoke() },
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = if (selected) colors.text else colors.textSecondary,
            focusedContainerColor = colors.surface2,
            focusedContentColor = colors.text,
            pressedContainerColor = colors.surface2,
            pressedContentColor = colors.text,
        ),
        scale = PrimeFocus.noScale(),
        border = PrimeFocus.border(shape),
    ) {
        Box(Modifier.height(44.dp).width(IntrinsicSize.Max).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = if (selected) PrimeTheme.typography.tabSelected else PrimeTheme.typography.tab,
                maxLines = 1,
            )
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(colors.accent),
                )
            }
        }
    }
}
