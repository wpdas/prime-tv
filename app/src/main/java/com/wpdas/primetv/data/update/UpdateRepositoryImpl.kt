package com.wpdas.primetv.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wpdas.primetv.BuildConfig
import com.wpdas.primetv.core.di.IoDispatcher
import com.wpdas.primetv.core.util.SemVer
import com.wpdas.primetv.core.util.SslErrors
import com.wpdas.primetv.data.local.prefs.appDataStore
import com.wpdas.primetv.data.remote.github.GitHubApi
import com.wpdas.primetv.domain.update.AppUpdate
import com.wpdas.primetv.domain.update.DownloadProgress
import com.wpdas.primetv.domain.update.UpdateCheck
import com.wpdas.primetv.domain.update.UpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.io.IOException
import java.net.UnknownHostException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Atualização pelo GitHub Releases: compara a tag mais recente com a versão instalada,
 * baixa o APK para o cache com progresso e dispara a instalação pelo sistema.
 */
@Singleton
class UpdateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: OkHttpClient,
    json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) : UpdateRepository {

    private val api: GitHubApi = Retrofit.Builder()
        .baseUrl(GitHubApi.BASE_URL)
        .client(http)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GitHubApi::class.java)

    private val jsonCodec = json
    private val owner = BuildConfig.GITHUB_REPO.substringBefore('/')
    private val repo = BuildConfig.GITHUB_REPO.substringAfter('/')

    override val pending: Flow<AppUpdate?> = context.appDataStore.data.map { prefs ->
        prefs[KEY_PENDING]?.let { raw -> runCatching { jsonCodec.decodeFromString(PendingUpdate.serializer(), raw).toDomain() }.getOrNull() }
            ?.takeIf { SemVer.isNewer(it.versionName, BuildConfig.VERSION_NAME) }
    }

    override suspend fun checkIfDue(minIntervalMs: Long) {
        val last = runCatching { context.appDataStore.data.map { it[KEY_LAST_CHECK] ?: 0L }.first() }.getOrDefault(0L)
        if (System.currentTimeMillis() - last < minIntervalMs) return
        // Só conta como checado quando o servidor respondeu: falha (rede ainda subindo na abertura,
        // por exemplo) não pode travar a próxima tentativa por meia hora.
        val result = runCatching { check() }.getOrNull()
        if (result != null && result !is UpdateCheck.Failed) {
            context.appDataStore.edit { it[KEY_LAST_CHECK] = System.currentTimeMillis() }
        }
    }

    override suspend fun check(): UpdateCheck = withContext(io) {
        val release = try {
            api.latestRelease(owner, repo)
        } catch (e: UnknownHostException) {
            return@withContext UpdateCheck.Failed("update.error_offline")
        } catch (e: javax.net.ssl.SSLException) {
            return@withContext UpdateCheck.Failed("update.error_ssl", SslErrors.describe(e))
        } catch (e: retrofit2.HttpException) {
            return@withContext if (e.code() == 404) UpdateCheck.UpToDate else UpdateCheck.Failed("update.error_server", "HTTP ${e.code()}")
        } catch (e: IOException) {
            return@withContext UpdateCheck.Failed("update.error_server", e.javaClass.simpleName)
        } catch (e: Exception) {
            return@withContext UpdateCheck.Failed("update.error_server", e.javaClass.simpleName)
        }
        if (release.draft || release.prerelease) return@withContext UpdateCheck.UpToDate
        val apk = release.apk
        if (apk == null || !SemVer.isNewer(release.tagName, BuildConfig.VERSION_NAME)) {
            context.appDataStore.edit { it.remove(KEY_PENDING) }
            return@withContext UpdateCheck.UpToDate
        }
        val update = AppUpdate(
            versionName = SemVer.parse(release.tagName)?.toString() ?: release.tagName.removePrefix("v"),
            tag = release.tagName,
            title = release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
            changelog = release.body.orEmpty().trim(),
            apkUrl = apk.downloadUrl,
            apkSizeBytes = apk.size,
            publishedAt = release.publishedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
        )
        context.appDataStore.edit { it[KEY_PENDING] = jsonCodec.encodeToString(PendingUpdate.serializer(), PendingUpdate.from(update)) }
        UpdateCheck.Available(update)
    }

    override fun download(update: AppUpdate): Flow<DownloadProgress> = flow {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "primetv-${update.tag}.apk")
        val tmp = File(dir, "${target.name}.part")
        try {
            http.newCall(Request.Builder().url(update.apkUrl).get().build()).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadProgress.Failed("update.error_download", "HTTP ${response.code}"))
                    return@flow
                }
                val total = response.body.contentLength().takeIf { it > 0 } ?: update.apkSizeBytes
                var downloaded = 0L
                var lastEmit = 0L
                response.body.byteStream().use { input ->
                    tmp.outputStream().use { out ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                            downloaded += read
                            val now = System.currentTimeMillis()
                            if (now - lastEmit > 200) {
                                lastEmit = now
                                emit(DownloadProgress.Running(downloaded, total))
                            }
                        }
                    }
                }
                emit(DownloadProgress.Running(downloaded, total))
            }
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
            emit(DownloadProgress.Done(target))
        } catch (e: UnknownHostException) {
            emit(DownloadProgress.Failed("update.error_offline"))
        } catch (e: IOException) {
            tmp.delete()
            // Falha de TLS (relógio errado ou autoridade desconhecida na box) ganha mensagem própria.
            if (SslErrors.isTls(e)) emit(DownloadProgress.Failed("update.error_ssl", SslErrors.describe(e)))
            else emit(DownloadProgress.Failed("update.error_download", e.javaClass.simpleName))
        }
    }.flowOn(io)

    override fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    override fun openInstallPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    override fun openDateSettings() {
        val intent = Intent(Settings.ACTION_DATE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    override fun install(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    @kotlinx.serialization.Serializable
    private data class PendingUpdate(
        val versionName: String, val tag: String, val title: String, val changelog: String,
        val apkUrl: String, val apkSizeBytes: Long, val publishedAt: Long?,
    ) {
        fun toDomain() = AppUpdate(versionName, tag, title, changelog, apkUrl, apkSizeBytes, publishedAt)
        companion object {
            fun from(u: AppUpdate) = PendingUpdate(u.versionName, u.tag, u.title, u.changelog, u.apkUrl, u.apkSizeBytes, u.publishedAt)
        }
    }

    private companion object {
        val KEY_PENDING = stringPreferencesKey("pending_update")
        val KEY_LAST_CHECK = androidx.datastore.preferences.core.longPreferencesKey("update_last_check")
    }
}
