package com.wpdas.primetv.data.player

import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Um player só para o ao vivo: a prévia da tela Ao vivo e a tela cheia usam a mesma instância, e só a
 * superfície de vídeo muda de lugar. Abrir em tela cheia o canal que já está na prévia não reconecta
 * nem recarrega; ao voltar, a prévia continua no canal que estava em tela cheia.
 *
 * Quem usa "segura" o player ([claim]) e solta ([unclaim]) ao sair; sem nenhum dono por
 * [RELEASE_GRACE_MS], ele é liberado (decodificador, memória e conexão com o servidor). A folga cobre a
 * troca de tela, em que um dono sai um instante antes ou depois do outro entrar. Tudo na thread principal.
 */
@OptIn(UnstableApi::class)
@Singleton
class SharedLivePlayer @Inject constructor(private val factory: PlayerFactory) {

    private val _player = MutableStateFlow<ExoPlayer?>(null)
    /** Instância atual (null quando liberada), para a prévia recompor com a nova. */
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    /** Endereço e canal em reprodução, para quem chega saber se pode só continuar. */
    var currentUrl: String? = null
        private set
    var currentChannelId: String? = null
        private set

    private val owners = HashSet<String>()
    private val handler = Handler(Looper.getMainLooper())
    private val releaseIfUnused = Runnable { if (owners.isEmpty()) release() }

    fun claim(owner: String): ExoPlayer {
        owners += owner
        handler.removeCallbacks(releaseIfUnused)
        return _player.value ?: factory.create().also { _player.value = it }
    }

    fun unclaim(owner: String) {
        owners -= owner
        if (owners.isEmpty()) {
            handler.removeCallbacks(releaseIfUnused)
            handler.postDelayed(releaseIfUnused, RELEASE_GRACE_MS)
        }
    }

    /** O canal [url] já está tocando ou carregando neste player (não precisa preparar de novo). */
    fun isActive(url: String): Boolean {
        val p = _player.value ?: return false
        return currentUrl == url && (p.playbackState == Player.STATE_READY || p.playbackState == Player.STATE_BUFFERING)
    }

    /** Toca [url] (continua, se já for ele). Exige um [claim] antes. */
    fun play(url: String, channelId: String?) {
        val p = _player.value ?: return
        if (isActive(url)) {
            p.playWhenReady = true
            if (channelId != null) currentChannelId = channelId
            return
        }
        markCurrent(url, channelId)
        p.setMediaItem(MediaItem.fromUri(url))
        p.prepare()
        p.playWhenReady = true
    }

    /** Quem prepara a mídia por conta própria (tela cheia) avisa o que está tocando. */
    fun markCurrent(url: String, channelId: String?) {
        currentUrl = url
        currentChannelId = channelId
    }

    fun stop() {
        _player.value?.let { runCatching { it.stop() } }
        currentUrl = null
        currentChannelId = null
    }

    private fun release() {
        handler.removeCallbacks(releaseIfUnused)
        val p = _player.value ?: return
        _player.value = null
        currentUrl = null
        currentChannelId = null
        runCatching { p.stop() }
        runCatching { p.release() }
    }

    companion object {
        const val OWNER_PREVIEW = "preview"
        const val OWNER_FULLSCREEN = "fullscreen"
        const val RELEASE_GRACE_MS = 1_500L
    }
}
