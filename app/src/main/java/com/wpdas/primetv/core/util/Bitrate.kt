package com.wpdas.primetv.core.util

import java.util.Locale

/** Formata uma taxa em bits por segundo para exibir: "850 kb/s", "2,4 Mb/s". */
object Bitrate {
    fun format(bitsPerSecond: Long, locale: Locale = Locale("pt", "BR")): String {
        val bps = bitsPerSecond.coerceAtLeast(0)
        return when {
            bps >= 1_000_000 -> String.format(locale, "%.1f Mb/s", bps / 1_000_000.0)
            else -> String.format(locale, "%d kb/s", bps / 1_000)
        }
    }
}
