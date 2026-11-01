package com.wpdas.primetv.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

val LocalI18n = staticCompositionLocalOf<I18n> { error("I18n não fornecido. Envolva a UI com PrimeTheme.") }

/**
 * Traduz uma chave dentro de uma composição. Lê o StateFlow de strings, então
 * trocar o idioma recompõe tudo que usa [t].
 */
@Composable
fun t(key: String, vararg args: Pair<String, Any>): String {
    val i18n = LocalI18n.current
    val strings by i18n.strings.collectAsState()
    return I18n.format(strings[key] ?: key, args)
}

/** Versão sem observação, para lambdas não composables dentro de telas (ex.: contentDescription estáticos). */
@Composable
@ReadOnlyComposable
fun i18n(): I18n = LocalI18n.current
