package com.wpdas.primetv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.delay

/**
 * Pede foco sem lançar exceção: false quando o alvo ainda não está na composição
 * (o Compose devolve false ou lança, conforme a versão).
 */
fun FocusRequester.tryRequestFocus(): Boolean = runCatching { requestFocus() }.getOrDefault(false)

/** Tenta por alguns quadros até o alvo existir (itens de listas lazy, painéis recém-abertos). */
suspend fun FocusRequester.requestFocusWhenReady(attempts: Int = 15, intervalMs: Long = 50): Boolean {
    repeat(attempts) {
        if (tryRequestFocus()) return true
        delay(intervalMs)
    }
    return false
}

@Composable
fun RequestFocusOnce(requester: FocusRequester, vararg keys: Any?) {
    LaunchedEffect(*keys) { requester.requestFocusWhenReady() }
}
