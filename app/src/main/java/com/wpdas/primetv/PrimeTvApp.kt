package com.wpdas.primetv

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.wpdas.primetv.core.i18n.I18n
import com.wpdas.primetv.data.sync.SyncScheduler
import com.wpdas.primetv.domain.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PrimeTvApp : Application(), Configuration.Provider {

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
    }
}
