package com.wpdas.primetv.data.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.wpdas.primetv.domain.model.BufferProfile
import com.wpdas.primetv.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cria instâncias do ExoPlayer com a configuração do app: OkHttp (mesmo User-Agent e
 * timeouts), decodificador por hardware com fallback para software e buffers pensados
 * para IPTV em box fraca. HLS, MPEG-TS e MP4/MKV são reconhecidos pela extensão/conteúdo.
 */
@OptIn(UnstableApi::class)
@Singleton
class PlayerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    http: OkHttpClient,
    private val settings: SettingsRepository,
) {
    private val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(context, OkHttpDataSource.Factory(http))

    fun create(): ExoPlayer {
        val prefs = settings.settings.value
        return create(prefs.preferSoftwareDecoder, prefs.bufferProfile)
    }

    fun create(preferSoftwareDecoder: Boolean, buffer: BufferProfile): ExoPlayer {
        val renderers = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(
                if (preferSoftwareDecoder) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON,
            )
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(buffer.minBufferMs, buffer.maxBufferMs, buffer.playbackMs, buffer.rebufferMs)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        return ExoPlayer.Builder(context, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(SEEK_STEP_MS)
            .setSeekForwardIncrementMs(SEEK_STEP_MS)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                    true,
                )
            }
    }

    companion object {
        const val SEEK_STEP_MS = 10_000L
    }
}
