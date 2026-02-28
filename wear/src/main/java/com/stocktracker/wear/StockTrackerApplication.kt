package com.stocktracker.wear

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.stocktracker.wear.worker.RefreshQuotesScheduler
import dagger.hilt.android.HiltAndroidApp
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
        WorkManager.initialize(this, getWorkManagerConfiguration())
        RefreshQuotesScheduler.schedule(this)
    }
}
