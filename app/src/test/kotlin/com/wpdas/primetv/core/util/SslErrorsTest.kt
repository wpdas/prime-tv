package com.wpdas.primetv.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import javax.net.ssl.SSLHandshakeException

class SslErrorsTest {
    @Test fun `reconhece erro de TLS em qualquer nivel da causa`() {
        assertTrue(SslErrors.isTls(SSLHandshakeException("x")))
        assertTrue(SslErrors.isTls(RuntimeException(CertificateException("y"))))
        assertFalse(SslErrors.isTls(java.io.IOException("socket closed")))
    }

    @Test fun `resume a causa em portugues`() {
        fun handshake(message: String, cause: Throwable? = null) = SSLHandshakeException(message).apply { cause?.let { initCause(it) } }
        assertEquals("data do aparelho atrasada", SslErrors.describe(handshake("Chain validation failed", CertificateException("certificate not yet valid"))))
        assertEquals("data do aparelho adiantada", SslErrors.describe(handshake("certificate expired at NotAfter: ...")))
        assertEquals("autoridade certificadora desconhecida", SslErrors.describe(handshake("Chain validation failed", CertPathValidatorException("Trust anchor for certification path not found."))))
        assertEquals("CertificateException", SslErrors.describe(handshake("x", CertificateException("algo"))))
    }
}
