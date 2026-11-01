package com.wpdas.primetv.ui.screens.appmenu

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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wpdas.primetv.BuildConfig
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.ui.components.Logo
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme

/**
 * Menu do logo: versão e atalhos. Verificar atualização e Trocar lista ganham
 * comportamento nas fases 6 e 7; por ora levam às telas provisórias.
 */
@Composable
fun AppMenuScreen(
    onCheckUpdate: () -> Unit,
    onSettings: () -> Unit,
    onChangePlaylist: () -> Unit,
    onAbout: () -> Unit,
) {
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Box(Modifier.fillMaxSize().background(colors.scrim)) {
        Logo(modifier = Modifier.padding(start = PrimeDimens.screenMargin, top = 18.dp))
        Column(
            Modifier
                .padding(start = PrimeDimens.screenMargin, top = 76.dp)
                .width(300.dp)
                .background(colors.surfaceMenu, PrimeTheme.shapes.panel)
                .padding(8.dp)
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
                Text("PrimeTV", style = typography.bodyStrong, color = colors.text)
                Text(t("about.version", "version" to BuildConfig.VERSION_NAME, "code" to BuildConfig.VERSION_CODE), style = typography.caption, color = colors.textSecondary)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.buttonSecondary))
            MenuItem(Icons.Rounded.Refresh, t("menu.check_update"), onCheckUpdate, Modifier.focusRequester(first))
            MenuItem(Icons.Rounded.Settings, t("menu.settings"), onSettings)
            MenuItem(Icons.Rounded.SwapHoriz, t("menu.change_playlist"), onChangePlaylist)
            MenuItem(Icons.Rounded.Info, t("menu.about"), onAbout)
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.card
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = colors.textBody,
            focusedContainerColor = colors.accentSoftBackground,
            focusedContentColor = colors.text,
            pressedContainerColor = colors.accentSoftBackground,
            pressedContentColor = colors.text,
        ),
        scale = PrimeFocus.noScale(),
        border = PrimeFocus.border(shape),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = PrimeTheme.typography.listTitle)
        }
    }
}
