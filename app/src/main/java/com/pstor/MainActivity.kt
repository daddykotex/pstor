package com.pstor

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.vision.barcode.Barcode import com.pstor.b2.OkHttpB2CredentialsClient
import com.pstor.databinding.ActivityMainBinding
import com.pstor.db.PStorDatabase
import com.pstor.models.images.DeleteImageViewModel
import com.pstor.models.stats.CleanSizeViewModel
import com.pstor.models.stats.StatsViewModel
import com.pstor.preferences.Keys
import com.pstor.preferences.SecurePreference

/*
Required for this application:

  B2_APPLICATION_KEY_ID
  B2_APPLICATION_KEY
 */

class MainActivity : AppCompatActivity(), Tagged {

    private var securePreference: SecurePreference? = null
    private var db: PStorDatabase? = null

    private lateinit var statsViewModel: StatsViewModel
    private lateinit var deleteImageViewModel: DeleteImageViewModel
    private lateinit var cleanSizeViewModel: CleanSizeViewModel
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)

        Log.i(tag, "onCreate")

        securePreference = SecurePreference.load(this)

        ensurePermissions()

        val creds = securePreference?.let { B2Credentials.loadFromPreferences(it) }
        creds?.let {
            binding.txtKeyId.text = Editable.Factory.getInstance().newEditable(it.keyId)
            binding.txtKey.text = Editable.Factory.getInstance().newEditable(it.key)
        }
        val bId = securePreference?.let { it.get(Keys.BucketId) }
        bId?.let {
            binding.txtBucketId.text = Editable.Factory.getInstance().newEditable(it)
        }

        val tableLayout = findViewById<TableLayout>(R.id.tblStats)
        db = PStorDatabase.getDatabase(this)
        statsViewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(StatsViewModel::class.java)
        cleanSizeViewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(CleanSizeViewModel::class.java)

        buildStats(tableLayout, statsViewModel)

        deleteImageViewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(DeleteImageViewModel::class.java)
    }

    private fun buildStats(tbl: TableLayout, db: StatsViewModel) {
        fun addRow(data: Pair<String, LiveData<String>>) {
            val titleCol = TextView(this)
            titleCol.text = data.first

            val valueCol = TextView(this)
            valueCol.text = "N/A"
            data.second.observe(this, {
                valueCol.text = it
            })

            val row = TableRow(this)
            row.addView(titleCol)
            row.addView(valueCol)

            tbl.addView(row)
        }

        //title column
        tbl.removeAllViews()
        tbl.setColumnStretchable(0, true)
        db.stats.forEach { addRow(it.toPair()) }

        val btnClean = findViewById<Button>(R.id.btnClean)
        cleanSizeViewModel.toBeRemovedSize.observe(this, {
            btnClean.isEnabled = it > 0
        })
    }



    private fun ensurePermissions() {
        if (!Permissions.checkAllPermissions(this)) {
            ActivityCompat.requestPermissions(
                this,
                Permissions.permissionsToRequest,
                Permissions.RC_REQUIRED_PERMISSIONS
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
                    tvSaveResult.visibility = VISIBLE

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

    fun startScan(view: View) {
        val intent = Intent(this, BarcodeCaptureActivity::class.java)
        startActivityForResult(intent, RC_BARCODE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                Log.d(tag, "Successfully returning from barcode scanning.")
                if (data != null) {
                    val barcode: Barcode? =
                        data.getParcelableExtra(BarcodeCaptureActivity.BARCODE_OBJECT)
                    barcode?.let { barCodeVal ->
                        Log.d(tag, "Barcode read correctly")
                        validateBarcode(barCodeVal.rawValue).let { maybePair ->
                            maybePair?.let { askAndApplySettings(it.first, it.second) }
                        }
                    }
                } else {
                    Log.d(tag, "No barcode captured, intent data is null")
                }
            }
        }

        if (requestCode == DELETE_PERMISSION_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(tag, "User permission granted and images were deleted")
                deleteImageViewModel.updateStatusOfPendingImages()
            } else {
                Log.i(tag, "User denied deletion")
            }
        }
    }


    private fun askAndApplySettings(bucketId: String, credentials: B2Credentials) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings_app_barcode_confirmation_title))
            .setMessage(getString(R.string.settings_app_barcode_confirmation_message, bucketId))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok
            ) { _, _ ->
                binding.txtBucketId.text = Editable.Factory.getInstance().newEditable(bucketId)
                binding.txtKeyId.text = Editable.Factory.getInstance().newEditable(credentials.keyId)
                binding.txtKey.text = Editable.Factory.getInstance().newEditable(credentials.key)
            }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun validateBarcode(barcode: String): Pair<String, B2Credentials>? {
        val parts = barcode.split("||")
        val id = parts.elementAtOrNull(0)
        val keyId = parts.elementAtOrNull(1)
        val key = parts.elementAtOrNull(2)

        return if (id != null && keyId != null && key != null) {
            id to B2Credentials(keyId, key)
        } else {
            null
        }
    }

    companion object {
        const val RC_BARCODE_CAPTURE = 9001
        const val DELETE_PERMISSION_REQUEST = 9002
    }

    fun launchPhotos(view: View) {
        startActivity(Intent(this, PhotosViewer::class.java))
    }

    fun askForCleaning(view: View) {
        val progressBar = findViewById<ProgressBar>(R.id.pBar)

        val onDelete: (PendingIntent) -> Unit = { pi ->
            Log.i(tag, "Asking for permissions to remove several files")

            runOnUiThread {
                progressBar.visibility = GONE
                view.visibility = VISIBLE
            }

            startIntentSenderForResult(
                pi.intentSender,
                DELETE_PERMISSION_REQUEST,
                null,
                0,
                0,
                0,
                null
            )
        }
        val onNoImages: () -> Unit = {
            runOnUiThread {
                progressBar.visibility = GONE
                view.visibility = VISIBLE
                Toast
                    .makeText(applicationContext, getString(R.string.settings_app_clean_no_image_toast), Toast.LENGTH_SHORT)
                    .show()
            }
        }

        progressBar.visibility = VISIBLE
        view.visibility = GONE
        deleteImageViewModel.requestImageDeletion(onDelete, onNoImages)
    }
}
