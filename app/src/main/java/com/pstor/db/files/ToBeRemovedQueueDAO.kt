package com.pstor.db.files

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ToBeRemovedQueueDAO {
    @Query("SELECT id, size FROM toberemovedqueue")
    fun getAll(): List<ToBeRemovedQueue>

    @Query("SELECT SUM(size) FROM toberemovedqueue")
    fun obsSize(): LiveData<Long?>

    @Query("DELETE FROM toberemovedqueue WHERE id in (:ids)")
    fun deleteByIds(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: ToBeRemovedQueue)
}