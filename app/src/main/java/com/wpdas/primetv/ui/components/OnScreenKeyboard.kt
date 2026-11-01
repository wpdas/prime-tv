package com.wpdas.primetv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wpdas.primetv.ui.theme.PrimeFocus
import com.wpdas.primetv.ui.theme.PrimeTheme

private val KEYS: List<String> = ('a'..'z').map { it.toString() } + ('0'..'9').map { it.toString() }

/**
 * Teclado para o D-pad: 7 colunas (a–z, 0–9) mais Espaço, Apagar e Limpar.
 * [extraKeys] permite acrescentar, por exemplo, "Voz".
 */
@Composable
fun OnScreenKeyboard(
    onKey: (String) -> Unit,
    onSpace: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    spaceLabel: String,
    backspaceLabel: String,
    clearLabel: String,
    modifier: Modifier = Modifier,
    firstKeyFocus: FocusRequester? = null,
    extraKeys: List<Pair<String, () -> Unit>> = emptyList(),
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        KEYS.chunked(7).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { i, key ->
                    Key(
                        label = key,
                        onClick = { onKey(key) },
                        modifier = Modifier.weight(1f).then(if (rowIndex == 0 && i == 0 && firstKeyFocus != null) Modifier.focusRequester(firstKeyFocus) else Modifier),
                    )
                }
                repeat(7 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Key(spaceLabel, onSpace, Modifier.weight(1f), tall = true)
            Key(backspaceLabel, onBackspace, Modifier.weight(1f), tall = true)
            Key(clearLabel, onClear, Modifier.weight(1f), tall = true)
            extraKeys.forEach { (label, action) -> Key(label, action, Modifier.weight(1f), tall = true) }
        }
    }
}

@Composable
private fun Key(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tall: Boolean = false) {
    val colors = PrimeTheme.colors
    val shape = PrimeTheme.shapes.small
    Surface(
        onClick = onClick,
        modifier = modifier.height(if (tall) 44.dp else 40.dp),
        shape = PrimeFocus.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (tall) colors.surface2 else colors.surfaceMenu,
            contentColor = colors.text,
            focusedContainerColor = colors.selected,
            focusedContentColor = colors.text,
            pressedContainerColor = colors.accent,
            pressedContentColor = colors.onAccent,
        ),
        scale = PrimeFocus.noScale(),
        border = PrimeFocus.border(shape),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (tall) label else label.uppercase(),
                style = if (tall) PrimeTheme.typography.buttonSmall else PrimeTheme.typography.listTitle,
                maxLines = 1,
            )
        }
    }
}
