package com.pstor.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pstor.db.files.File
import com.pstor.db.files.FileDAO

@Database(entities = arrayOf(File::class), version = 1)
abstract class PStorDatabase : RoomDatabase() {
    abstract fun fileDAO(): FileDAO
}
