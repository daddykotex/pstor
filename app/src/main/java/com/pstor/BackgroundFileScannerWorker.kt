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

data class Image(val uri: Uri,
                 val name: String,
                 val dateAdded: Long,
                 val size: Long
)



class BackgroundFileScannerWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private val tag = "BackgroundFileScannerWorker"

    private val db: PStorDatabase = Room.databaseBuilder(
        applicationContext,
        PStorDatabase::class.java, "pstor-database"
    ).build()

    override fun doWork(): Result {
        Log.i(tag, "Starting work.")

        val imagesList = mutableListOf<Image>()

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

                imagesList += Image(contentUri, name, dateAdded, size)
            }
        }

        Log.i(tag, "Got ${imagesList.size} files")
        imagesList.forEach { i->
            Log.i(tag, "Image: ${i.name} at ${i.uri}")
        }

        return Result.success()
    }
}
