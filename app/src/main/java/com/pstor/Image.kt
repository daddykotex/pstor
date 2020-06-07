package com.pstor

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

object ImageUri {
    val ContentUriBase: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    fun contentUri(id: Long): Uri = ContentUris.withAppendedId(
        ContentUriBase,
        id
    )
}

enum class ImageStatus {
    IN_QUEUE, UPLOADED, TO_BE_REMOVED, FAILED_TO_PROCESS
}