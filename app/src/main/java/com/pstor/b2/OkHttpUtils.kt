package com.pstor.b2

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient

object OkHttpUtils {
    val JsonMediaType = "application/json; charset=utf-8".toMediaType()
    val client: OkHttpClient = OkHttpClient()
}