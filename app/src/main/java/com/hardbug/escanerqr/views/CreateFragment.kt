package com.hardbug.escanerqr.views

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.oned.Code128Writer
import com.google.zxing.qrcode.QRCodeWriter
import com.hardbug.escanerqr.R
import com.hardbug.escanerqr.models.BarcodeTypes

class CreateFragment : Fragment() {

    private lateinit var selectedImage: Bitmap
    private var isImageSelected = false
    private lateinit var editTextData: EditText
    private lateinit var buttonGenerate: Button
    private lateinit var buttonSelectImage: Button
    private lateinit var imageViewCode: ImageView
    private lateinit var spinnerBarCodeTypes: AutoCompleteTextView

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        if (allPermissionsGranted) {
            selectImageFromGallery()
        } else {
            Snackbar.make(requireView(), R.string.NoPermissionStorage, Snackbar.LENGTH_LONG)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create, container, false)

        editTextData = view.findViewById(R.id.editTextData)
        buttonGenerate = view.findViewById(R.id.buttonGenerate)
        buttonSelectImage = view.findViewById(R.id.buttonSelectImage)
        imageViewCode = view.findViewById(R.id.imageViewCode)
        spinnerBarCodeTypes = view.findViewById(R.id.spinnerBarCodeTypes)

        val barCodeTypes = BarcodeTypes.barCodes.map { it.name }
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, barCodeTypes)
        spinnerBarCodeTypes.setAdapter(adapter)

        spinnerBarCodeTypes.setOnItemClickListener { _, _, position, _ ->
            val selectedType = BarcodeTypes.barCodes[position]
            val maxLength = selectedType.length
            val filters =
                if (maxLength > 0) arrayOf(InputFilter.LengthFilter(maxLength)) else arrayOf()
            editTextData.filters = filters
        }

        buttonSelectImage.setOnClickListener {
            val permissions = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                }
                else -> {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            requestPermissions.launch(permissions)
        }


        buttonGenerate.setOnClickListener {
            val data = editTextData.text.toString()
            val selectedType = spinnerBarCodeTypes.text.toString()
            if (data.isNotEmpty()) {
                val bitmap = when (selectedType) {
                    "QRCode" -> generateQRCode(data)
                    "Code 128" -> generateCode128(data)
                    else -> null
                }
                bitmap?.let {
                    if (isImageSelected) {
                        val finalBitmap = overlayBitmap(it, selectedImage)
                        imageViewCode.setImageBitmap(finalBitmap)
                    } else {
                        imageViewCode.setImageBitmap(it)
                    }
                }
            }
        }

        return view
    }

    private fun generateQRCode(data: String): Bitmap? {
        val size = 512
        val qrCodeWriter = QRCodeWriter()
        return try {
            val bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) -0x1000000 else -0x1)
                }
            }
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    private fun generateCode128(data: String): Bitmap? {
        val size = 512
        val code128Writer = Code128Writer()
        return try {
            val bitMatrix = code128Writer.encode(data, BarcodeFormat.CODE_128, size, 200)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) -0x1000000 else -0x1)
                }
            }
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    private fun overlayBitmap(bitmap: Bitmap, overlay: Bitmap): Bitmap {
        val combined = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(combined)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        val overlaySize = (bitmap.width * 0.2).toInt()
        val left = (bitmap.width - overlaySize) / 2
        val top = (bitmap.height - overlaySize) / 2
        canvas.drawBitmap(
            Bitmap.createScaledBitmap(overlay, overlaySize, overlaySize, true),
            left.toFloat(),
            top.toFloat(),
            null
        )
        return combined
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            data?.data?.let { uri ->
                selectedImage = uriToBitmap(uri)
                isImageSelected = true
            }
        }
    }

    companion object {
        private const val IMAGE_PICK_CODE = 1000
    }
}