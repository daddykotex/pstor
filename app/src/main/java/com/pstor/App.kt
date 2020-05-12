package com.pstor

import android.app.Application
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit


/**
 * The [Application]. Responsible for initializing [WorkManager] in [Log.VERBOSE] mode.
 */
class App : Application(), Configuration.Provider {
    init {
        Log.i("PSTOR", "sauce")
    }
    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setMinimumLoggingLevel(Log.VERBOSE)
            .build()

    override fun onCreate() {
        super.onCreate()

        val uploadWorkRequest = PeriodicWorkRequestBuilder<BackgroundFileScannerWorker>(15, TimeUnit.MINUTES).build()
        Log.i("PSTOR", "Registered BackgroundFileScannerWorker for work.")

        WorkManager.getInstance(this).enqueue(uploadWorkRequest)

    }
}
