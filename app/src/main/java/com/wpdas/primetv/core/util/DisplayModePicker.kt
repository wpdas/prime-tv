package com.wpdas.primetv.core.util

import kotlin.math.abs

/** Modo de saída do aparelho (HDMI): resolução física e taxa de quadros. */
data class DisplayMode(val id: Int, val width: Int, val height: Int, val refreshRate: Float)

/**
 * Escolhe o modo de saída para a TV fazer o upscaling no lugar da box: o menor modo que comporta
 * o vídeo (entre iguais, a taxa de quadros mais próxima da atual). Nunca desce de 720 linhas (a interface precisa
 * continuar legível) e, se nenhum modo comporta o vídeo, fica como está.
 */
object DisplayModePicker {
    const val MIN_HEIGHT = 720

    /** null = não mexer (já está no modo certo, vídeo sem tamanho ou nenhum modo serve). */
    fun pick(modes: List<DisplayMode>, current: DisplayMode, videoWidth: Int, videoHeight: Int): DisplayMode? {
        if (videoWidth <= 0 || videoHeight <= 0) return null
        val fitting = modes.filter { it.width >= videoWidth && it.height >= videoHeight && it.height >= MIN_HEIGHT }
        if (fitting.isEmpty()) return null
        // Menor resolução que comporta o vídeo; entre as iguais, a taxa mais próxima da atual.
        val best = fitting.minWith(compareBy<DisplayMode>({ it.width.toLong() * it.height }, { abs(it.refreshRate - current.refreshRate) }, { it.id }))
        return best.takeIf { it.id != current.id }
    }
}
