package com.pstor

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.pstor.cache.PreferenceCache
import com.pstor.db.PStorDatabase
import com.pstor.models.images.ImageViewModel
import com.pstor.preferences.SecurePreference


class PhotosViewer : AppCompatActivity() {
    private val tag = this.javaClass.simpleName

    private var db: PStorDatabase? = null
    private var securePreference: SecurePreference? = null
    private var preferenceCache: PreferenceCache? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photos_viewer)

        securePreference = SecurePreference.load(this)
        preferenceCache = securePreference?.let { PreferenceCache(it) }

        val image: ImageViewModel by viewModels()
        image.getImage().observe(this, Observer{ maybeImage ->
            maybeImage.fold(
                { Log.e(tag, it) },
                { Log.i(tag, "Yeah ${it.fileName}")}
            )
        })


        val imageView: ImageView = findViewById(R.id.imageView) as ImageView
        Glide.with(this).load("https://lh6.ggpht.com/9SZhHdv4URtBzRmXpnWxZcYhkgTQurFuuQ8OR7WZ3R7fyTmha77dYkVvcuqMu3DLvMQ=w300").into(imageView)
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