package com.pstor

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.pstor.cache.PreferenceCache
import com.pstor.db.PStorDatabase
import com.pstor.models.images.ImageViewModel
import com.pstor.preferences.SecurePreference


class PhotosViewer : AppCompatActivity() {
    private val tag = this.javaClass.simpleName

    private var securePreference: SecurePreference? = null
    private var preferenceCache: PreferenceCache? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photos_viewer)

        securePreference = SecurePreference.load(this)
        preferenceCache = securePreference?.let { PreferenceCache(it) }

        val imageView: ImageView = findViewById(R.id.imageView) as ImageView
        val image: ImageViewModel by viewModels()
        image.getImage().observe(this, Observer{ maybeImage ->
            maybeImage.fold(
                { Log.e(tag, it) },
                {
                    val headers = LazyHeaders.Builder().addHeader("Authorization", it.auth.authorizationToken).build()
                    val url = GlideUrl(it.url, headers)
                    Glide.with(this).load(url).into(imageView)
                }
            )
        })
    }

//    private fun loadImage() {
//        val eitherAuth =
//            Option.fromNullable(securePreference)
//            .flatMap { Option.fromNullable(B2Credentials.loadFromPreferences(it)) }
//            .toEither { "Unable to load authorization." }
//            .flatMap {
//                Option.fromNullable(preferenceCache)
//                    .toEither { "Unable to load preference cache." }
//                    .flatMap { pc -> pc.getAuth(it) }
//            }
//
//        tupled(eitherAuth, eitherImage)
//            .map {
//                        OkHttpB2FileClient.getFileByName(it.a, it.b.fileName, { err -> Log.e(tag, "Oops $err") }) {
//                    Log.i(tag, "Success")
//                }
//            }
//    }
}