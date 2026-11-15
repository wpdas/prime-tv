package com.wpdas.primetv.ui.screens.live

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.wpdas.primetv.data.player.SharedLivePlayer

/**
 * Prévia do canal escolhido na tela Ao vivo, no player único do ao vivo ([SharedLivePlayer]). A tela
 * segura o player enquanto está visível e solta ao sair; indo para a tela cheia, o player continua o
 * mesmo e só a imagem muda de lugar. [url] null para a prévia.
 */
@OptIn(UnstableApi::class)
@Composable
fun LivePreview(url: String?, channelId: String?, shared: SharedLivePlayer, modifier: Modifier = Modifier, withSound: Boolean = false) {
    val owner = LocalLifecycleOwner.current
    var startTick by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> startTick++
                // Tela coberta (player em tela cheia, outra tela) ou app em segundo plano.
                Lifecycle.Event.ON_STOP -> shared.unclaim(SharedLivePlayer.OWNER_PREVIEW)
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            shared.unclaim(SharedLivePlayer.OWNER_PREVIEW)
        }
    }
    LaunchedEffect(url, startTick) {
        if (url == null) {
            // Sem prévia escolhida: não segura o player (se a tela cheia não estiver usando, ele é liberado).
            shared.unclaim(SharedLivePlayer.OWNER_PREVIEW)
        } else if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            shared.claim(SharedLivePlayer.OWNER_PREVIEW)
            shared.play(url, channelId)
        }
    }
    val player by shared.player.collectAsStateWithLifecycle()
    LaunchedEffect(player, withSound, url) { if (url != null) player?.volume = if (withSound) 1f else 0f }
    // Uma PlayerView (e uma superfície) por instância; ao ganhar a view, a imagem volta para cá.
    val p = player
    if (p != null && url != null) {
        key(p) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        this.player = p
                    }
                },
                onRelease = { view -> view.player = null },
                modifier = modifier,
            )
        }
    }
}
