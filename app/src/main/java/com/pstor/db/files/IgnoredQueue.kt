package com.pstor.db.files

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class IgnoredQueue(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "reason") val reason: String
)
