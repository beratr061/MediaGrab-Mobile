package com.berat.mediagrab.di

import android.content.Context
import androidx.room.Room
import com.berat.mediagrab.data.database.AppDatabase
import com.berat.mediagrab.data.database.DownloadDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mediagrab_database"
        )
            // Add all migrations for safe database upgrades
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            // Fallback to destructive migration only if no migration path exists
            // This prevents crashes but loses data - use with caution
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideDownloadDao(database: AppDatabase): DownloadDao {
        return database.downloadDao()
    }
}
