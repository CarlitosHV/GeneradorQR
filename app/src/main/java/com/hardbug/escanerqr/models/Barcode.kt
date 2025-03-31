package com.hardbug.escanerqr.models

data class Barcode (
    val name: String,
    val length: Int
)

object BarcodeTypes{
    val barCodes = listOf(
        Barcode("UPC-A", 12),
        Barcode("UPC-E", 8),
        Barcode("EAN-8", 8),
        Barcode("EAN-13", 13),
        Barcode("Code 39", 0),
        Barcode("Code 128", 0),
        Barcode("ITF", 0),
        Barcode("Codebar", 0),
        Barcode("QRCode", 0)
    )
}