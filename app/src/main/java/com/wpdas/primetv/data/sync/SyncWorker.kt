package com.wpdas.primetv.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.SyncState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Trabalho periódico de uma seção. A rede é garantida pelas restrições do agendamento. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncer: CatalogSyncer,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val section = inputData.getString(KEY_SECTION)?.let { name -> ContentSection.entries.firstOrNull { it.name == name } }
            ?: return Result.failure()
        return when (syncer.runSectionNow(section)) {
            is SyncState.Done, is SyncState.Skipped -> Result.success()
            is SyncState.Error -> if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            else -> Result.success()
        }
    }

    companion object {
        const val KEY_SECTION = "section"
        private const val MAX_RETRIES = 2
    }
}
