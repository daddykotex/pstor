package com.pstor.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pstor.db.files.IgnoredQueue
import com.pstor.db.files.IgnoredQueueDAO
import com.pstor.db.files.Queue
import com.pstor.db.files.QueueDAO

@Database(entities = arrayOf(Queue::class, IgnoredQueue::class), version = 1)
abstract class PStorDatabase : RoomDatabase() {
    abstract fun queueDAO(): QueueDAO
    abstract fun ignoreQueueDAO(): IgnoredQueueDAO

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: PStorDatabase? = null

        fun getDatabase(context: Context): PStorDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        PStorDatabase::class.java,
                        "pstor-database"
                    )
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
