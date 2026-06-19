package com.presto.mediamanager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.presto.mediamanager.data.model.MediaStatus

class Converters {
    @TypeConverter
    fun toStatus(value: String): MediaStatus = MediaStatus.valueOf(value)

    @TypeConverter
    fun fromStatus(status: MediaStatus): String = status.name
}

@Database(entities = [MediaItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "presto-media.db",
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
