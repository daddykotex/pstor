package com.pstor.db.files

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface QueueDAO {
    @Query("SELECT COUNT(id) FROM queue")
    fun count(): Long

    @Query("SELECT id, content_uri, status, status FROM queue")
    fun getAll(): List<Queue>

    @Query("SELECT id, content_uri, status FROM queue WHERE id IN (:ids)")
    fun loadAllByIds(ids: Array<Long>): List<Queue>

    @Query("SELECT id, content_uri, status FROM queue WHERE id = :id")
    fun loadById(id: Long): Queue?

    @Query("SELECT id, content_uri, status FROM queue WHERE status = :status")
    fun findByStatus(status: String): List<Queue>

    @Insert
    fun insertAll(vararg files: Queue)

    @Insert
    fun insert(files: Queue)

    @Delete
    fun delete(file: Queue)
}