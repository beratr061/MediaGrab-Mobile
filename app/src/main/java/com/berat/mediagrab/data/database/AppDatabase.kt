package com.berat.mediagrab.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DownloadEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    companion object {
        // Migration from version 1 to 2 (template for future migrations)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Example: Add new column
                // db.execSQL("ALTER TABLE downloads ADD COLUMN pausedAt INTEGER")
            }
        }

        // All migrations list for easy access
        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
    }
}
