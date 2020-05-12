package com.pstor.b2

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.backblaze.b2.json.B2Json
import com.backblaze.b2.json.B2JsonException
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

class VolleyB2JsonRequest<T>(
    method: Int, url: String,
    private val headers: Map<String, String>,
    private val clazz: Class<T>,
    onError: Response.ErrorListener,
    private val listener: Response.Listener<T>
    ) : Request<T>(method, url, onError) {
    override fun parseNetworkResponse(response: NetworkResponse?): Response<T> {
        return try {
            val jsonString = String(
                response!!.data,
                Charset.forName(HttpHeaderParser.parseCharset(response.headers, "utf-8"))
            )
            Response.success<T>(
                B2Json.get().fromJson(jsonString, clazz),
                HttpHeaderParser.parseCacheHeaders(response)
            )
        } catch (e: UnsupportedEncodingException) {
            Response.error<T>(ParseError(e))
        } catch (je: B2JsonException) {
            Response.error<T>(ParseError(je))
        }
    }

    override fun deliverResponse(response: T) {
        listener.onResponse(response)
    }

    override fun getHeaders(): MutableMap<String, String> {
        return headers.toMutableMap()
    }
}