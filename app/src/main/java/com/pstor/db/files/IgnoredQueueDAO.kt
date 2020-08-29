package com.pstor.db.files

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface IgnoredQueueDAO {
    @Query("SELECT id, reason FROM ignoredqueue")
    fun getAll(): List<IgnoredQueue>

    @Insert
    fun insert(queue: IgnoredQueue)
}