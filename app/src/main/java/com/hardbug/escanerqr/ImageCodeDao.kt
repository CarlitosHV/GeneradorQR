package com.hardbug.escanerqr

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hardbug.escanerqr.models.ImageCode
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageCodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageCode(imageCode: ImageCode)

    @Update
    suspend fun updateImageCode(imageCode: ImageCode)

    @Query("SELECT * FROM image_codes WHERE imageCodeUuid = :uuid")
    suspend fun getImageCodeById(uuid: String): ImageCode?

    @Query("SELECT * FROM image_codes ORDER BY createdAt DESC")
    fun getAllImageCodes(): Flow<List<ImageCode>>

    @Query("SELECT * FROM image_codes WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteImageCodes(): Flow<List<ImageCode>>

    @Query("DELETE FROM image_codes WHERE imageCodeUuid = :uuid")
    suspend fun deleteImageCode(uuid: String)
}
