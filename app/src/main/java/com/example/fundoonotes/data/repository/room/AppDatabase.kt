package com.example.fundoonotes.data.repository.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.fundoonotes.data.repository.room.dao.LabelDao
import com.example.fundoonotes.data.repository.room.entity.LabelEntity

@Database(entities = [NoteEntity::class, LabelEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun labelDao(): LabelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fundoo_notes_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}