package com.wpdas.primetv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.tv.material3.Text
import com.wpdas.primetv.ui.theme.PrimeTheme
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val HourMinute: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Relógio HH:mm que atualiza na virada de cada minuto. */
@Composable
fun Clock(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(60_000L - (now.second * 1000L + now.nano / 1_000_000L))
        }
    }
    Text(now.format(HourMinute), style = PrimeTheme.typography.clock, color = PrimeTheme.colors.textSecondary, modifier = modifier)
}
