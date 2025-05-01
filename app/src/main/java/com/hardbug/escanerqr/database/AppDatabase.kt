package com.hardbug.escanerqr.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hardbug.escanerqr.ImageCodeDao
import com.hardbug.escanerqr.models.ImageCode

@Database(entities = [ImageCode::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageCodeDao(): ImageCodeDao
}
