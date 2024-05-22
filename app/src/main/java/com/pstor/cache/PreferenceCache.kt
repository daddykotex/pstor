package com.pstor.cache

import android.util.Log
import arrow.core.Either
import com.backblaze.b2.client.structures.B2AccountAuthorization
import com.backblaze.b2.client.structures.B2UploadUrlResponse
import com.backblaze.b2.json.B2Json
import com.backblaze.b2.json.B2JsonOptions
import com.pstor.B2Credentials
import com.pstor.Tagged
import com.pstor.b2.OkHttpB2CredentialsClient
import com.pstor.b2.OkHttpB2FileClient
import com.pstor.preferences.SecurePreference
import java.lang.RuntimeException

class PreferenceCache(private val securePreference: SecurePreference): Tagged {

    fun getFileUrl(auth: B2AccountAuthorization, bucketId: String): Either<Throwable, B2UploadUrlResponse> {
        fun fetchFileURL(): Either<Throwable, B2UploadUrlResponse> {
            return safeRequest { OkHttpB2FileClient.getUploadUrl(auth, bucketId) }
        }

        val decode: (String) -> Either<Throwable, ExpiringFileUrl> =
            jsonDecode(ExpiringFileUrl::class.java)
        val encode: (ExpiringFileUrl) -> String =
            { payload -> B2Json.toJsonOrThrowRuntime(payload) }
        return fromCache(
            "B2_FILE_URL",
            decode,
            encode,
            { fetchFileURL().map { ExpiringFileUrl.build(it) } }
        ).map { it.value }
    }

    fun getAuth(credentials: B2Credentials): Either<Throwable, B2AccountAuthorization> {
        fun fetchAuth(): Either<Throwable, B2AccountAuthorization> {
            Log.i(tag, "Authorizing to B2.")
            return safeRequest {
                OkHttpB2CredentialsClient.checkCredentials(
                    credentials
                )
            }
        }

        val decode: (String) -> Either<Throwable, ExpiringAuth> = jsonDecode(ExpiringAuth::class.java)
        val encode: (ExpiringAuth) -> String =
            { payload -> B2Json.toJsonOrThrowRuntime(payload) }
        return fromCache(
            "B2_AUTH_TOKEN",
            decode,
            encode,
            { fetchAuth().map { ExpiringAuth.build(it) } }
        ).map { it.value }
    }



    private fun <T> safeRequest(
        f: () -> T?
    ): Either<Throwable, T> {
        return try {
            val res = f()
            return if (res == null) {
                Either.Left(RuntimeException("Unexpected null result."))
            } else {
                Either.Right(res)
            }
        } catch (ex: Throwable) {
            Either.Left(ex)
        }
    }

    private fun <T : ExpiringCacheEntry> fromCache(
        key: String,
        decode: (String) -> Either<Throwable, T>,
        encode: (T) -> String,
        orElse: () -> Either<Throwable, T>
    ): Either<Throwable, T> {
        val cached = securePreference.get(key)
        return if (cached == null) {
            val res = orElse()
            when (res) {
                is Either.Right -> {
                    Log.d(tag, "Recording entry for $key.")
                    securePreference.put(key, encode(res.b))
                }
                is Either.Left -> {
                    Log.e(tag, "Exception grabbing the fallback value", res.a)
                }
            }
            res
        } else {
            val res = decode(cached)
            when (res) {
                is Either.Left -> {
                    Log.d(tag, "Removing entry for $key.")
                    securePreference.remove(key)
                }
                is Either.Right -> {
                    val tsInSeconds = System.currentTimeMillis() / 1000

                    if (res.b.isExpiring(tsInSeconds)) {
                        Log.d(tag, "Entry has expired, removing it.")
                        securePreference.remove(key)
                        orElse()
                    } else {
                        Either.Right(res.b)
                    }
                }
            }
            res
        }
    }

    private fun <T> jsonDecode(kClass: Class<T>): (String) -> Either<Throwable, T> {
        return { payload ->
            try {
                Either.right(B2Json.fromJsonOrThrowRuntime(payload, kClass, B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS))
            } catch (ex: Throwable) {
                Log.e(tag, payload, ex)
                Either.left(RuntimeException("Unable to decode JSON from cache."))
            }
        }
    }
}

private class ExpiringFileUrl @B2Json.constructor(params = "timestampSeconds,value") constructor(
    @B2Json.required override val timestampSeconds: Long,
    @B2Json.required val value: B2UploadUrlResponse
) : ExpiringCacheEntry() {

    companion object Factory {
        fun build(value: B2UploadUrlResponse): ExpiringFileUrl {
            val tsInSeconds = System.currentTimeMillis() / 1000
            return ExpiringFileUrl(tsInSeconds, value)
        }
    }
}

private class ExpiringAuth @B2Json.constructor(params = "timestampSeconds,value") constructor(
    @B2Json.required override val timestampSeconds: Long,
    @B2Json.required val value: B2AccountAuthorization
) : ExpiringCacheEntry() {

    companion object Factory {
        fun build(value: B2AccountAuthorization): ExpiringAuth {
            val tsInSeconds = System.currentTimeMillis() / 1000
            return ExpiringAuth(tsInSeconds, value)
        }
    }
}

private abstract class ExpiringCacheEntry {
    abstract val timestampSeconds: Long

    fun isExpiring(nowInSeconds: Long): Boolean {
        // there has been more than 23 hours in between, consider expired
        return (nowInSeconds - timestampSeconds) >= 23 * 60 * 60
    }
}