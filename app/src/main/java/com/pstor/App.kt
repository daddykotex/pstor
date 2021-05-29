package com.pstor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.work.*
import com.pstor.App.Companion.Notification.ChannelId
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
