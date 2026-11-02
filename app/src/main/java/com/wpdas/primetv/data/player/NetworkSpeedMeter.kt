package com.wpdas.primetv.data.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Velocidade de download do stream nos últimos segundos, medida nos bytes que chegam da rede.
 * Funciona também em streams contínuos (MPEG-TS ao vivo), em que o ExoPlayer não publica
 * estimativa de banda porque a transferência nunca termina.
 */
@OptIn(UnstableApi::class)
@Singleton
class NetworkSpeedMeter @Inject constructor() : TransferListener {
    private class Sample(val at: Long, val bytes: Long)

    private val samples = ArrayDeque<Sample>()
    private val lock = Any()

    override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit
    override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit
    override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit

    override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {
        if (!isNetwork || bytesTransferred <= 0) return
        val now = System.currentTimeMillis()
        synchronized(lock) {
            samples.addLast(Sample(now, bytesTransferred.toLong()))
            trim(now)
        }
    }

    /** Bits por segundo na janela recente; null sem tráfego. */
    fun bitrate(now: Long = System.currentTimeMillis()): Long? = synchronized(lock) {
        trim(now)
        if (samples.isEmpty()) return null
        val bytes = samples.sumOf { it.bytes }
        val span = (now - samples.first.at).coerceAtLeast(MIN_SPAN_MS)
        bytes * 8 * 1000 / span
    }

    private fun trim(now: Long) {
        while (samples.isNotEmpty() && now - samples.first.at > WINDOW_MS) samples.removeFirst()
    }

    private companion object {
        const val WINDOW_MS = 3_000L
        const val MIN_SPAN_MS = 500L
    }
}
