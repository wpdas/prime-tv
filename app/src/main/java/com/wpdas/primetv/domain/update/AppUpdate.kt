package com.wpdas.primetv.domain.update

import kotlinx.coroutines.flow.Flow
import java.io.File

data class AppUpdate(
    val versionName: String,
    val tag: String,
    val title: String,
    val changelog: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val publishedAt: Long?,
)

sealed interface UpdateCheck {
    data class Available(val update: AppUpdate) : UpdateCheck
    data object UpToDate : UpdateCheck
    data class Failed(val messageKey: String, val detail: String? = null) : UpdateCheck
}

sealed interface DownloadProgress {
    data class Running(val downloadedBytes: Long, val totalBytes: Long) : DownloadProgress {
        val fraction: Float get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    }
    data class Done(val file: File) : DownloadProgress
    data class Failed(val messageKey: String, val detail: String? = null) : DownloadProgress
}

interface UpdateRepository {
    /** Última verificação com versão nova, se houver (preenchida pela checagem diária). */
    val pending: Flow<AppUpdate?>

    suspend fun check(): UpdateCheck
    /** Checagem silenciosa ao abrir o app, no máximo uma a cada [minIntervalMs]. */
    suspend fun checkIfDue(minIntervalMs: Long = 6 * 60 * 60 * 1000L)
    fun download(update: AppUpdate): Flow<DownloadProgress>

    /** O Android permite instalar APKs vindos deste app? Falso leva à tela de permissão. */
    fun canInstall(): Boolean
    fun openInstallPermission()
    fun install(file: File)
}
