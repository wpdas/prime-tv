package com.wpdas.primetv.ui.screens.live

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.wpdas.primetv.data.player.PlayerFactory

/**
 * Dono do player da prévia. Criado sob demanda e liberado de forma síncrona: ao abrir o canal em
 * tela cheia a prévia solta decodificador, memória e a conexão com o servidor antes de o player
 * principal nascer (muitas listas limitam conexões simultâneas e boxes fracas têm um decodificador só).
 */
@OptIn(UnstableApi::class)
class PreviewPlayerHolder(private val factory: PlayerFactory) {
    var player: ExoPlayer? by mutableStateOf(null)
        private set

    fun obtain(): ExoPlayer = player ?: factory.create().also { player = it }

    fun release() {
        val p = player ?: return
        player = null
        runCatching { p.stop() }
        runCatching { p.release() }
    }
}

@Composable
fun rememberPreviewPlayerHolder(factory: PlayerFactory): PreviewPlayerHolder {
    val holder = remember { PreviewPlayerHolder(factory) }
    // App em segundo plano ou outra tela por cima: nada de prévia tocando escondida.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) holder.release() }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            holder.release()
        }
    }
    return holder
}

/**
 * Prévia do canal em foco na tela Ao vivo. Troca de mídia quando [url] muda; null para e solta o player.
 */
@OptIn(UnstableApi::class)
@Composable
fun LivePreview(url: String?, holder: PreviewPlayerHolder, modifier: Modifier = Modifier, withSound: Boolean = false) {
    var resumeTick by remember { mutableStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        // Voltando à tela (ex.: do player), a prévia recomeça com um player novo.
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_START) resumeTick++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(url, resumeTick) {
        if (url == null) {
            holder.release()
        } else {
            val player = holder.obtain()
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.playWhenReady = true
        }
    }
    val player = holder.player
    LaunchedEffect(player, withSound) { player?.volume = if (withSound) 1f else 0f }
    // Uma PlayerView (e uma superfície) por player: trocar o player dentro da mesma view deixava a
    // imagem preta ao voltar do tela cheia, com áudio tocando.
    if (player != null) {
        androidx.compose.runtime.key(player) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        this.player = player
                    }
                },
                onRelease = { view -> view.player = null },
                modifier = modifier,
            )
        }
    }
}
