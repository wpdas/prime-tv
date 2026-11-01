package com.wpdas.primetv.data.remote.m3u

import java.io.BufferedReader

/** Uma linha `#EXTINF` com a URL que a segue. */
data class M3uEntry(
    val name: String,
    val url: String,
    val attributes: Map<String, String>,
    val group: String? = attributes["group-title"],
) {
    val tvgId: String? get() = attributes["tvg-id"]?.takeIf { it.isNotBlank() }
    val tvgName: String? get() = attributes["tvg-name"]?.takeIf { it.isNotBlank() }
    val logo: String? get() = attributes["tvg-logo"]?.takeIf { it.isNotBlank() }
    val channelNumber: Int? get() = attributes["tvg-chno"]?.trim()?.toIntOrNull()
}

data class M3uHeader(val epgUrl: String?)

sealed interface M3uItem {
    data class Header(val header: M3uHeader) : M3uItem
    data class Entry(val entry: M3uEntry) : M3uItem
}

/**
 * Parser de M3U/M3U Plus em streaming: lê linha a linha e emite cada entrada assim que
 * encontra a URL. Tolera atributos faltando ou sem aspas, `#EXTGRP`, CRLF e linhas em branco.
 */
object M3uParser {

    fun parse(reader: BufferedReader): Sequence<M3uItem> = sequence {
        var pendingName: String? = null
        var pendingAttrs: Map<String, String> = emptyMap()
        var pendingGroup: String? = null
        var headerEmitted = false
        var first = true

        while (true) {
            val raw = reader.readLine() ?: break
            val line = if (first) raw.trimStart('﻿').trim() else raw.trim()
            first = false
            if (line.isEmpty()) continue

            when {
                line.startsWith("#EXTM3U", ignoreCase = true) -> {
                    val attrs = parseAttributes(line.substringAfter("#EXTM3U").trim())
                    val epg = attrs["url-tvg"] ?: attrs["x-tvg-url"] ?: attrs["tvg-url"]
                    yield(M3uItem.Header(M3uHeader(epg?.split(',')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() })))
                    headerEmitted = true
                }
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val body = line.substringAfter(':', "")
                    val (attrPart, name) = splitAttributesAndName(body)
                    pendingAttrs = parseAttributes(attrPart)
                    pendingName = name.ifBlank { pendingAttrs["tvg-name"] ?: "" }
                    // #EXTGRP pode vir antes ou depois do #EXTINF; group-title tem prioridade.
                    pendingGroup = pendingAttrs["group-title"] ?: pendingGroup
                }
                line.startsWith("#EXTGRP", ignoreCase = true) -> {
                    pendingGroup = line.substringAfter(':', "").trim().ifEmpty { null }
                }
                line.startsWith("#") -> Unit // #EXTVLCOPT, comentários etc.
                else -> {
                    if (!headerEmitted) {
                        yield(M3uItem.Header(M3uHeader(null)))
                        headerEmitted = true
                    }
                    val name = pendingName
                    if (name != null) {
                        yield(M3uItem.Entry(M3uEntry(name = name, url = line, attributes = pendingAttrs, group = pendingGroup)))
                    }
                    pendingName = null
                    pendingAttrs = emptyMap()
                    pendingGroup = null
                }
            }
        }
    }

    /**
     * Separa `-1 tvg-id="a,b" group-title="X",Nome do canal` em
     * (`-1 tvg-id="a,b" group-title="X"`, `Nome do canal`): a vírgula que separa o nome
     * é a primeira fora de aspas.
     */
    internal fun splitAttributesAndName(body: String): Pair<String, String> {
        var inQuotes = false
        for (i in body.indices) {
            when (body[i]) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) return body.substring(0, i) to body.substring(i + 1).trim()
            }
        }
        return body to ""
    }

    private val ATTR = Regex("""([A-Za-z0-9_\-]+)=(?:"([^"]*)"|([^\s"]+))""")

    internal fun parseAttributes(text: String): Map<String, String> {
        if (text.isEmpty()) return emptyMap()
        val map = LinkedHashMap<String, String>()
        for (m in ATTR.findAll(text)) {
            val key = m.groupValues[1].lowercase()
            val value = m.groupValues[2].ifEmpty { m.groupValues[3] }
            map[key] = value
        }
        return map
    }
}
