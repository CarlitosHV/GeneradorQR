package com.hardbug.escanerqr.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_codes")
data class ImageCode(
    @PrimaryKey
    var imageCodeUuid: String = "",
    var name: String = "",
    var urlPath: String = "",
    var metaData: String = ""
)
