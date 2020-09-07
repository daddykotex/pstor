package com.pstor

import android.app.Notification
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.backblaze.b2.client.structures.B2AccountAuthorization
import com.backblaze.b2.client.structures.B2FileVersion
import com.backblaze.b2.client.structures.B2UploadUrlResponse
import com.backblaze.b2.json.B2Json
import com.backblaze.b2.json.B2Json.constructor
import com.backblaze.b2.json.B2Json.required
import com.pstor.App.Companion.Notification.ChannelId
import com.pstor.App.Companion.Notification.ProgressNotificationId
import com.pstor.b2.OkHttpB2CredentialsClient
import com.pstor.b2.OkHttpB2FileClient
import com.pstor.cache.PreferenceCache
import com.pstor.db.PStorDatabase
import com.pstor.db.files.Queue
import com.pstor.preferences.Keys
import com.pstor.preferences.SecurePreference
import com.pstor.utils.Either
import okio.source
import java.io.FileNotFoundException

class BackgroundFileUploaderWorker(private val appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    private val tag = this.javaClass.simpleName

    private val db: PStorDatabase = PStorDatabase.getDatabase(appContext)

    private val securePreference: SecurePreference = SecurePreference.load(appContext)
    private val preferenceCache = PreferenceCache(securePreference)

    override fun onStopped() {
        super.onStopped()
        with(NotificationManagerCompat.from(appContext)) {
            cancel(ProgressNotificationId)
        }
    }

    override fun doWork(): Result {
        Log.i(tag, "Starting work.")

        Log.i(tag, "Checking permissions.")
        if (!Permissions.checkAllPermissions(appContext)) {
            return Result.failure(
                Data.Builder().putString(
                    "error",
                    "not all permissions"
                ).build()
            )
        }

        Log.i(tag, "Checking credentials to B2")
        val credentials =
            B2Credentials.loadFromPreferences(securePreference) ?: return Result.failure(
                Data.Builder().putString(
                    "error",
                    "credentials unavailable"
                ).build()
            )

        val bucketId =
            securePreference.get(Keys.BucketId) ?: return Result.failure(
                Data.Builder().putString(
                    "error",
                    "bucket id unavailable"
                ).build()
            )

        val auth: B2AccountAuthorization
        when (val maybeAuth = preferenceCache.getAuth(credentials)) {
            is Either.Left -> return Result.failure(errorResult(maybeAuth.left.message))
            is Either.Right -> auth = maybeAuth.right
        }
        Log.i(tag, "Authorized.")


        val maybeFileUrl = preferenceCache.getFileUrl(auth, bucketId)
        val fileUrlResponse: B2UploadUrlResponse
        when (maybeFileUrl) {
            is Either.Left -> return Result.failure(errorResult(maybeFileUrl.left.message))
            is Either.Right -> fileUrlResponse = maybeFileUrl.right
        }
        Log.i(tag, "Successfully retrieved an URL to start uploading: ${fileUrlResponse.uploadUrl}")

        val queueItems = db.queueDAO().findByStatus(ImageStatus.IN_QUEUE.toString(), Queue.AttemptCountLimit, 50)
        Log.i(tag, "Processing with ${credentials.key}, images to process: ${queueItems.size}")


        fun upload(queue: List<Queue>) {
            with(NotificationManagerCompat.from(appContext)) {
                notify(ProgressNotificationId, buildNotification(0, queue.size))
            }

            queue.forEachIndexed { index, q ->
                try {
                    if (!checkIfFileExists(auth, q)) {
                        val result =
                            uploadOne(
                                fileUrlResponse.authorizationToken,
                                fileUrlResponse.uploadUrl,
                                q
                            )

                        with(NotificationManagerCompat.from(appContext)) {
                            notify(ProgressNotificationId, buildNotification(index, queue.size))
                        }

                        if (result != null) {
                            Log.d(
                                tag,
                                "Upload successful of ${q.fileName} with a size of ${result.contentLength}"
                            )
                            val updated = q.copy(status = ImageStatus.UPLOADED.toString())
                            db.queueDAO().update(updated)
                        } else {
                            val updated = q.copy(
                                status = ImageStatus.FAILED_TO_PROCESS.toString(),
                                attemptCount = q.attemptCount + 1
                            )
                            db.queueDAO().update(updated)
                        }
                    } else {
                        Log.d(tag, "Nothing to upload. ${q.fileName} exists already.")
                        val updated = q.copy(status = ImageStatus.UPLOADED.toString())
                        db.queueDAO().update(updated)
                    }
                } catch (ex: FileNotFoundException) {
                    val updated = q.copy(status = ImageStatus.FILE_NOT_FOUND.toString(), attemptCount = q.attemptCount + 1)
                    Log.w(tag, "File missing.", ex)
                    db.queueDAO().update(updated)
                } catch (ex: Throwable) {
                    val updated = q.copy(status = ImageStatus.FAILED_TO_PROCESS.toString(), attemptCount = q.attemptCount + 1)
                    Log.e(tag, "Could not upload.", ex)
                    db.queueDAO().update(updated)
                }
            }

            with(NotificationManagerCompat.from(appContext)) {
                cancel(ProgressNotificationId)
            }
        }

        if (queueItems.isEmpty()) {
            val errorItems = db.queueDAO().findByStatus(ImageStatus.FAILED_TO_PROCESS.toString(), Queue.AttemptCountLimit, 50)
            Log.i(tag, "Processing with ${credentials.key}, images to in error: ${errorItems.size}")

            upload(errorItems)
        } else {
            upload(queueItems)
        }

        Log.d(tag, "Done looking up images.")
        return Result.success()
    }

    private fun checkIfFileExists(
        auth: B2AccountAuthorization,
        q: Queue
    ): Boolean {
        val exists = OkHttpB2FileClient.getFileInfoByName(auth, q.fileName)
        return exists.map { fileInfo -> fileInfo.sha1 == q.sha1 }.orElse(false)
    }
    private fun uploadOne(
        fileAuthToken: String,
        uploadUrl: String,
        q: Queue
    ): B2FileVersion? {
        val contentUri: Uri = ImageUri.contentUri(q.id)

        val ins = applicationContext.contentResolver.openInputStream(contentUri)
        if (ins == null) {
            Log.d(tag, "File content not available.")
            return null
        } else {
            Log.d(tag, "Ready to upload $contentUri.")
            return ins.source().use { stream ->
                OkHttpB2FileClient.uploadFile(
                    fileAuthToken,
                    uploadUrl,
                    q.fileName,
                    q.mimeType,
                    q.sha1,
                    q.size,
                    stream
                )
            }
        }

    }

    private fun buildNotification(current: Int, total: Int): Notification {
        return NotificationCompat.Builder(appContext, ChannelId)
            .setSmallIcon(R.mipmap.pstor_launcher)
            .setContentTitle(appContext.getString(R.string.notitication_progress_title))
            .setContentText(appContext.getString(R.string.notitication_progress_description, current, total))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    companion object {
        fun errorResult(msg: String?): Data {
            val finalError = msg ?: "Unknown error"
            return Data.Builder().putString("error", finalError).build()
        }
    }
}
