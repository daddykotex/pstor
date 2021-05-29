package com.pstor.b2

import android.util.Log
import com.backblaze.b2.client.structures.B2AccountAuthorization
import com.backblaze.b2.json.B2Json
import com.backblaze.b2.json.B2JsonOptions
import com.pstor.B2Credentials
import com.pstor.Tagged
import okhttp3.*
import java.io.IOException

object OkHttpB2CredentialsClient: Tagged {
    private const val url = "https://api.backblazeb2.com/b2api/v2/b2_authorize_account"

    private fun buildRequest(b2Credentials: B2Credentials): Request {
        val creds = Credentials.basic(b2Credentials.keyId, b2Credentials.key)
        return Request.Builder()
            .url(url)
            .get()
            .headers(
                Headers.Builder().add("Authorization", creds).build()
            )
            .build()
    }

    fun checkCredentials(
        b2Credentials: B2Credentials
    ): B2AccountAuthorization? {
        val request = buildRequest(b2Credentials)
        OkHttpUtils.client.newCall(request).execute().use { response ->
            return if (response.isSuccessful && response.body != null) {
                B2Json.fromJsonOrThrowRuntime(
                    response.body!!.string(),
                    B2AccountAuthorization::class.java,
                    B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS
                )
            } else {
                null
            }
        }
    }

    fun checkCredentialsAsync(
        b2Credentials: B2Credentials,
        callback: (B2AccountAuthorization?) -> Unit
    ) {
        val request = buildRequest(b2Credentials)

        OkHttpUtils.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(tag, "Failed to check credentials", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful && response.body != null) {
                    val decode =
                        B2Json.fromJsonOrThrowRuntime(
                            response.body!!.string(),
                            B2AccountAuthorization::class.java,
                            B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS
                        )
                    callback(decode)
                } else {
                    callback(null)
                }
            }
        })
    }
}