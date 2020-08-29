package com.pstor.b2

import android.util.Log
import com.backblaze.b2.client.structures.*
import com.backblaze.b2.json.B2Json
import com.backblaze.b2.json.B2JsonOptions
import com.backblaze.b2.util.B2StringUtil.percentEncode
import com.pstor.B2Credentials
import okhttp3.*
import okhttp3.Headers.Companion.headersOf
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.Source
import java.lang.RuntimeException
import java.util.*

data class FileInfo(val fileId: String, val fileName: String, val sha1: String, val contentType: String) {
    companion object {
        fun fromHeaders(headers: Headers): FileInfo? {
            val fileId = headers["X-Bz-File-Id"]
            val fileName = headers["X-Bz-File-Name"]
            val sha1 = headers["X-Bz-Content-Sha1"]
            val contentType = headers["Content-Type"]

            return if (fileId != null && fileName != null && sha1 != null && contentType != null) {
                FileInfo(fileId, fileName, sha1, contentType)
            } else {
                null
            }
        }
    }
}

object OkHttpB2FileClient {
    private val tag = this.javaClass.simpleName

    private fun buildUrl(baseUrl: String, path: String): String {
        return "$baseUrl/b2api/v2/$path"
    }

    private fun downloadUrlByName(downloadUrl: String, bucketName: String, fileName: String): String {
        return downloadUrl
            .toHttpUrl() //throws if bad
            .newBuilder()
            .addPathSegment("file")
            .addPathSegment(bucketName)
            .addPathSegment(percentEncode(fileName))
            .build()
            .toString()
    }

    fun getUploadUrl(authorization: B2AccountAuthorization, bucketId: String): B2UploadUrlResponse? {
        val body = B2GetUploadUrlRequest.builder(bucketId).build()
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

    fun getFileInfoByName(authorization: B2AccountAuthorization, fileName: String): Optional<FileInfo> {
        val request = Request.Builder()
            .url(downloadUrlByName(authorization.downloadUrl, authorization.allowed.bucketName, fileName))
            .head()
            .headers(headersOf(
                "Authorization", authorization.authorizationToken
            ))
            .build()
        OkHttpUtils.client.newCall(request).execute().use {
            return if (it.isSuccessful && it.body != null) {
                Log.d(tag, "Found file $fileName on B2.")
                Optional.ofNullable(FileInfo.fromHeaders(it.headers))
            } else if (it.code == 404) {
                Log.d(tag, "File does not exist on B2.")
                Optional.empty()
            } else {
                throw RuntimeException("Unexpected exception looking for a file.")
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