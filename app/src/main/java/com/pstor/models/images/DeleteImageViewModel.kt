package com.pstor.models.images

import android.app.Application
import android.app.PendingIntent
import android.media.Image
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
import com.pstor.db.files.ToBeRemovedQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit


class DeleteImageViewModel(private val app: Application) : AndroidViewModel(app), Tagged {

    private val db: PStorDatabase = PStorDatabase.getDatabase(app)

    private var pendingImagesToDelete: List<ToBeRemovedQueue>? = null

    fun updateStatusOfPendingImages() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val ids = listOfNotNull(pendingImagesToDelete).flatten().map { it.id }

                Log.i(tag, "Updating status of %d images".format(ids.size))

                if (ids.nonEmpty()) {
                    ids
                        .chunked(100)
                        .forEach { grouped ->
                            db.queueDAO().updateStatusByIds(grouped, ImageStatus.UPLOADED_AND_REMOVED.toString())
                            db.toBeRemovedQueueDAO().deleteByIds(grouped)
                        }
                }
            }
        }
    }

    fun requestImageDeletion(withPi: (PendingIntent) -> Unit, onNoImages: () -> Unit) {
        viewModelScope.launch { buildDeleteRequest(withPi, onNoImages) }
    }

    private suspend fun buildDeleteRequest(withPi: (PendingIntent) -> Unit, onNoImages: () -> Unit) {

        withContext(Dispatchers.IO) {
            val toBeRemoved = db.toBeRemovedQueueDAO().getAll()

            fun buildImagesDeletionRequest(images: List<ToBeRemovedQueue>): PendingIntent {
                Log.i(tag, "Generating delete request for %d files.".format(images.size))
                return MediaStore.createDeleteRequest(app.contentResolver, images.map { ImageUri.contentUri(it.id) })
            }

            if (toBeRemoved.nonEmpty()) {
                val pi = buildImagesDeletionRequest(toBeRemoved)
                pendingImagesToDelete = toBeRemoved
                withPi(pi)
            } else {
                onNoImages()
            }
        }
    }
}