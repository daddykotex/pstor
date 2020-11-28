package com.pstor.models.images

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import arrow.core.*
import arrow.core.extensions.either.apply.tupled
import com.backblaze.b2.client.structures.B2AccountAuthorization
import com.pstor.B2Credentials
import com.pstor.b2.OkHttpB2FileClient
import com.pstor.cache.PreferenceCache
import com.pstor.preferences.Keys
import com.pstor.preferences.SecurePreference

data class ImageToLoad(val url: String, val auth: B2AccountAuthorization)

class ImageViewModel(app: Application) : AndroidViewModel(app) {

    private val securePreference = SecurePreference(app)
    private val preferenceCache = PreferenceCache(securePreference)

    private val image: LiveData<Either<String, ImageToLoad>> = liveData {
        val result = loadImage()
        emit(result)
    }

    fun getImage(): LiveData<Either<String, ImageToLoad>> {
        return image
    }

    private suspend fun loadImage(): Either<String, ImageToLoad> {
        return tupled(getBucketId(), getAuth())
            .flatMap { getOneImageUrl(it.b, it.a).map { url -> ImageToLoad(url, it.b) } }
    }

    private fun getBucketId(): Either<String, String> {
        return securePreference.get(Keys.BucketId).rightIfNotNull { "No bucket id." }
    }

    private fun getAuth(): Either<String, B2AccountAuthorization> {
        return B2Credentials.loadFromPreferences(securePreference)
            .rightIfNotNull { "Unable to load authorization." }
            .flatMap {
                preferenceCache
                    .rightIfNotNull { "Unable to load preference cache." }
                    .flatMap { pc -> pc.getAuth(it).mapLeft { err -> err.message ?: "Error while grabbing auth." } }
            }
    }

    private suspend fun getOneImageUrl(auth: B2AccountAuthorization, bucketId: String): Either<String, String> {
        return Either
            .catch { OkHttpB2FileClient.getFileNames(auth, bucketId) }
            .mapLeft { err -> err.message ?: "Unable to load images" }
            .flatMap {
                val first = it.files.firstOrNull()
                first.rightIfNotNull { "No images available." }
                    .map { info ->
                        OkHttpB2FileClient.downloadUrlByName(
                            auth.downloadUrl,
                            auth.allowed.bucketName,
                            info.fileName
                        )
                    }
            }
    }
}