package com.pstor.db.files

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class File(
    @PrimaryKey val fullPath: String,
    @ColumnInfo(name = "status") val status: String
)
