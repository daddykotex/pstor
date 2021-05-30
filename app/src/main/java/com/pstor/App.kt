package com.pstor

import android.app.Application
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.minutes


class App : Application(), Configuration.Provider {

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setMinimumLoggingLevel(Log.VERBOSE)
            .build()

    @ExperimentalTime
    override fun onCreate() {
        super.onCreate()

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

        val existingWorkPolicy = ExistingPeriodicWorkPolicy.REPLACE

        fun <T: ListenableWorker> registerPeriodicWorker(clazz: Class<T>, repeatInterval: Duration, initialDelay: Duration) {
            val pReq =
                PeriodicWorkRequest.Builder(clazz, repeatInterval.toLong(TimeUnit.SECONDS), TimeUnit.SECONDS)
                    .setInitialDelay(initialDelay.toLong(TimeUnit.SECONDS), TimeUnit.SECONDS)
                    .setConstraints(constraints)
                    .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                clazz.javaClass.simpleName,
                existingWorkPolicy,
                pReq
            )
        }

        registerPeriodicWorker(BackgroundFileScannerWorker::class.java, 15.minutes, 1.minutes)
        registerPeriodicWorker(BackgroundFileUploaderWorker::class.java, 15.minutes, 2.minutes)

        registerPeriodicWorker(BackgroundFileToDeleteWorker::class.java, 60.minutes, 5.minutes)
    }
}
