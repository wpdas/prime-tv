package com.wpdas.primetv.ui.screens.intro

import android.media.MediaPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wpdas.primetv.R
import com.wpdas.primetv.ui.components.LogoWordmark
import com.wpdas.primetv.ui.components.tryRequestFocus
import com.wpdas.primetv.ui.theme.PrimeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Vinheta de abertura: o wordmark "PrimeTV" surge num leve zoom vindo em direção à tela,
 * com um som curto que cresce e termina num "plim". Espera ~1 s de fundo liso antes de
 * começar, para a animação não disputar o processador com a inicialização do app (banco,
 * lista, primeira composição) e travar. Qualquer tecla pula, e nada espera por ela: a lista
 * e a Início carregam por baixo enquanto ela roda.
 */
@Composable
fun IntroOverlay(onFinished: () -> Unit) {
    val colors = PrimeTheme.colors
    val context = LocalContext.current
    val scale = remember { Animatable(0.82f) }
    val alpha = remember { Animatable(0f) }
    val focus = remember { FocusRequester() }
    // Guardado fora do estado do Compose: criado em segundo plano e liberado ao sair.
    val holder = remember { arrayOfNulls<MediaPlayer>(1) }

    DisposableEffect(Unit) {
        onDispose { runCatching { holder[0]?.release() }; holder[0] = null }
    }
    LaunchedEffect(Unit) {
        focus.tryRequestFocus()
        // Decodifica o som fora da thread principal enquanto a tela "respira".
        val player = withContext(Dispatchers.IO) { runCatching { MediaPlayer.create(context, R.raw.intro) }.getOrNull() }
        holder[0] = player
        delay(START_DELAY_MS)
        runCatching { player?.start() }
        coroutineScope {
            launch { alpha.animateTo(1f, tween(FADE_IN_MS)) }
            launch { scale.animateTo(1f, tween(ZOOM_MS, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))) }
        }
        delay(HOLD_MS)
        alpha.animateTo(0f, tween(FADE_OUT_MS))
        onFinished()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                // Qualquer tecla pula a vinheta (consome o evento para não vazar para a tela de baixo).
                if (event.type == KeyEventType.KeyUp) onFinished()
                true
            },
        contentAlignment = Alignment.Center,
    ) {
        LogoWordmark(
            fontSize = 84,
            // Folga em volta: o texto não é cortado nas bordas da própria caixa ao ser desenhado maior.
            modifier = Modifier.padding(24.dp).graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
        )
    }
}

private const val START_DELAY_MS = 1_000L
private const val FADE_IN_MS = 450
private const val ZOOM_MS = 1_500
private const val HOLD_MS = 1_750L
private const val FADE_OUT_MS = 350
