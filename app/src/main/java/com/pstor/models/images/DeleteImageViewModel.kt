package com.pstor.models.images

import android.app.Application
import android.app.PendingIntent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.extensions.list.foldable.nonEmpty
import com.pstor.ImageStatus
import com.pstor.ImageUri
import com.pstor.Tagged
import com.pstor.db.PStorDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit


class DeleteImageViewModel(private val app: Application) : AndroidViewModel(app), Tagged {

    private val db: PStorDatabase = PStorDatabase.getDatabase(app)

    private var pendingImagesToDelete: List<FileInfo>? = null

    private data class FileInfo(val id: Long, val name: String, val dateAdded: Long, val uri: Uri) {
        fun instantAdded(): Instant {
            return Instant.ofEpochSecond(dateAdded)
        }
    }

    fun updateStatusOfPendingImages() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                pendingImagesToDelete?.let {
                    it.forEach { file ->
                        db.queueDAO()
                            .updateStatusById(file.id, ImageStatus.UPLOADED_AND_REMOVED.toString())
                    }
                }
            }
        }
    }

    fun requestImageDeletion(withPi: (PendingIntent) -> Unit, onNoImages: () -> Unit) {
        viewModelScope.launch { buildDeleteRequest(withPi, onNoImages) }
    }

    private suspend fun buildDeleteRequest(withPi: (PendingIntent) -> Unit, onNoImages: () -> Unit) {
        val now = Instant.now(Clock.systemUTC())
        val daysOld: Long = 5 * 30
        val sixMonthOld = now.minus(daysOld, ChronoUnit.DAYS)

        fun buildImagesDeletionRequest(images: List<FileInfo>): PendingIntent? {
            Log.i(tag, "Generating delete request for %d files.".format(images.size))
            return if (images.isNotEmpty()) {
                MediaStore.createDeleteRequest(app.contentResolver, images.map { it.uri })
            } else {
                null
            }
        }

        fun getFileInfo(imageId: Long): FileInfo? {
            val projection = arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )

            val selection = "${MediaStore.Images.Media._ID} = ?"
            val selectionArgs = arrayOf(imageId.toString())

            val query = app.contentResolver.query(
                ImageUri.ContentUriBase,
                projection,
                selection,
                selectionArgs,
                null
            )

            return query?.use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                return if (cursor.moveToNext()) {
                    FileInfo(
                        imageId,
                        cursor.getString(nameColumn),
                        cursor.getLong(dateColumn),
                        ImageUri.contentUri(imageId)
                    )
                } else {
                    null
                }
            }
        }

        fun getImages(): List<FileInfo> {
            val idList = db.queueDAO().findAllIdsByStatus(ImageStatus.UPLOADED.toString())
            return idList
                .flatMap { imageId ->
                    val fileInfo = getFileInfo(imageId)
                    if (fileInfo == null) {
                        Log.d(tag, "No info for %d".format(imageId))
                        db.queueDAO().updateStatusById(imageId, ImageStatus.UPLOADED_AND_REMOVED.toString())
                        emptyList()
                    } else {
                        Log.d(tag, "Has info for %d".format(imageId))
                        listOfNotNull(fileInfo)
                    }
                }
                .flatMap { fileInfo ->
                    val added = fileInfo.instantAdded()
                    if (added.isBefore(sixMonthOld)) {
                        Log.d(tag, "Removing %d because it is older than %d days old.".format(fileInfo.id, daysOld))
                        listOf(fileInfo)
                    } else {
                        Log.d(tag, "Not removing %d because it is not old enough.".format(fileInfo.id))
                        emptyList()
                    }
                }
                .take(250)
        }

        withContext(Dispatchers.IO) {
            val images = getImages()
            if (images.nonEmpty()) {
                Log.d(tag, "Asking for removal of %d images.".format(images.size))
                val pi = buildImagesDeletionRequest(images)
                pendingImagesToDelete = images
                pi?.let {
                    withPi(it)
                }
            } else {
                Log.d(tag, "No images to remove.")
                onNoImages()
            }
        }
    }
}