package com.hardbug.escanerqr

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hardbug.escanerqr.models.ImageCode
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageCodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageCode(imageCode: ImageCode)

    @Query("SELECT * FROM image_codes WHERE imageCodeUuid = :uuid")
    suspend fun getImageCodeById(uuid: String): ImageCode?

    @Query("SELECT * FROM image_codes ORDER BY name ASC")
    fun getAllImageCodes(): Flow<List<ImageCode>>

    @Query("DELETE FROM image_codes WHERE imageCodeUuid = :uuid")
    suspend fun deleteImageCode(uuid: String)
}
