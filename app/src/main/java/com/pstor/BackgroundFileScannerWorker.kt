package com.pstor

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.pstor.db.PStorDatabase
import com.pstor.db.files.IgnoredQueue
import com.pstor.db.files.Queue
import org.apache.commons.codec.digest.DigestUtils
import java.io.FileNotFoundException

class BackgroundFileScannerWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private val tag = "BackgroundFileScannerWorker"

    private val db: PStorDatabase = PStorDatabase.getDatabase(appContext)

    override fun doWork(): Result {
        Log.i(tag, "Starting work.")

        val count = db.queueDAO().count()
        val ignoredList = db.ignoreQueueDAO().getAll()
        val ignoreMap = ignoredList.map { i -> i.id to i.reason }.toMap()
        Log.i(tag, "Images in queue: $count, ${ignoredList.size} ignored.")

        val lastId = db.queueDAO().lastId() ?: 0L

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC"
        val selection = "${MediaStore.Images.Media._ID} >= ?"
        val selectionArgs = arrayOf(lastId.toString())


        val query = applicationContext.contentResolver.query(
            ImageUri.ContentUriBase,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mtColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val size = cursor.getLong(sizeColumn)
                val mimeType = cursor.getString(mtColumn)

                val contentUri = ImageUri.contentUri(id)
                val sha1 = try {
                    applicationContext.contentResolver.openInputStream(contentUri).use { stream ->
                        DigestUtils.sha1Hex(stream)
                    }
                } catch (ex: FileNotFoundException) {
                    null
                }

                val fileEntry = db.queueDAO().loadById(id)
                val ignored = ignoreMap[id]

                if (ignored == null && fileEntry == null && sha1 != null && size > 0) {
                    Log.d(tag, "Queueing image with id: $id")
                    db.queueDAO().insert(
                        Queue(
                            id,
                            name,
                            mimeType,
                            size,
                            sha1,
                            ImageStatus.IN_QUEUE.toString()
                        )
                    )
                } else if (ignored == null) {
                    if (sha1 == null) {
                        Log.d(tag, "SHA1 is null for $id")
                        db.ignoreQueueDAO().insert(
                            IgnoredQueue(id,"SHA1 unavailable")
                        )
                    }
                    if (size <= 0) {
                        Log.d(tag, "SHA1 is null for $id")
                        db.ignoreQueueDAO().insert(
                            IgnoredQueue(id,"size is equal or smaller than 0 bytes")
                        )
                    }
                }
            }
        }

        Log.d(tag, "Done looking up images.")
        return Result.success()
    }
}
