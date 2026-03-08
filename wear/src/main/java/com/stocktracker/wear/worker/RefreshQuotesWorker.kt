package com.stocktracker.wear.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stocktracker.wear.data.StockRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class RefreshQuotesWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: StockRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Timber.d("RefreshQuotesWorker: starting (attempt %d)", runAttemptCount + 1)
        return try {
            repository.clearStaleCache()
            repository.refreshQuotes(force = false)
            Timber.d("RefreshQuotesWorker: completed successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.w(e, "RefreshQuotesWorker: failed, scheduling retry")
            Result.retry()
        }
    }
}

