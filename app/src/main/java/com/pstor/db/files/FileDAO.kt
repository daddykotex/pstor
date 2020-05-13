package com.pstor.db.files

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FileDAO {
    @Query("SELECT COUNT(id) FROM file")
    fun count(): Long

    @Query("SELECT id, content_uri, status, status FROM file")
    fun getAll(): List<File>

    @Query("SELECT id, content_uri, status FROM file WHERE id IN (:ids)")
    fun loadAllByIds(ids: Array<Long>): List<File>

    @Query("SELECT id, content_uri, status FROM file WHERE id = :id")
    fun loadById(id: Long): File?

    @Query("SELECT id, content_uri, status FROM file WHERE status = :status")
    fun findByStatus(status: String): List<File>

    @Insert
    fun insertAll(vararg files: File)

    @Insert
    fun insert(files: File)

    @Delete
    fun delete(file: File)
}