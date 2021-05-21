package com.pstor.models.images

import android.app.Application
import android.app.PendingIntent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pstor.ImageStatus
import com.pstor.ImageUri
import com.pstor.db.PStorDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit


class DeleteImageViewModel(private val app: Application) : AndroidViewModel(app) {

    private val tag = this.javaClass.simpleName

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

    fun requestImageDeletion(withPi: (PendingIntent) -> Unit) {
        viewModelScope.launch { buildDeleteRequest(withPi) }
    }

    private suspend fun buildDeleteRequest(withPi: (PendingIntent) -> Unit) {
        val now = Instant.now(Clock.systemUTC())
        val sixMonthOld = now.minus(6 * 30, ChronoUnit.DAYS)

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
                .take(50)
                .flatMap { imageId ->
                    val fileInfo = getFileInfo(imageId)
                    if (fileInfo == null) {
                        db.queueDAO().updateStatusById(imageId, ImageStatus.UPLOADED_AND_REMOVED.toString())
                        emptyList()
                    } else {
                        listOfNotNull(fileInfo)
                    }
                }
                .flatMap { fileInfo ->
                    val added = fileInfo.instantAdded()
                    if (added.isBefore(sixMonthOld)) {
                        listOf(fileInfo)
                    } else {
                        emptyList()
                    }
                }
                .take(50)
        }

        withContext(Dispatchers.IO) {
            val images = getImages()
            val pi = buildImagesDeletionRequest(images)
            pendingImagesToDelete = images
            pi?.let {
                withPi(it)
            }
        }
    }
}