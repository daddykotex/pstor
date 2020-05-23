package com.pstor

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pstor.b2.OkHttpB2CredentialsClient
import com.pstor.preferences.SecurePreference

/*
Required for this application:

  B2_APPLICATION_KEY_ID
  B2_APPLICATION_KEY
 */

class MainActivity : AppCompatActivity() {

    private var securePreference: SecurePreference? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        securePreference = SecurePreference.load(this)


        val creds = securePreference?.let { B2Credentials.loadFromPreferences(it) }
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

            OkHttpB2CredentialsClient.checkCredentialsAsync(creds) { response ->
                this.runOnUiThread {
                    val msg: String
                    val color: Int
                    if (response != null) {
                        msg =  getString(R.string.settings_app_save_success, response.apiUrl)
                        color = Color.GREEN

                        securePreference?.let { B2Credentials.saveCredentials(it, creds) }
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
}
