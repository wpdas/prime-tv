package com.wpdas.primetv.data.remote.xtream

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Painéis Xtream misturam tipos no mesmo campo (ids como número ou string, rating como
 * "7.1" ou 7.1, null onde se espera texto). Este serializer aceita qualquer primitivo
 * e devolve String?, deixando a conversão para o mapper.
 */
object LenientString : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val input = decoder as? JsonDecoder ?: return decoder.decodeString()
        return when (val element = input.decodeJsonElement()) {
            is JsonNull -> null
            is JsonPrimitive -> element.content
            else -> element.toString()
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value)
    }
}

internal fun String?.toIntLenient(): Int? = this?.trim()?.toDoubleOrNull()?.toInt()
internal fun String?.toLongLenient(): Long? = this?.trim()?.toDoubleOrNull()?.toLong()
internal fun String?.toDoubleLenient(): Double? = this?.trim()?.toDoubleOrNull()
internal fun String?.toBoolLenient(): Boolean = this?.trim() in setOf("1", "true", "yes")
internal fun String?.blankToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
internal fun String?.yearLenient(): Int? = this?.trim()?.take(4)?.toIntOrNull()?.takeIf { it in 1900..2100 }
internal fun JsonPrimitive?.contentOrNull(): String? = this?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
