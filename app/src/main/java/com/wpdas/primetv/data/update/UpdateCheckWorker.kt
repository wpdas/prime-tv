package com.wpdas.primetv.data.update

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wpdas.primetv.domain.update.UpdateCheck
import com.wpdas.primetv.domain.update.UpdateRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Checagem diária silenciosa: só grava a versão nova; a UI avisa quando houver. */
@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val updates: UpdateRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (updates.check()) {
        is UpdateCheck.Failed -> if (runAttemptCount < 2) Result.retry() else Result.success()
        else -> Result.success()
    }

    companion object {
        private const val NAME = "update_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
