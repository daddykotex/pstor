package com.example.pstor.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.Key
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec


class SecurePreference(context: Context) {
    private val AndroidKeyStore = "AndroidKeyStore"
    private val AES_MODE = "AES/GCM/NoPadding"
    private val KEY_ALIAS = "B2_KEYS"
    private val DUMMY_IV = "ASDFGHJKIUYG" //12 bytes

    private val keyStore: KeyStore
    private val sharedPreferences: SharedPreferences;

    init {
        keyStore = KeyStore.getInstance(AndroidKeyStore);
        keyStore.load(null)
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator: KeyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore)
            val purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            keyGenerator.init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, purposes)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(false)
                    .build()
            )
            keyGenerator.generateKey()
        }

        sharedPreferences = context.getSharedPreferences(
            "PSTOR_SECURE",
            Context.MODE_PRIVATE
        )
    }

    @SuppressLint("ApplySharedPref")
    fun put(key: String, value: String) {
        sharedPreferences.edit().putString(key, encryptAndEncode(value)).commit()
    }

    fun get(key: String): String? {
        val value = sharedPreferences.getString(key, null)
        return if (value != null) {
            decodeAndDecrypt(value)
        } else {
            null
        }
    }

    private fun encryptAndEncode(input: String): String {
        val c: Cipher = Cipher.getInstance(AES_MODE)
        c.init(
            Cipher.ENCRYPT_MODE,
            getKey(),
            GCMParameterSpec(128, DUMMY_IV.toByteArray())
        )
        val encodedBytes: ByteArray = c.doFinal(input.toByteArray())
        return Base64.encodeToString(encodedBytes, Base64.DEFAULT)
    }

    private fun decodeAndDecrypt(input: String): String {
        val c: Cipher = Cipher.getInstance(AES_MODE)
        c.init(
            Cipher.DECRYPT_MODE,
            getKey(),
            GCMParameterSpec(128, DUMMY_IV.toByteArray())
        )
        return String(c.doFinal(Base64.decode(input, Base64.DEFAULT)))
    }

    private fun getKey(): Key {
        return keyStore.getKey(KEY_ALIAS, null)
    }


    companion object Instance {
        private var securePreference: SecurePreference? = null
        fun load(context: Context): SecurePreference {
            if (securePreference == null) {
                securePreference = SecurePreference(context)
            }
            return securePreference as SecurePreference
        }
    }
}