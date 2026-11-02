package com.wpdas.primetv.core.util

/**
 * Conta rebuffers (player voltou a carregar no meio da reprodução) numa janela de tempo.
 * Quando chega ao limite, [onStall] devolve true uma vez e zera a contagem: é a deixa para
 * baixar a qualidade. Travamentos causados por seek ou pela carga inicial não devem ser contados.
 */
class StallDetector(private val limit: Int = 3, private val windowMs: Long = 90_000L) {
    private val times = ArrayDeque<Long>()

    fun onStall(nowMs: Long): Boolean {
        times.addLast(nowMs)
        while (times.isNotEmpty() && times.first() < nowMs - windowMs) times.removeFirst()
        if (times.size < limit) return false
        times.clear()
        return true
    }

    fun reset() = times.clear()
}
