package com.hardbug.escanerqr.models

class ImageCode {
    var imageCodeUuid : String = ""
    var name : String = ""
    var urlPath : String = ""
    var metaData : String = ""

    public fun ImageCode(){
    }

    fun GetPath() : String {
        return this.urlPath
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageCode

        if (imageCodeUuid != other.imageCodeUuid) return false
        if (name != other.name) return false
        if (urlPath != other.urlPath) return false
        if (metaData != other.metaData) return false

        return true
    }

    override fun toString(): String {
        return "ImageCode(name='$name')"
    }
}