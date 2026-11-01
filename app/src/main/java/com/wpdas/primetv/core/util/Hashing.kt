package com.wpdas.primetv.core.util

import java.security.MessageDigest

object Hashing {
    /** MD5 hexadecimal curto para gerar ids estáveis a partir de texto (não é uso criptográfico). */
    fun shortId(text: String, length: Int = 16): String {
        val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(length)
    }
}
