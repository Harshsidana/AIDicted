package com.ai.aidicted

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ai.aidicted.notification.NewsRefreshWorker
import com.ai.aidicted.notification.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class AIDictedApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Schedule periodic news refresh (every 1 hour, only when connected)
        scheduleNewsRefresh()
    }

    private fun scheduleNewsRefresh() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val refreshWork = PeriodicWorkRequestBuilder<NewsRefreshWorker>(
            1, TimeUnit.HOURS  // Check for new articles every hour
        )
            .setConstraints(constraints)
            .setInitialDelay(5, TimeUnit.MINUTES) // First run 5 min after app start (faster for testing)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            NewsRefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Don't restart if already scheduled
            refreshWork
        )
    }
}
