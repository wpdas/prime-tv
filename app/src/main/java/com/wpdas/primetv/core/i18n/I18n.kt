package com.wpdas.primetv.core.i18n

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Textos da UI carregados de `assets/i18n/<idioma>.json` (chaves planas).
 *
 * - [DEFAULT_LANGUAGE] é sempre carregado e serve de fallback para chaves ausentes.
 * - Placeholders no formato `{nome}` são substituídos por [t].
 * - [source] abstrai de onde o JSON vem, para no futuro aceitar arquivos baixados.
 */
class I18n(
    private val source: I18nSource,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private var fallback: Map<String, String> = emptyMap()
    private val _strings = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _language = MutableStateFlow(DEFAULT_LANGUAGE)

    /** Mapa atual (idioma escolhido sobre o fallback). Observe para recompor ao trocar idioma. */
    val strings: StateFlow<Map<String, String>> = _strings.asStateFlow()
    val language: StateFlow<String> = _language.asStateFlow()

    /** Carrega o idioma de forma síncrona. Só para o arranque do app (arquivo pequeno). */
    fun loadBlocking(language: String = DEFAULT_LANGUAGE) {
        if (fallback.isEmpty()) fallback = parse(source.read(DEFAULT_LANGUAGE))
        val chosen = if (language == DEFAULT_LANGUAGE) emptyMap() else parse(source.read(language))
        _strings.value = fallback + chosen
        _language.value = language
    }

    /** Idem, para uso em coroutines (troca de idioma nas Configurações). */
    suspend fun load(language: String) = loadBlocking(language)

    fun t(key: String, vararg args: Pair<String, Any>): String =
        format(_strings.value[key] ?: fallback[key] ?: key, args)

    fun availableLanguages(): List<String> = source.available()

    private fun parse(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return json.parseToJsonElement(raw).jsonObject
            .mapNotNull { (k, v) -> (v as? JsonPrimitive)?.content?.let { k to it } }
            .toMap()
    }

    companion object {
        const val DEFAULT_LANGUAGE = "pt-BR"

        fun format(template: String, args: Array<out Pair<String, Any>>): String {
            if (args.isEmpty()) return template
            var out = template
            for ((name, value) in args) out = out.replace("{$name}", value.toString())
            return out
        }
    }
}

/** De onde vêm os JSONs de tradução. */
interface I18nSource {
    /** Conteúdo bruto do JSON ou null se o idioma não existir. */
    fun read(language: String): String?
    fun available(): List<String>
}

class AssetsI18nSource(private val context: Context) : I18nSource {
    override fun read(language: String): String? = runCatching {
        context.assets.open("$DIR/$language.json").bufferedReader().use { it.readText() }
    }.getOrNull()

    override fun available(): List<String> =
        context.assets.list(DIR).orEmpty().filter { it.endsWith(".json") }.map { it.removeSuffix(".json") }

    private companion object {
        const val DIR = "i18n"
    }
}
