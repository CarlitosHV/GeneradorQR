package com.hardbug.escanerqr.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hardbug.escanerqr.ImageCodeDao
import com.hardbug.escanerqr.models.ImageCode

@Database(entities = [ImageCode::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageCodeDao(): ImageCodeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "image_code_db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}