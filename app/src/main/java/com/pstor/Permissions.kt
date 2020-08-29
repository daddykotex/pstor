package com.pstor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.*

object Permissions {

    const val RC_REQUIRED_PERMISSIONS = 200
    const val RC_CAMERA_PERMISSION = 201
    private val sPermissions = object : ArrayList<String>() {
        init {
            add(Manifest.permission.INTERNET)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    val permissionsToRequest = sPermissions.toTypedArray()

    fun checkAllPermissions(context: Context): Boolean {
        return sPermissions
            .stream()
            .allMatch { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
    }
}