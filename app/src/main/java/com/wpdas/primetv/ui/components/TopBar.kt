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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
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
 * Barra superior: logo (abre o menu do app), Buscar, abas, relógio e Configurações.
 * Altura 72, margem lateral 48.
 */
@Composable
fun TopBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onLogoClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PrimeDimens.topBarHeight)
            .padding(horizontal = PrimeDimens.screenMargin)
            .onFocusChanged { if (it.hasFocus) onFocused?.invoke() }
            .focusRestorer()
            .focusGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        LogoButton(onClick = onLogoClick, contentDescription = t("nav.app_menu"))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PrimeIconButton(
                icon = Icons.Rounded.Search,
                contentDescription = t("nav.search"),
                onClick = onSearchClick,
                style = PrimeButtonStyle.Ghost,
                size = 44.dp,
            )
            HomeTab.entries.forEach { tab ->
                TabItem(label = t(tab.labelKey), selected = tab == selectedTab, onClick = { onTabSelected(tab) })
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Clock()
            PrimeIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = t("nav.settings"),
                onClick = onSettingsClick,
                style = PrimeButtonStyle.Ghost,
                size = 44.dp,
            )
        }
    }
}

@Composable
private fun LogoButton(onClick: () -> Unit, contentDescription: String) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.card
    Surface(
        onClick = onClick,
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = colors.text,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = colors.text,
            pressedContainerColor = Color.Transparent,
            pressedContentColor = colors.text,
        ),
        scale = PrimeFocus.noScale(),
        border = PrimeFocus.border(shape, colors.text),
    ) {
        Logo(Modifier.padding(end = 4.dp))
    }
}

/** Aba de 44 de altura; a selecionada ganha peso 600 e um traço violeta embaixo. */
@Composable
private fun TabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.card
    Surface(
        onClick = onClick,
        modifier = Modifier.height(44.dp),
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
