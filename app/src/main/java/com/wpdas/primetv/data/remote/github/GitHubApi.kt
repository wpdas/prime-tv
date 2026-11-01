package com.wpdas.primetv.data.remote.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubApi {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun latestRelease(@Path("owner") owner: String, @Path("repo") repo: String): GitHubRelease

    companion object {
        const val BASE_URL = "https://api.github.com/"
    }
}

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<GitHubAsset> = emptyList(),
) {
    /** O APK universal da release: prefere o nome `primetv-*.apk`, senão o primeiro `.apk`. */
    val apk: GitHubAsset? get() = assets.firstOrNull { it.name.startsWith("primetv", true) && it.name.endsWith(".apk", true) }
        ?: assets.firstOrNull { it.name.endsWith(".apk", true) }
}

@Serializable
data class GitHubAsset(
    val name: String,
    val size: Long = 0,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("content_type") val contentType: String? = null,
)
