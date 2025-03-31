package com.hardbug.escanerqr.models

class Photo {
    private var photoId: Int = 0
    private var path:String = ""
    private var metaData:String = ""

    public fun GetUrl():String{
        return path;
    }

    public fun GetMetaData():String{
        return metaData;
    }
}