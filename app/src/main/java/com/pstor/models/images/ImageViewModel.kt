package com.pstor.models.images

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import arrow.core.Either
import arrow.core.firstOrNone
import arrow.core.flatMap
import com.pstor.ImageStatus
import com.pstor.db.PStorDatabase
import com.pstor.db.files.Queue

class ImageViewModel(private val app: Application) : AndroidViewModel(app) {

    private val image: LiveData<Either<String, Queue>> = liveData {
        val data = loadImage()
        emit(data)
    }

    fun getImage(): LiveData<Either<String, Queue>> {
        return image
    }

    private suspend fun loadImage(): Either<String, Queue> {
        val db = PStorDatabase.getDatabase(app)
        return Either
            .catch {
                db.queueDAO()
                    .findByStatusAsync(ImageStatus.UPLOADED.toString(), Queue.NoLimit, 1)
            }
            .mapLeft { "Error while loading the image." }
            .flatMap { it.firstOrNone().toEither { "No image to load." } }
    }
}