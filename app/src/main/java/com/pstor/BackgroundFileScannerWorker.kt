package com.pstor

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.pstor.db.PStorDatabase
import com.pstor.db.files.Queue
import org.apache.commons.codec.digest.DigestUtils

class BackgroundFileScannerWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private val tag = "BackgroundFileScannerWorker"

    private val db: PStorDatabase = Room.databaseBuilder(
        applicationContext,
        PStorDatabase::class.java, "pstor-database"
    ).build()

    override fun doWork(): Result {
        Log.i(tag, "Starting work.")

        val count = db.queueDAO().count()
        Log.i(tag, "Images in queue: $count")

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC"

        val query = applicationContext.contentResolver.query(
            ImageUri.ContentUriBase,
            projection,
            null,
            null,
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
                val sha1 = applicationContext.contentResolver.openInputStream(contentUri).use { stream ->
                    DigestUtils.sha1Hex(stream)
                }

                val fileEntry = db.queueDAO().loadById(id)
                if (fileEntry == null) {
                    Log.d(tag, "Queueing image with id: $id")
                    db.queueDAO().insert(Queue(id, name, mimeType, size, sha1, ImageStatus.IN_QUEUE.toString()))
                }
            }
        }

        Log.d(tag, "Done looking up images.")
        return Result.success()
    }
}
