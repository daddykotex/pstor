package com.pstor.b2

import android.util.Log
import com.backblaze.b2.client.structures.B2AccountAuthorization
import com.backblaze.b2.client.structures.B2FileVersion
import com.backblaze.b2.client.structures.B2GetUploadUrlRequest
import com.backblaze.b2.client.structures.B2UploadUrlResponse
import com.backblaze.b2.json.B2Json
import com.backblaze.b2.json.B2JsonOptions
import okhttp3.*
import okhttp3.Headers.Companion.headersOf
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okio.BufferedSink
import okio.Source


object OkHttpB2FileClient {
    private val tag = this.javaClass.simpleName

    private fun buildUrl(baseUrl: String, path: String): String {
        return "$baseUrl/b2api/v2/$path"
    }

    fun getUploadUrl(authorization: B2AccountAuthorization): B2UploadUrlResponse? {
        val body = B2GetUploadUrlRequest.builder("22a0f5be0bdc74507c170819").build()
        val request = Request.Builder()
            .url(buildUrl(authorization.apiUrl, "b2_get_upload_url"))
            .post(B2Json.toJsonOrThrowRuntime(body).toRequestBody(OkHttpUtils.JsonMediaType))
            .headers(headersOf(
                "Authorization", authorization.authorizationToken
            ))
            .build()
        OkHttpUtils.client.newCall(request).execute().use {
            return if (it.isSuccessful && it.body != null) {
                B2Json.fromJsonOrThrowRuntime(it.body!!.string(), B2UploadUrlResponse::class.java)
            } else {
                null
            }
        }
    }

    fun uploadFile(fileAuthToken: String, uploadUrl: String, fileName: String, contentType: String, sha1: String, size: Long, src: Source): B2FileVersion? {
        val requestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return contentType.toMediaType()
            }

            override fun writeTo(sink: BufferedSink) {
                sink.writeAll(src)
            }

            override fun contentLength(): Long {
                return size
            }
        }
        val request = Request.Builder()
            .url(uploadUrl)
            .post(requestBody)
            .headers(headersOf(
                "Authorization", fileAuthToken,
                "X-Bz-File-Name", fileName,
                "X-Bz-Content-Sha1", sha1
            ))
            .build()

        Log.d(tag, "Request to upload $fileName ($size bytes) at $uploadUrl")
        OkHttpUtils.client.newCall(request).execute().use {
            return if (it.isSuccessful && it.body != null) {
                B2Json.fromJsonOrThrowRuntime(it.body!!.string(), B2FileVersion::class.java, B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS)
            } else {
                val body = it.body?.string()
                Log.d(tag, "Unsuccessful request to $uploadUrl, code: ${it.code}, body: $body")
                null
            }
        }
    }
}