package com.wpdas.primetv.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class SourceType(val labelKey: String) {
    XTREAM("source.xtream"),
    M3U("source.m3u"),
}

/** Lista configurada pelo usuário. Persistida criptografada; nunca vai para log. */
@Serializable
sealed interface PlaylistSource {
    val originalUrl: String
    val type: SourceType

    @Serializable
    @SerialName("xtream")
    data class Xtream(
        override val originalUrl: String,
        val baseUrl: String,
        val username: String,
        val password: String,
    ) : PlaylistSource {
        override val type: SourceType get() = SourceType.XTREAM
    }

    @Serializable
    @SerialName("m3u")
    data class M3u(
        override val originalUrl: String,
        val url: String,
        val epgUrl: String? = null,
    ) : PlaylistSource {
        override val type: SourceType get() = SourceType.M3U
    }
}

data class DetectionResult(val source: PlaylistSource, val hasEpg: Boolean)

/** Erros ao validar/baixar a lista. Cada um tem mensagem em português e uma ação na UI. */
sealed interface PlaylistError {
    val messageKey: String

    data object InvalidUrl : PlaylistError { override val messageKey = "error.invalid_url" }
    data object NoInternet : PlaylistError { override val messageKey = "error.no_internet" }
    data object ServerUnreachable : PlaylistError { override val messageKey = "error.server_unreachable" }
    data object InvalidCredentials : PlaylistError { override val messageKey = "error.invalid_credentials" }
    data object InvalidPlaylist : PlaylistError { override val messageKey = "error.invalid_playlist" }
    data class Unknown(val detail: String) : PlaylistError { override val messageKey = "error.unknown" }
}

class PlaylistException(val error: PlaylistError, cause: Throwable? = null) : Exception(error.toString(), cause)
