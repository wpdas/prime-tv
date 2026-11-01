package com.wpdas.primetv.ui.screens.live

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.wpdas.primetv.data.player.PlayerFactory

/**
 * Prévia sem áudio do canal em foco na tela Ao vivo. O player vive enquanto o composable
 * existir e troca de mídia quando [url] muda; null para a reprodução.
 */
@OptIn(UnstableApi::class)
@Composable
fun LivePreview(url: String?, playerFactory: PlayerFactory, modifier: Modifier = Modifier) {
    val player = remember { playerFactory.create().apply { volume = 0f } }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(url) {
        if (url == null) {
            player.stop()
            player.clearMediaItems()
        } else {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.playWhenReady = true
        }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                this.player = player
            }
        },
        modifier = modifier,
    )
}
