package com.stocktracker.wear

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.stocktracker.wear.worker.RefreshQuotesScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class StockTrackerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.d("StockTrackerApplication starting")

        WorkManager.initialize(this, getWorkManagerConfiguration())
        Timber.d("WorkManager initialized")

        RefreshQuotesScheduler.schedule(this)
        Timber.d("RefreshQuotesScheduler registered")
    }
}
