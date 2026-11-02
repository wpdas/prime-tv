package com.wpdas.primetv.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

/**
 * Ao ganhar foco, traz o item para a área visível com uma folga ao redor, para o anel de
 * foco e a escala (que ficam fora dos limites do item) não serem cortados pela lista.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.bringIntoViewOnFocus(padding: Dp = 24.dp): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var size by remember { mutableStateOf(Size.Zero) }
    return this
        .onSizeChanged { size = Size(it.width.toFloat(), it.height.toFloat()) }
        .bringIntoViewRequester(requester)
        .onFocusChanged { state ->
            if (state.isFocused && size != Size.Zero) {
                val pad = with(density) { padding.toPx() }
                scope.launch { requester.bringIntoView(Rect(-pad, -pad, size.width + pad, size.height + pad)) }
            }
        }
}
