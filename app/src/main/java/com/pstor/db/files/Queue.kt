package com.pstor.db.files

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Queue(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "size") val size: Long,
    @ColumnInfo(name = "sha1") val sha1: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "attempt_count") val attemptCount: Int

) {
    companion object {
        const val AttemptCountLimit = 5
    }
}
