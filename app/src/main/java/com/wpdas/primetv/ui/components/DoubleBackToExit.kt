package com.wpdas.primetv.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wpdas.primetv.core.i18n.t
import com.wpdas.primetv.ui.theme.PrimeDimens
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tela inicial sem nada atrás (ex.: Ao vivo no modo simples): o primeiro Voltar avisa e o segundo, em
 * até 2,5 s, fecha o app de verdade (tarefa e processo), como na Início.
 */
@Composable
fun BoxScope.DoubleBackToExit(enabled: Boolean) {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var armed by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    BackHandler(enabled = enabled) {
        if (armed) {
            activity?.finishAndRemoveTask()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ android.os.Process.killProcess(android.os.Process.myPid()) }, 300L)
        } else {
            armed = true
            job?.cancel()
            job = scope.launch { delay(2_500L); armed = false }
        }
    }
    StatusToast(
        text = if (armed) t("home.press_back_again") else null,
        showSpinner = false,
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = PrimeDimens.screenMargin, bottom = 20.dp),
    )
}
