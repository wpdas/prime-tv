package com.wpdas.primetv.data.remote.epg

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class XmlTvChannel(val id: String, val displayName: String, val icon: String?)
data class XmlTvProgramme(val channel: String, val start: Long, val end: Long, val title: String, val description: String?)

sealed interface XmlTvItem {
    data class Channel(val channel: XmlTvChannel) : XmlTvItem
    data class Programme(val programme: XmlTvProgramme) : XmlTvItem
}

/**
 * Parser XMLTV em streaming (pull parser). Emite canais e programas à medida que lê,
 * sem montar a árvore de um arquivo que pode ter dezenas de MB.
 */
object XmlTvParser {
    private val DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    fun parse(input: InputStream): Sequence<XmlTvItem> = sequence {
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }.newPullParser()
        parser.setInput(input, null)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "channel" -> readChannel(parser)?.let { yield(XmlTvItem.Channel(it)) }
                    "programme" -> readProgramme(parser)?.let { yield(XmlTvItem.Programme(it)) }
                }
            }
            event = parser.next()
        }
    }

    private fun readChannel(parser: XmlPullParser): XmlTvChannel? {
        val id = parser.getAttributeValue(null, "id").orEmpty()
        var name: String? = null
        var icon: String? = null
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (parser.name) {
                        "display-name" -> if (name == null) name = readText(parser).also { depth-- }
                        "icon" -> icon = parser.getAttributeValue(null, "src")
                    }
                }
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return null
            }
        }
        val display = name?.trim().orEmpty()
        if (display.isEmpty()) return null
        return XmlTvChannel(id = id.ifBlank { display }, displayName = display, icon = icon)
    }

    private fun readProgramme(parser: XmlPullParser): XmlTvProgramme? {
        val channel = parser.getAttributeValue(null, "channel").orEmpty()
        val start = parseTime(parser.getAttributeValue(null, "start"))
        val stop = parseTime(parser.getAttributeValue(null, "stop"))
        var title: String? = null
        var desc: String? = null
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (parser.name) {
                        "title" -> if (title == null) title = readText(parser).also { depth-- }
                        "desc" -> if (desc == null) desc = readText(parser).also { depth-- }
                    }
                }
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return null
            }
        }
        if (channel.isBlank() || start == null || stop == null || stop <= start) return null
        val t = title?.trim().orEmpty()
        if (t.isEmpty()) return null
        return XmlTvProgramme(channel, start, stop, t, desc?.trim()?.ifEmpty { null })
    }

    /** Lê o texto até o END_TAG correspondente (consome o END_TAG). */
    private fun readText(parser: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.TEXT -> sb.append(parser.text)
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return sb.toString()
            }
        }
        return sb.toString()
    }

    /** "20261101163000 -0300" ou "20261101163000" (assume UTC). Devolve epoch millis. */
    internal fun parseTime(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.trim().split(' ', limit = 2)
        val digits = parts[0].padEnd(14, '0').take(14)
        val local = runCatching { LocalDateTime.parse(digits, DATE) }.getOrNull() ?: return null
        val offset = parts.getOrNull(1)?.let { parseOffset(it) } ?: ZoneOffset.UTC
        return local.toEpochSecond(offset) * 1000
    }

    private fun parseOffset(text: String): ZoneOffset? = runCatching {
        val t = text.trim()
        if (t.length == 5 && (t[0] == '+' || t[0] == '-')) ZoneOffset.of("${t.substring(0, 3)}:${t.substring(3)}")
        else ZoneOffset.of(t)
    }.getOrNull()
}
