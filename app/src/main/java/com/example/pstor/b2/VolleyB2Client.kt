package com.example.pstor.b2

import android.content.Context
import android.util.Base64
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.pstor.B2Credentials
import org.json.JSONObject

object VolleyB2Client {
    private val VERSION = "v2"
    private val url = "https://api.backblazeb2.com/b2api/$VERSION/b2_authorize_account"

    fun checkCredentials(b2Credentials: B2Credentials, context: Context, callback: (response: Boolean) -> Unit): Unit {
        val creds = Base64.encodeToString("${b2Credentials.keyId}:${b2Credentials.key}".toByteArray(), Base64.DEFAULT)

        val queue = Volley.newRequestQueue(context)

        val jsonObjectRequest = object : JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            Response.Listener<JSONObject> {
                callback(true)
            },
            Response.ErrorListener { error ->
                if (error.networkResponse.statusCode == 401) {
                    callback(false)
                } else {
                    throw error
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mapOf("Authorization" to "Basic $creds").toMutableMap()
            }
        }

        queue.add(jsonObjectRequest)
    }
}