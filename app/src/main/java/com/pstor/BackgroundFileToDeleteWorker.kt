package com.pstor

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.pstor.db.PStorDatabase
import com.pstor.db.files.ToBeRemovedQueue
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit


/**
 * This background job runs over time and look for uploaded image. When an uploaded image is
 * N days old, it is marked inserted in the TO_BE_REMOVED table.
 *
 * On the UI, the click on "Clean" will remove all images in the TO_BE_REMOVED table. It will
 * update the file status as UPLOADED_AND_REMOVED.
 *
 * see @daysOld
 */
class BackgroundFileToDeleteWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams), Tagged {

    private val db: PStorDatabase = PStorDatabase.getDatabase(appContext)

    override fun doWork(): Result {
        Log.i(tag, "Starting work.")

        val now = Instant.now(Clock.systemUTC())
        val removeBefore = now.minus(DAYS_OLD, ChronoUnit.DAYS)

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.SIZE
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

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

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val size = cursor.getLong(sizeColumn)

                val loadById = db.queueDAO().loadById(id)

                if (loadById != null) {
                    if (loadById.status == ImageStatus.UPLOADED.toString()) {
                        Log.i(tag, "Tagging image %s to be removed".format(id))
                        db.toBeRemovedQueueDAO().insert(ToBeRemovedQueue(id, size))
                    } else {
                        Log.i(tag, "Image %s not uploaded yet".format(id))
                    }
                } else {
                    Log.d(tag, "Image %s not scanned yet".format(id))
                }
            }

        }

        Log.d(tag, "Done looking up images.")
        return Result.success()
    }


    companion object {
        const val DAYS_OLD: Long = 5 * 30
    }
}
