package com.wpdas.primetv.ui.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.wpdas.primetv.BuildConfig
import com.wpdas.primetv.R
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.ui.components.BackLink
import com.wpdas.primetv.ui.components.Logo
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeTheme

/** Sobre: versão, repositório e a atribuição exigida pelo TMDB. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = PrimeTheme.colors
    val typography = PrimeTheme.typography
    val backFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }
    Column(
        Modifier.fillMaxSize().background(colors.background).padding(horizontal = PrimeDimens.screenMargin, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        BackLink(onClick = onBack, modifier = Modifier.focusRequester(backFocus))
        Logo(markSize = 56.dp, fontSize = 32, gap = 14.dp)
        Text(t("about.version", "version" to BuildConfig.VERSION_NAME, "code" to BuildConfig.VERSION_CODE), style = typography.body, color = colors.textSecondary)
        Text(t("about.description"), style = typography.bodyLarge, color = colors.textBody, modifier = Modifier.width(760.dp))
        Text(t("about.repository", "repo" to "github.com/${BuildConfig.GITHUB_REPO}"), style = typography.body, color = colors.textSecondary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(top = 20.dp)) {
            Image(painter = painterResource(R.drawable.tmdb_logo), contentDescription = "TMDB", modifier = Modifier.width(180.dp))
            Text(t("about.tmdb_notice"), style = typography.body, color = colors.textBody, modifier = Modifier.width(560.dp))
        }
        Text(t("about.inter_notice"), style = typography.caption, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
    }
}
