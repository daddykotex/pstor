package com.pstor.db.files

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FileDAO {
    @Query("SELECT fullPath, status FROM file")
    fun getAll(): List<File>

    @Query("SELECT fullPath, status FROM file WHERE fullPath IN (:fullPaths)")
    fun loadAllByIds(fullPaths: Array<String>): List<File>

    @Query("SELECT fullPath, status FROM file WHERE status = :status")
    fun findByStatus(status: String): List<File>

    @Insert
    fun insertAll(vararg files: File)

    @Delete
    fun delete(file: File)
}