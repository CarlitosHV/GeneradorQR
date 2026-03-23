package com.hardbug.escanerqr.models

import com.google.zxing.BarcodeFormat

data class Barcode (
    val name: String,
    val length: Int,
    val format: BarcodeFormat
)

object BarcodeTypes{
    val barCodes = listOf(
        Barcode("UPC-A", 12, BarcodeFormat.UPC_A),
        Barcode("UPC-E", 8, BarcodeFormat.UPC_E),
        Barcode("EAN-8", 8, BarcodeFormat.EAN_8),
        Barcode("EAN-13", 13, BarcodeFormat.EAN_13),
        Barcode("Code 39", 0, BarcodeFormat.CODE_39),
        Barcode("Code 128", 0, BarcodeFormat.CODE_128),
        Barcode("ITF", 0, BarcodeFormat.ITF),
        Barcode("Codabar", 0, BarcodeFormat.CODABAR),
        Barcode("QRCode", 0, BarcodeFormat.QR_CODE)
    )
}