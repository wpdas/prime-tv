package com.wpdas.primetv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme

/**
 * Card horizontal 210 de largura: arte 110 de altura com título e barra de
 * progresso, mais uma linha de metadados. Usado em "Continuar assistindo".
 */
@Composable
fun WideCard(
    item: MediaItem,
    meta: String,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: ((MediaItem) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.card
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .width(210.dp)
            .onFocusChanged { if (it.isFocused) onFocused?.invoke(item) },
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.surface,
            contentColor = colors.text,
            focusedContainerColor = colors.surface,
            focusedContentColor = colors.text,
            pressedContainerColor = colors.surface,
            pressedContentColor = colors.text,
        ),
        scale = PrimeFocus.scale(),
        border = PrimeFocus.border(shape),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(110.dp)) {
                BackdropArtwork(item, Modifier.fillMaxWidth().height(110.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, colors.background.copy(alpha = 0.7f)))),
                )
                Text(
                    text = item.title,
                    style = PrimeTheme.typography.cardTitle,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 10.dp),
                )
                ThinProgressBar(progress, Modifier.align(Alignment.BottomStart))
            }
            Text(
                text = meta,
                style = PrimeTheme.typography.caption,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
