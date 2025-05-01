package com.hardbug.escanerqr

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.hardbug.escanerqr.models.ImageCode

@Dao
interface ImageCodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageCode(imageCode: ImageCode)

    @Query("SELECT * FROM image_codes WHERE imageCodeUuid = :uuid")
    suspend fun getImageCodeById(uuid: String): ImageCode?
}
