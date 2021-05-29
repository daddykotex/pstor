package com.pstor.db.files

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ToBeRemovedQueueDAO {
    @Query("SELECT id, size FROM toberemoved")
    fun getAll(): List<ToBeRemovedQueue>

    @Query("SELECT SUM(size) FROM toberemoved")
    fun getTotalSize(): List<Long>

    @Insert
    fun insert(item: ToBeRemovedQueue)
}