package com.pstor.db.files

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface QueueDAO {
    @Query("SELECT COUNT(id) FROM queue")
    fun count(): Long

    @Query("SELECT COUNT(id) FROM queue")
    fun obsCount(): LiveData<Long>

    @Query("SELECT COUNT(id) FROM queue WHERE status = :status")
    fun obsCountByStatus(status: String): LiveData<Long>

    @Query("SELECT MAX(id) FROM queue")
    fun lastId(): Long?

    @Query("SELECT id, file_name, mime_type, size, status, sha1, attempt_count FROM queue ORDER BY id")
    fun getAll(): List<Queue>

    @Query("SELECT id, file_name, mime_type, size, status, sha1, attempt_count FROM queue WHERE id IN (:ids) ORDER BY id")
    fun loadAllByIds(ids: Array<Long>): List<Queue>

    @Query("SELECT id, file_name, mime_type, size, status, sha1, attempt_count FROM queue WHERE id = :id")
    fun loadById(id: Long): Queue?

    @Query("SELECT id, file_name, mime_type, size, status, sha1, attempt_count FROM queue WHERE status = :status AND attempt_count <= :attemptCountLimit ORDER BY id LIMIT :limit")
    fun findByStatus(status: String, attemptCountLimit: Int, limit: Int): List<Queue>

    @Update
    fun update(queue: Queue)

    @Insert
    fun insertAll(vararg files: Queue)

    @Insert
    fun insert(queue: Queue)

    @Delete
    fun delete(queue: Queue)
}