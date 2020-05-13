package com.pstor

import android.net.Uri


data class ImageContent(val id: Long,
                 val uri: Uri,
                 val name: String,
                 val dateAdded: Long,
                 val size: Long
)


enum class ImageStatus {
    IN_QUEUE, UPLOADED, TO_BE_REMOVED
}