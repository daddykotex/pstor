package com.pstor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.IOException


class BarcodeCaptureActivity : AppCompatActivity(), Tagged {
    private var mPreview: CameraSourcePreview? = null
    private var mCameraSource: CameraSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.barcode_capture)

        mPreview = findViewById(R.id.preview)

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource()
        } else {
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        Log.w(tag,"Camera permission is not granted. Requesting permission")
        val permissions = arrayOf(Manifest.permission.CAMERA)
        ActivityCompat.requestPermissions(this, permissions, Permissions.RC_CAMERA_PERMISSION)
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private fun createCameraSource() {
        val context = applicationContext
        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        val barcodeDetector = BarcodeDetector.Builder(context)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
        val processor = MultiProcessor.Builder(BarcodeTrackerFactory { this.onCode(it) }).build()
        barcodeDetector.setProcessor(processor)
        if (!barcodeDetector.isOperational) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(tag,"Detector dependencies are not yet available.")
        }
        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        val builder: CameraSource.Builder =
            CameraSource.Builder(applicationContext, barcodeDetector)
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .setAutoFocusEnabled(true)
            .setRequestedPreviewSize(1600, 1024)
            .setRequestedFps(15.0f)
        mCameraSource = builder.build()
    }

    private fun startCameraSource() { // check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext)
        if (code != ConnectionResult.SUCCESS) {
            val dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)
            dlg.show()
        }
        Log.i(tag, "Starting camera source.")
        mCameraSource.let { cameraSource ->
            try {
                mPreview?.let {
                    Log.i(tag, "Starting camera preview.")
                    it.start(cameraSource)
                }
            } catch (e: IOException) {
                Log.e(tag, "Unable to start camera source.", e)
                mCameraSource!!.release()
                mCameraSource = null
            }
        }
    }

    /**
     * Restarts the camera.
     */
    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    /**
     * Stops the camera.
     */
    override fun onPause() {
        super.onPause()
        mPreview?.let { it.stop() }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    override fun onDestroy() {
        super.onDestroy()
        mPreview?.let { it.release() }
    }

    fun cancel(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun onCode(barcode: Barcode) {
        val intent = Intent()
        intent.putExtra(BARCODE_OBJECT, barcode)
        setResult(CommonStatusCodes.SUCCESS, intent)
        finish()
    }


    private class BarcodeTracker(private val onCode: (Barcode) -> Unit) : Tracker<Barcode>() {
        override fun onNewItem(p0: Int, p1: Barcode?) {
            super.onNewItem(p0, p1)
            p1?.let {
                Log.i("BarcodeTracker", "Found barcode, passing to callback.")
                onCode(it)
            }
        }
    }

    private class BarcodeTrackerFactory(private val onCode: (Barcode) -> Unit) : MultiProcessor.Factory<Barcode> {
        override fun create(p0: Barcode?): Tracker<Barcode> {
            return BarcodeTracker(onCode)
        }
    }

    companion object {
        private const val RC_HANDLE_GMS = 9001
        const val BARCODE_OBJECT = "Barcode"
    }
}