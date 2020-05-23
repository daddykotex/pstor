package com.pstor

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

object ImageUri {
    val ContentUriBase = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    fun contentUri(id: Long): Uri = ContentUris.withAppendedId(
        ContentUriBase,
        id
    )
}

data class ImageContent(
    val id: Long,
    val name: String,
    val dateAdded: Long,
    val size: Long
)


enum class ImageStatus {
    IN_QUEUE, UPLOADED, TO_BE_REMOVED
}