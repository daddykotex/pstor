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
        image.getImage().observe(this, { maybeImage ->
            maybeImage.fold(
                { Log.e(tag, it) },
                {
                    val headers =
                        LazyHeaders.Builder().addHeader("Authorization", it.auth.authorizationToken)
                            .build()
                    val url = GlideUrl(it.url, headers)
                    Glide.with(this).load(url).into(imageView)
                }
            )
        })
    }
}