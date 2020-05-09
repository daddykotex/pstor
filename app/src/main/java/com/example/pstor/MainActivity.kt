package com.example.pstor

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.pstor.b2.VolleyB2CredentialsClient
import com.example.pstor.preferences.SecurePreference

/*
Required for this application:

  B2_APPLICATION_KEY_ID
  B2_APPLICATION_KEY
 */

class MainActivity : AppCompatActivity() {
    private val PREF_B2_KEY_ID = "B2_KEY_ID"
    private val PREF_B2_KEY = "B2_KEY"

    private var securePreference: SecurePreference? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        securePreference = SecurePreference.load(this)


        val creds = loadCredentials()
        if (creds != null){
            val txtKeyId = findViewById<EditText>(R.id.txtKeyId)
            txtKeyId.text = Editable.Factory.getInstance().newEditable(creds.keyId)
            val txtKey = findViewById<EditText>(R.id.txtKey)
            txtKey.text = Editable.Factory.getInstance().newEditable(creds.key)
        }


    }

    fun onSend(view: View) {
        val txtKeyId = findViewById<EditText>(R.id.txtKeyId)
        val txtKey = findViewById<EditText>(R.id.txtKey)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val tvSaveResult = findViewById<TextView>(R.id.tvSaveResult)
        tvSaveResult.visibility = View.INVISIBLE

        val creds = validateAndBuildCredentials(txtKeyId, txtKey)
        if (creds != null) {
            btnSave.isEnabled = false

            VolleyB2CredentialsClient.checkCredentials(creds, this) { response ->
                val msg: String
                val color: Int
                if (response != null) {
                    msg =  getString(R.string.settings_app_save_success, response.apiUrl)
                    color = Color.GREEN

                    saveCredentials(creds)
                } else {
                    msg = getString(R.string.settings_app_save_fail)
                    color = Color.RED
                }

                tvSaveResult.text = msg
                tvSaveResult.setTextColor(color)
                tvSaveResult.visibility = View.VISIBLE

                btnSave.isEnabled = true
            }
        }
    }

    private fun validateAndBuildCredentials(txtKeyId: EditText, txtKey: EditText): B2Credentials? {
        if (TextUtils.isEmpty(txtKeyId.text)) {
            txtKeyId.error = getString(R.string.common_errors_empty)
            return null
        } else {
            txtKeyId.error = null
        }
        if (TextUtils.isEmpty(txtKey.text)) {
            txtKey.error = getString(R.string.common_errors_empty)
            return null
        } else {
            txtKey.error = null
        }
        return B2Credentials(txtKeyId.text.toString(), txtKey.text.toString())
    }

    private fun saveCredentials(b2Credentials: B2Credentials) {
        securePreference?.let {
            it.put(PREF_B2_KEY_ID, b2Credentials.keyId)
            it.put(PREF_B2_KEY, b2Credentials.key)
        }
    }

    private fun loadCredentials(): B2Credentials? {
        return securePreference?.let {
            val keyId = it.get(PREF_B2_KEY_ID)
            val key = it.get(PREF_B2_KEY)
            return if (key != null && keyId != null) B2Credentials(keyId, key) else null
        }
    }
}
