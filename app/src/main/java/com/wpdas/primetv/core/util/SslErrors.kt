package com.wpdas.primetv.core.util

import javax.net.ssl.SSLException

/** Lê uma falha de TLS e resume a causa em português, para a mensagem de erro ser acionável. */
object SslErrors {
    /** true quando a exceção (ou alguma causa dela) é de TLS/certificado. */
    fun isTls(error: Throwable): Boolean = generateSequence(error) { it.cause }.any {
        it is SSLException || it is java.security.cert.CertificateException || it is java.security.cert.CertPathValidatorException
    }

    /** "data do aparelho atrasada", "data do aparelho adiantada", "autoridade certificadora desconhecida" ou o nome do erro. */
    fun describe(error: Throwable): String {
        val text = generateSequence(error) { it.cause }.joinToString(" ") { it.message.orEmpty() }.lowercase()
        return when {
            "not yet valid" in text || "notbefore" in text -> "data do aparelho atrasada"
            "expired" in text || "notafter" in text -> "data do aparelho adiantada"
            "trust anchor" in text || "unable to find valid certification path" in text || "unknown ca" in text -> "autoridade certificadora desconhecida"
            else -> generateSequence(error) { it.cause }.last().javaClass.simpleName
        }
    }
}
