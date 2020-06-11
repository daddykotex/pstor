package com.pstor.b2

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object OkHttpUtils {
    val JsonMediaType = "application/json; charset=utf-8".toMediaType()
    val client: OkHttpClient =
        OkHttpClient.Builder()
            .readTimeout(1L, TimeUnit.MINUTES)
            .writeTimeout(5L, TimeUnit.MINUTES)
            .callTimeout(5L, TimeUnit.MINUTES)
            .build()
}