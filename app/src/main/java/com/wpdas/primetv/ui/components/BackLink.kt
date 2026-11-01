package com.wpdas.primetv.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wpdas.primetv.core.i18n.t

/** "← Voltar" discreto no topo das telas internas. */
@Composable
fun BackLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    PrimeButton(
        text = t("common.back"),
        onClick = onClick,
        style = PrimeButtonStyle.Ghost,
        icon = Icons.AutoMirrored.Rounded.ArrowBack,
        height = 32.dp,
        horizontalPadding = 8.dp,
        modifier = modifier,
    )
}
