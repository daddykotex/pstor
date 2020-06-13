package com.pstor

import android.app.Notification
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
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
import com.pstor.db.PStorDatabase
import com.pstor.db.files.Queue
import com.pstor.preferences.Keys
import com.pstor.preferences.SecurePreference
import com.pstor.utils.Either
import okio.source

class BackgroundFileUploaderWorker(private val appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    private val tag = this.javaClass.simpleName

    private val db: PStorDatabase = Room.databaseBuilder(
        applicationContext,
        PStorDatabase::class.java, "pstor-database"
    ).build()

    private val securePreference: SecurePreference = SecurePreference.load(appContext)

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
        when (val maybeAuth = getAuth(credentials)) {
            is Either.Left -> return maybeAuth.left
            is Either.Right -> auth = maybeAuth.right
        }
        Log.i(tag, "Authorized.")


        val maybeFileUrl = getFileUrl(auth, bucketId)
        val fileUrlResponse: B2UploadUrlResponse
        when (maybeFileUrl) {
            is Either.Left -> return maybeFileUrl.left
            is Either.Right -> fileUrlResponse = maybeFileUrl.right
        }
        Log.i(tag, "Successfully retrieved an URL to start uploading: ${fileUrlResponse.uploadUrl}")

        val queueItems = db.queueDAO().findByStatus(ImageStatus.IN_QUEUE.toString(), 20)
        Log.i(tag, "Processing with ${credentials.key}, images to process: ${queueItems.size}")

        with(NotificationManagerCompat.from(appContext)) {
            notify(ProgressNotificationId, buildNotification(0, queueItems.size))
        }

        queueItems.forEachIndexed { index, q ->
            try {
                val result =
                    uploadOne(fileUrlResponse.authorizationToken, fileUrlResponse.uploadUrl, q)

                with(NotificationManagerCompat.from(appContext)) {
                    notify(ProgressNotificationId, buildNotification(index, queueItems.size))
                }

                if (result != null) {
                    Log.d(
                        tag,
                        "Upload successful of ${q.fileName} with a size of ${result.contentLength}"
                    )
                    val updated = q.copy(status = ImageStatus.UPLOADED.toString())
                    db.queueDAO().update(updated)
                } else {
                    val updated = q.copy(status = ImageStatus.FAILED_TO_PROCESS.toString())
                    db.queueDAO().update(updated)
                }
            } catch (ex: Throwable) {
                val updated = q.copy(status = ImageStatus.FAILED_TO_PROCESS.toString())
                Log.e(tag, "Could not upload.", ex)
                db.queueDAO().update(updated)
            }
        }

        with(NotificationManagerCompat.from(appContext)) {
            cancel(ProgressNotificationId)
        }

        Log.d(tag, "Done looking up images.")
        return Result.success()
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
        }

        Log.d(tag, "Ready to upload $contentUri.")
        return ins.source().use { stream ->
            return OkHttpB2FileClient.uploadFile(
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

    private fun buildNotification(current: Int, total: Int): Notification {
        return NotificationCompat.Builder(appContext, ChannelId)
            .setSmallIcon(R.mipmap.pstor_launcher)
            .setContentTitle(appContext.getString(R.string.notitication_progress_title))
            .setContentText(appContext.getString(R.string.notitication_progress_description, current, total))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun <T> safeRequest(
        f: () -> T?
    ): Either<Result, T> {
        return try {
            val res = f()
            return if (res == null) {
                Either.Left(Result.failure(errorResult("could not get the file url")))
            } else {
                Either.Right(res)
            }
        } catch (ex: Throwable) {
            Log.e(tag, "Failed to get a file url.", ex)
            Either.Left(Result.failure(errorResult("error while getting the file url")))
        }
    }

    private fun <T : ExpiringCacheEntry> fromCache(
        key: String,
        decode: (String) -> Either<Result, T>,
        encode: (T) -> String,
        orElse: () -> Either<Result, T>
    ): Either<Result, T> {
        val cached = securePreference.get(key)
        return if (cached == null) {
            val res = orElse()
            when (res) {
                is Either.Right -> {
                    Log.d(tag, "Recording entry for $key.")
                    securePreference.put(key, encode(res.right))
                }
            }
            res
        } else {
            val res = decode(cached)
            when (res) {
                is Either.Left -> {
                    Log.d(tag, "Removing entry for $key.")
                    securePreference.remove(key)
                }
                is Either.Right -> {
                    val tsInSeconds = System.currentTimeMillis() / 1000

                    if (res.right.isExpiring(tsInSeconds)) {
                        Log.d(tag, "Entry has expired, removing it.")
                        securePreference.remove(key)
                        orElse()
                    } else {
                        Either.Right(res.right)
                    }
                }
            }
            res
        }
    }

    private fun <T> jsonDecode(kClass: Class<T>): (String) -> Either<Result, T> {
        return { payload ->
            Either.safe(
                { B2Json.fromJsonOrThrowRuntime(payload, kClass) },
                {
                    Log.e(tag, payload, it)
                    Result.failure(
                        Data.Builder().putString(
                            "error",
                            "parsing json from preferences failed"
                        ).build()
                    )
                }
            )
        }
    }

    private fun getFileUrl(auth: B2AccountAuthorization, bucketId: String): Either<Result, B2UploadUrlResponse> {
        fun fetchFileURL(): Either<Result, B2UploadUrlResponse> {
            return safeRequest { OkHttpB2FileClient.getUploadUrl(auth, bucketId) }
        }

        val decode: (String) -> Either<Result, ExpiringFileUrl> =
            jsonDecode(ExpiringFileUrl::class.java)
        val encode: (ExpiringFileUrl) -> String =
            { payload -> B2Json.toJsonOrThrowRuntime(payload) }
        return fromCache(
            "B2_FILE_URL",
            decode,
            encode,
            { fetchFileURL().map { ExpiringFileUrl.build(it) } }
        ).map { it.value }
    }

    private fun getAuth(credentials: B2Credentials): Either<Result, B2AccountAuthorization> {
        fun fetchAuth(): Either<Result, B2AccountAuthorization> {
            Log.i(tag, "Authorizing to B2.")
            return safeRequest {
                OkHttpB2CredentialsClient.checkCredentials(
                    credentials
                )
            }
        }

        val decode: (String) -> Either<Result, ExpiringAuth> = jsonDecode(ExpiringAuth::class.java)
        val encode: (ExpiringAuth) -> String =
            { payload -> B2Json.toJsonOrThrowRuntime(payload) }
        return fromCache(
            "B2_AUTH_TOKEN",
            decode,
            encode,
            { fetchAuth().map { ExpiringAuth.build(it) } }
        ).map { it.value }
    }

    companion object {
        fun errorResult(msg: String): Data {
            return Data.Builder().putString("error", msg).build()
        }
    }
}

private class ExpiringFileUrl @constructor(params = "timestampSeconds,value") constructor(
    @required override val timestampSeconds: Long,
    @required val value: B2UploadUrlResponse
) : ExpiringCacheEntry() {

    companion object Factory {
        fun build(value: B2UploadUrlResponse): ExpiringFileUrl {
            val tsInSeconds = System.currentTimeMillis() / 1000
            return ExpiringFileUrl(tsInSeconds, value)
        }
    }
}

private class ExpiringAuth @constructor(params = "timestampSeconds,value") constructor(
    @required override val timestampSeconds: Long,
    @required val value: B2AccountAuthorization
) : ExpiringCacheEntry() {

    companion object Factory {
        fun build(value: B2AccountAuthorization): ExpiringAuth {
            val tsInSeconds = System.currentTimeMillis() / 1000
            return ExpiringAuth(tsInSeconds, value)
        }
    }
}

private abstract class ExpiringCacheEntry {
    abstract val timestampSeconds: Long

    fun isExpiring(nowInSeconds: Long): Boolean {
        // there has been more than 23 hours in between, consider expired
        return (nowInSeconds - timestampSeconds) >= 23 * 60 * 60
    }
}
