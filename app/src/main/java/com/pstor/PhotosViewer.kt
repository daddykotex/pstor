package com.pstor

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.*
import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.either.apply.tupled
import arrow.core.firstOrNone
import arrow.core.flatMap
import com.pstor.b2.OkHttpB2FileClient
import com.pstor.cache.PreferenceCache
import com.pstor.db.PStorDatabase
import com.pstor.db.files.Queue
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