package com.hardbug.escanerqr.models

import com.google.zxing.BarcodeFormat
import com.hardbug.escanerqr.R

data class Barcode (
    val nameResId: Int,
    val length: Int,
    val format: BarcodeFormat
)

object BarcodeTypes{
    val barCodes = listOf(
        Barcode(R.string.barcode_qrcode, 0, BarcodeFormat.QR_CODE),
        Barcode(R.string.barcode_upca, 12, BarcodeFormat.UPC_A),
        Barcode(R.string.barcode_upce, 8, BarcodeFormat.UPC_E),
        Barcode(R.string.barcode_ean8, 8, BarcodeFormat.EAN_8),
        Barcode(R.string.barcode_ean13, 13, BarcodeFormat.EAN_13),
        Barcode(R.string.barcode_code39, 0, BarcodeFormat.CODE_39),
        Barcode(R.string.barcode_code128, 0, BarcodeFormat.CODE_128),
        Barcode(R.string.barcode_itf, 0, BarcodeFormat.ITF),
        Barcode(R.string.barcode_codabar, 0, BarcodeFormat.CODABAR)
    )
}