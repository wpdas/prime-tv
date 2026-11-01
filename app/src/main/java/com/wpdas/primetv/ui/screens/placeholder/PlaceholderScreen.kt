package com.wpdas.primetv.ui.screens.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.ui.components.PrimeButton
import com.wpdas.primetv.ui.components.PrimeButtonStyle
import com.wpdas.primetv.ui.theme.PrimeDimens
import com.wpdas.primetv.ui.theme.PrimeTheme

/** Tela provisória para destinos que chegam em fases seguintes. Voltar funciona. */
@Composable
fun PlaceholderScreen(titleKey: String, phase: Int, onBack: () -> Unit, subtitle: String? = null) {
    val colors = PrimeTheme.colors
    val backFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { backFocus.requestFocus() }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = PrimeDimens.screenMargin, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PrimeButton(
            text = t("common.back"),
            onClick = onBack,
            style = PrimeButtonStyle.Ghost,
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            height = 32.dp,
            horizontalPadding = 8.dp,
            modifier = Modifier.focusRequester(backFocus),
        )
        Text(t(titleKey), style = PrimeTheme.typography.screenTitle, color = colors.text)
        if (subtitle != null) Text(subtitle, style = PrimeTheme.typography.body, color = colors.textSecondary)
        Text(t("common.coming_soon", "phase" to phase), style = PrimeTheme.typography.bodyLarge, color = colors.textBody)
    }
}
