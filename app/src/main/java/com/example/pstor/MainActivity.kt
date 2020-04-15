package com.example.pstor

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.pstor.b2.VolleyB2Client

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
        val txtKeyId = findViewById<EditText>(R.id.txtKeyId)
        val txtKey = findViewById<EditText>(R.id.txtKey)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val tvSaveResult = findViewById<TextView>(R.id.tvSaveResult)
        tvSaveResult.visibility = View.INVISIBLE

        val creds = validateAndBuildCredentials(txtKeyId, txtKey)
        if (creds != null) {
            btnSave.isEnabled = false

            VolleyB2Client.checkCredentials(creds, this) { response ->
                val msg =
                    if (response) getString(R.string.settings_app_save_success) else getString(R.string.settings_app_save_fail)
                val color = if (response) Color.GREEN else Color.RED

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
}
