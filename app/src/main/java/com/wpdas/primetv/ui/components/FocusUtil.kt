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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.mutableLongStateOf
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

/**
 * Tecla direcional segurada no controle: o Android repete o evento a cada ~50 ms, mais rápido do
 * que uma box fraca consegue rolar e animar. Enquanto a lista ainda estiver rolando (ou antes de
 * [minIntervalMs] desde o último passo) as repetições são engolidas; o foco anda no ritmo da tela
 * e o anel nunca sai da área visível. O primeiro toque nunca é atrasado.
 *
 * @param vertical eixo em que a lista rola; só as setas desse eixo são freadas.
 * @param isScrolling normalmente `{ listState.isScrollInProgress }`.
 */
@Composable
fun Modifier.throttleHeldDpad(vertical: Boolean, minIntervalMs: Long = 80L, isScrolling: () -> Boolean): Modifier {
    var lastStepMs by remember { mutableLongStateOf(0L) }
    return onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val onAxis = if (vertical) event.key == Key.DirectionDown || event.key == Key.DirectionUp
        else event.key == Key.DirectionLeft || event.key == Key.DirectionRight
        if (!onAxis) return@onPreviewKeyEvent false
        val now = android.os.SystemClock.uptimeMillis()
        if (event.nativeKeyEvent.repeatCount == 0) {
            lastStepMs = now
            return@onPreviewKeyEvent false
        }
        if (isScrolling() || now - lastStepMs < minIntervalMs) return@onPreviewKeyEvent true
        lastStepMs = now
        false
    }
}
