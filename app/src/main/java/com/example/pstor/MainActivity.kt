package com.example.pstor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText

/*
Required for this application:

  B2_APPLICATION_KEY_ID
  B2_APPLICATION_KEY
 */

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onSend(view: View) {
        val tvKeyId = findViewById<EditText>(R.id.txtKeyId)
        val tvKey = findViewById<EditText>(R.id.txtKey)

        val creds = validateAndBuildCredentials(tvKeyId, tvKey)
    }

    fun validateAndBuildCredentials(tvKeyId: EditText, tvKey: EditText): B2Credentials? {
        if (TextUtils.isEmpty(tvKeyId.text)) {
            tvKeyId.error = getString(R.string.settings_app_key_id_errors_empty)
            return null
        } else {
            tvKeyId.error = null
        }
        if (TextUtils.isEmpty(tvKey.text)) {
            tvKey.error = getString(R.string.settings_app_key_errors_empty)
            return null
        } else {
            tvKey.error = null
        }
        return B2Credentials(tvKeyId.text.toString(), tvKey.text.toString())
    }
}
