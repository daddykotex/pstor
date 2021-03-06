package com.pstor.db.files

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ToBeRemovedQueue(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "size") val size: Long
)
