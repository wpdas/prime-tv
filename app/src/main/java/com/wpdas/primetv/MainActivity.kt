package com.wpdas.primetv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.wpdas.primetv.core.i18n.I18n
import com.wpdas.primetv.domain.crests.TeamCrestRepository
import com.wpdas.primetv.ui.components.LocalCrestResolver
import androidx.compose.runtime.CompositionLocalProvider
import com.wpdas.primetv.ui.navigation.PrimeNavHost
import com.wpdas.primetv.ui.theme.PrimeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Single activity. Tela cheia, sempre horizontal (ver manifest), UI escalada para a base 1280. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var i18n: I18n
    @Inject lateinit var crests: TeamCrestRepository
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterFullscreen()
        setContent {
            PrimeTheme(i18n = i18n) {
                // null enquanto não sabemos se há lista configurada (fundo da janela cobre).
                val start by mainViewModel.startDestination.collectAsStateWithLifecycle()
                CompositionLocalProvider(LocalCrestResolver provides crests) {
                    start?.let { PrimeNavHost(startDestination = it) }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterFullscreen()
    }

    private fun enterFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
