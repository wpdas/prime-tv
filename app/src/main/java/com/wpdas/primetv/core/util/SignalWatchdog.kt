package com.wpdas.primetv.core.util

/**
 * Vigia o começo de um canal ao vivo: se nenhum byte chegar da rede por [timeoutMs] desde a
 * sintonia ou desde o último dado recebido, [onTick] devolve true uma única vez. Cobre tanto o
 * servidor que não responde quanto o que manda alguns pacotes e para (a velocidade cai e some).
 */
class SignalWatchdog(private val timeoutMs: Long) {
    private var lastDataAt = 0L
    private var armed = false

    /** Nova sintonia: zera o relógio. */
    fun start(nowMs: Long) {
        lastDataAt = nowMs
        armed = true
    }

    /** Desliga até o próximo [start] (canal pronto, erro tratado ou usuário escolhendo outro). */
    fun stop() {
        armed = false
    }

    /** Chamado periodicamente enquanto o canal carrega. [receivingData]: chegaram bytes na janela recente. */
    fun onTick(nowMs: Long, receivingData: Boolean): Boolean {
        if (!armed) return false
        if (receivingData) {
            lastDataAt = nowMs
            return false
        }
        if (nowMs - lastDataAt < timeoutMs) return false
        armed = false
        return true
    }
}
