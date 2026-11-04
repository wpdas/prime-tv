package com.wpdas.primetv.data.link

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/** API do site "Receber link" (repo prime-tv-web). Caminhos relativos à origem do site. */
interface LinkApi {
    @POST("api/sessions")
    suspend fun createSession(): LinkSessionResponse

    @GET("api/sessions/{code}")
    suspend fun poll(@Path("code") code: String, @Header("Authorization") authorization: String): LinkStatusResponse

    @DELETE("api/sessions/{code}")
    suspend fun release(@Path("code") code: String, @Header("Authorization") authorization: String): Response<Unit>
}

@Serializable
data class LinkSessionResponse(val code: String, val deviceToken: String, val expiresAt: Long, val siteUrl: String? = null)

@Serializable
data class LinkStatusResponse(val status: String, val url: String? = null, val expiresAt: Long? = null) {
    companion object {
        const val PENDING = "pending"
        const val DELIVERED = "delivered"
        const val EXPIRED = "expired"
    }
}

/** Corpo de erro do site: `{ error, message }`, com [message] em pt-BR e uma ação. */
@Serializable
data class LinkErrorResponse(val error: String? = null, val message: String? = null)

/** Endereços do site a partir de `LINK_SITE_URL` (`https://host/link`). */
object LinkSites {
    /** Origem com barra final, base da API: `https://host/link` -> `https://host/`. */
    fun origin(siteUrl: String): String {
        val schemeEnd = siteUrl.indexOf("://").takeIf { it >= 0 }?.plus(3) ?: return siteUrl.trimEnd('/') + "/"
        val hostEnd = siteUrl.indexOfAny(charArrayOf('/', '?', '#'), schemeEnd).takeIf { it >= 0 } ?: siteUrl.length
        return siteUrl.substring(0, hostEnd) + "/"
    }

    /** URL do QR: o site com o código já preenchido. */
    fun qrUrl(siteUrl: String, code: String): String = "$siteUrl?code=$code"
}
