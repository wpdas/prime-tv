package com.wpdas.primetv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.wpdas.primetv.core.i18n.I18n
import com.wpdas.primetv.ui.navigation.PrimeNavHost
import com.wpdas.primetv.ui.theme.PrimeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Single activity. Tela cheia, sempre horizontal (ver manifest), UI escalada para a base 1280. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var i18n: I18n

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterFullscreen()
        setContent {
            PrimeTheme(i18n = i18n) {
                PrimeNavHost()
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
