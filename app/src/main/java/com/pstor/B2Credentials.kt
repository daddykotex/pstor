package com.pstor

import com.pstor.preferences.SecurePreference

data class B2Credentials(val keyId: String, val key: String) {

    companion object Utils {
        private val PREF_B2_KEY_ID = "B2_KEY_ID"
        private val PREF_B2_KEY = "B2_KEY"

        fun saveCredentials(securePreference: SecurePreference, b2Credentials: B2Credentials) {
            securePreference.put(PREF_B2_KEY_ID, b2Credentials.keyId)
            securePreference.put(PREF_B2_KEY, b2Credentials.key)
        }

        fun loadFromPreferences(securePreference: SecurePreference): B2Credentials? {
            val keyId = securePreference.get(PREF_B2_KEY_ID)
            val key = securePreference.get(PREF_B2_KEY)
            return if (key != null && keyId != null) B2Credentials(keyId, key) else null
        }
    }
}
