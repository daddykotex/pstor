package com.pstor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.work.*
import com.pstor.App.Companion.Notification.ChannelId
import java.util.concurrent.TimeUnit


class App : Application(), Configuration.Provider {

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setMinimumLoggingLevel(Log.VERBOSE)
            .build()

    override fun onCreate() {
        super.onCreate()

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()
        val fileScannerWorker =
            PeriodicWorkRequestBuilder<BackgroundFileScannerWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
        val fileUploaderWorker =
            PeriodicWorkRequestBuilder<BackgroundFileUploaderWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

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

        createNotificationChannel()
    }


    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val name = this.getString(R.string.notitication_channel_name)
        val descriptionText = this.getString(R.string.notitication_channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(ChannelId, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        object Notification {
            const val ChannelId = "com.pstor"
            const val ProgressNotificationId = 150
        }
    }
}
