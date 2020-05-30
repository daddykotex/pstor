package com.pstor

import android.app.Application
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit


/**
 * The [Application]. Responsible for initializing [WorkManager] in [Log.VERBOSE] mode.
 */
class App : Application(), Configuration.Provider {
    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setMinimumLoggingLevel(Log.VERBOSE)
            .build()

    override fun onCreate() {
        super.onCreate()

        Log.d("Pstor", "onCreate called")

        val fileScannerWorker =
            PeriodicWorkRequestBuilder<BackgroundFileScannerWorker>(15, TimeUnit.MINUTES).build()
        val fileUploaderWorker =
            PeriodicWorkRequestBuilder<BackgroundFileUploaderWorker>(30, TimeUnit.MINUTES).build()

        val existingWorkPolicy = ExistingPeriodicWorkPolicy.REPLACE

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            BackgroundFileScannerWorker::class.java.simpleName,
            existingWorkPolicy,
            fileScannerWorker
        )
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            BackgroundFileUploaderWorker::class.java.simpleName,
            existingWorkPolicy,
            fileUploaderWorker
        )
    }
}
