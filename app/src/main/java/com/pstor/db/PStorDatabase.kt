package com.pstor.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pstor.db.files.Queue
import com.pstor.db.files.QueueDAO

@Database(entities = arrayOf(Queue::class), version = 1)
abstract class PStorDatabase : RoomDatabase() {
    abstract fun queueDAO(): QueueDAO
}
