package com.wpdas.primetv

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.wpdas.primetv.core.i18n.I18n
import com.wpdas.primetv.data.sync.SyncScheduler
import com.wpdas.primetv.data.update.UpdateCheckWorker
import com.wpdas.primetv.domain.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PrimeTvApp : Application(), Configuration.Provider, coil3.SingletonImageLoader.Factory {

    @Inject lateinit var i18n: I18n
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Arquivo pequeno; carregar antes da primeira tela evita mostrar chaves cruas.
        i18n.loadBlocking(settings.settings.value.language)
        syncScheduler.start()
        UpdateCheckWorker.schedule(this)
    }

    /**
     * Imagens: só as que estão na tela (e a próxima de cada linha, pelo prefetch das listas) são
     * carregadas; o cache em memória fica pequeno para não disputar RAM com o vídeo nas boxes, e o
     * de disco tem teto para não lotar o armazenamento interno.
     */
    override fun newImageLoader(context: android.content.Context): coil3.ImageLoader {
        val lowRam = (getSystemService(ACTIVITY_SERVICE) as? android.app.ActivityManager)?.isLowRamDevice == true
        return coil3.ImageLoader.Builder(context)
            .memoryCache {
                coil3.memory.MemoryCache.Builder()
                    .maxSizePercent(context, if (lowRam) 0.08 else 0.15)
                    .build()
            }
            .diskCache {
                coil3.disk.DiskCache.Builder()
                    .directory(okio.Path.Companion.run { context.cacheDir.resolve("images").absolutePath.toPath() })
                    .maxSizeBytes(if (lowRam) 64L * 1024 * 1024 else 160L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}
