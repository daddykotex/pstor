package com.pstor

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.pstor.b2.OkHttpB2CredentialsClient
import com.pstor.db.PStorDatabase
import com.pstor.models.stats.StatsViewModel
import com.pstor.preferences.Keys
import com.pstor.preferences.SecurePreference

/*
Required for this application:

  B2_APPLICATION_KEY_ID
  B2_APPLICATION_KEY
 */

class MainActivity : AppCompatActivity() {

    private var securePreference: SecurePreference? = null
    private var db: PStorDatabase? = null

    private lateinit var statsViewModel: StatsViewModel

    private val tag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i(tag, "onCreate")

        securePreference = SecurePreference.load(this)

        ensurePermissions()

        val creds = securePreference?.let { B2Credentials.loadFromPreferences(it) }
        creds?.let {
            val txtKeyId = findViewById<EditText>(R.id.txtKeyId)
            txtKeyId.text = Editable.Factory.getInstance().newEditable(it.keyId)
            val txtKey = findViewById<EditText>(R.id.txtKey)
            txtKey.text = Editable.Factory.getInstance().newEditable(it.key)
        }
        val bId = securePreference?.let { it.get(Keys.BucketId) }
        bId?.let {
            val txtBucketId = findViewById<EditText>(R.id.txtBucketId)
            txtBucketId.text = Editable.Factory.getInstance().newEditable(it)
        }

        val tableLayout = findViewById<TableLayout>(R.id.tblStats)
        db = PStorDatabase.getDatabase(this)
        statsViewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(StatsViewModel::class.java)

        statsViewModel?.let { buildStats(tableLayout, it) }
    }

    private fun buildStats(tbl: TableLayout, db: StatsViewModel) {
        fun addRow(title: String, data: (StatsViewModel) -> LiveData<Long>) {
            val titleCol = TextView(this)
            titleCol.text = title

            val valueCol = TextView(this)
            valueCol.text = "N/A"
            data(db).observe(this, Observer<Long> {
                valueCol.text = it.toString()
            })

            val row = TableRow(this)
            row.addView(titleCol)
            row.addView(valueCol)

            tbl.addView(row)
        }

        //title column
        tbl.removeAllViews()
        tbl.setColumnStretchable(0, true)
        addRow(getString(R.string.settings_app_stats_count_scanned)) { it.allCount }
        addRow(getString(R.string.settings_app_stats_count_uploaded)) { it.succeedCount }
        addRow(getString(R.string.settings_app_stats_count_error)) { it.failedCount }
    }



    private fun ensurePermissions() {
        if (!Permissions.checkAllPermissions(this)) {
            ActivityCompat.requestPermissions(
                this,
                Permissions.permissionsToRequest,
                Permissions.REQUEST_CODE_PERMISSIONS
            )
        }
    }

    fun onSend(view: View) {
        val txtKeyId = findViewById<EditText>(R.id.txtKeyId)
        val txtKey = findViewById<EditText>(R.id.txtKey)
        val txtBucketId = findViewById<EditText>(R.id.txtBucketId)
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

                        securePreference?.let {
                            B2Credentials.saveCredentials(it, creds)
                            it.put(Keys.BucketId, txtBucketId.text.toString())

                        }
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
