package com.pstor.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pstor.db.files.*

@Database(entities = arrayOf(Queue::class, IgnoredQueue::class, ToBeRemovedQueue::class), version = 3)
abstract class PStorDatabase : RoomDatabase() {
    abstract fun queueDAO(): QueueDAO
    abstract fun ignoreQueueDAO(): IgnoredQueueDAO
    abstract fun toBeRemovedQueueDAO(): ToBeRemovedQueueDAO

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `Queue` ADD COLUMN `attempt_count` INTEGER DEFAULT 0 NOT NULL")
            }
        }

        /**
         * Grabbed from com.pstor.db.PStorDatabase_Impl
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `ToBeRemovedQueue` (`id` INTEGER NOT NULL, `size` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
