package com.pstor

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.pstor.db.PStorDatabase
import com.pstor.db.files.Queue

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
        Log.i(tag, "Images in queue: ${count}")

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC"

        val query = applicationContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateAddedColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val size = cursor.getLong(sizeColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val image = ImageContent(id, contentUri, name, dateAdded, size)
                val fileEntry = db.queueDAO().loadById(image.id)
                if (fileEntry == null) {
                    Log.d(tag, "Queueing image with id: ${image.id}")
                    db.queueDAO().insert(Queue(image.id, image.uri.toString(), ImageStatus.IN_QUEUE.toString()))
                }
            }
        }

        Log.d(tag, "Done looking up images.")
        return Result.success()
    }
}
