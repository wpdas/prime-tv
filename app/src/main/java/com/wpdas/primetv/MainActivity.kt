package com.wpdas.primetv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity do app. Nesta fase só valida o build; o tema, a escala 1280
 * e a navegação entram na fase 1.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B0F)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "PrimeTV", color = Color(0xFFF2F2F2))
            }
        }
    }
}
