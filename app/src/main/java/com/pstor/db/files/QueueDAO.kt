package com.pstor.db.files

import androidx.room.*

@Dao
interface QueueDAO {
    @Query("SELECT COUNT(id) FROM queue")
    fun count(): Long

    @Query("SELECT id, file_name, mime_type, size, status, sha1 FROM queue")
    fun getAll(): List<Queue>

    @Query("SELECT id, file_name, mime_type, size, status, sha1 FROM queue WHERE id IN (:ids)")
    fun loadAllByIds(ids: Array<Long>): List<Queue>

    @Query("SELECT id, file_name, mime_type, size, status, sha1 FROM queue WHERE id = :id")
    fun loadById(id: Long): Queue?

    @Query("SELECT id, file_name, mime_type, size, status, sha1 FROM queue WHERE status = :status LIMIT :limit")
    fun findByStatus(status: String, limit: Int): List<Queue>

    @Update
    fun update(queue: Queue)

    @Insert
    fun insertAll(vararg files: Queue)

    @Insert
    fun insert(queue: Queue)

    @Delete
    fun delete(queue: Queue)
}