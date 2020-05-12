package com.pstor.b2

import android.content.Context
import android.util.Base64
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.backblaze.b2.client.structures.B2AccountAuthorization
import com.pstor.B2Credentials

object VolleyB2CredentialsClient {
    private val VERSION = "v2"
    private val url = "https://api.backblazeb2.com/b2api/$VERSION/b2_authorize_account"

    fun checkCredentials(b2Credentials: B2Credentials, context: Context, callback: (response: B2AccountAuthorization?) -> Unit) {
        val creds = Base64.encodeToString("${b2Credentials.keyId}:${b2Credentials.key}".toByteArray(), Base64.DEFAULT)

        val queue = Volley.newRequestQueue(context)

        val jsonObjectRequest = VolleyB2JsonRequest(
            Request.Method.GET,
            url,
            mapOf("Authorization" to "Basic $creds"),
            B2AccountAuthorization::class.java,
            Response.ErrorListener { error ->
                if (error.networkResponse.statusCode == 401) {
                    callback(null)
                } else {
                    throw error
                }
            },
            Response.Listener { response ->
                callback(response)
            }
        )
        queue.add(jsonObjectRequest)
    }
}