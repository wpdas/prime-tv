package com.wpdas.primetv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.wpdas.primetv.ui.theme.PrimeTheme

/** Aviso discreto de status (ex.: "Atualizando Filmes em segundo plano"). Posicione no canto da tela. */
@Composable
fun StatusToast(
    text: String?,
    modifier: Modifier = Modifier,
    showSpinner: Boolean = true,
) {
    val colors = PrimeTheme.colors
    AnimatedVisibility(
        visible = text != null,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        Row(
            Modifier
                .background(colors.surface2, PrimeTheme.shapes.card)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showSpinner) LoadingSpinner(size = 14.dp, strokeWidth = 2.dp)
            Text(text.orEmpty(), style = PrimeTheme.typography.cardBody, color = colors.textBody)
        }
    }
}
