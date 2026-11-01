package com.wpdas.primetv.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.wpdas.primetv.core.di.ApplicationScope
import com.wpdas.primetv.domain.model.AppSettings
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Um PeriodicWorkRequest por seção, com o intervalo das Configurações e restrição de rede
 * (opcionalmente só rede não medida). Reagenda sozinho quando as preferências mudam.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val workManager get() = WorkManager.getInstance(context)

    /** Chame uma vez no arranque (depois de existir lista). */
    fun start() {
        settings.settings
            .map { it.syncIntervals to it.unmeteredOnly }
            .distinctUntilChanged()
            .onEach { apply(settings.settings.value) }
            .launchIn(scope)
    }

    fun apply(current: AppSettings) {
        for (section in ContentSection.entries) {
            val interval = current.interval(section)
            val name = workName(section)
            if (interval.isManual) {
                workManager.cancelUniqueWork(name)
                continue
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (current.unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(interval.hours.toLong(), TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInputData(workDataOf(SyncWorker.KEY_SECTION to section.name))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .addTag(TAG)
                .build()
            workManager.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }

    fun cancelAll() = workManager.cancelAllWorkByTag(TAG)

    private fun workName(section: ContentSection) = "sync_${section.name.lowercase()}"

    private companion object {
        const val TAG = "primetv_sync"
    }
}
