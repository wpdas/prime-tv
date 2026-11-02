package com.wpdas.primetv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

/** Card vertical 150×220 com pôster e título sobreposto. */
@Composable
fun PosterCard(
    item: MediaItem,
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
            .size(150.dp, 220.dp)
            .bringIntoViewOnFocus()
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
        PosterArtwork(item, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxWidth()
                .height(90.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.background.copy(alpha = 0.85f)))),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(item.title, style = PrimeTheme.typography.cardTitle, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
