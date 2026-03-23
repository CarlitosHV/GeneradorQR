package com.hardbug.escanerqr.views

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.scale
import androidx.core.graphics.set
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.hardbug.escanerqr.HomeActivity
import com.hardbug.escanerqr.R
import com.hardbug.escanerqr.models.Barcode
import com.hardbug.escanerqr.models.BarcodeTypes
import com.hardbug.escanerqr.viewmodels.CodeViewModel
import java.io.InputStream

class AdvancedCreateFragment : Fragment() {
    private lateinit var codeViewModel: CodeViewModel
    private lateinit var editTextData: com.google.android.material.textfield.TextInputEditText
    private lateinit var dataInputLayout: TextInputLayout
    private lateinit var editTextName: com.google.android.material.textfield.TextInputEditText
    private lateinit var buttonGenerate: MaterialButton
    private lateinit var buttonSelectImage: MaterialButton
    private lateinit var buttonSelectColor: MaterialButton
    private lateinit var colorPreview: ImageView
    private lateinit var spinnerBarCodeTypes: AutoCompleteTextView

    private var selectedLogo: Bitmap? = null
    private var currentColor = Color.BLACK
    private var currentBackgroundColor = Color.WHITE

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        if (allPermissionsGranted) {
            selectImageFromGallery()
        } else {
            showSnackbar(getString(R.string.NoPermissionStorage))
        }
    }

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedLogo = uriToBitmap(uri)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_advanced_create, container, false)
        codeViewModel = ViewModelProvider(requireActivity())[CodeViewModel::class.java]

        editTextData = view.findViewById(R.id.editTextData)
        dataInputLayout = view.findViewById(R.id.textInputLayoutData)
        buttonGenerate = view.findViewById(R.id.buttonGenerate)
        buttonSelectImage = view.findViewById(R.id.buttonSelectImage)
        buttonSelectColor = view.findViewById(R.id.buttonSelectColor)
        colorPreview = view.findViewById(R.id.colorPreview)
        spinnerBarCodeTypes = view.findViewById(R.id.spinnerBarCodeTypes)
        editTextName = view.findViewById(R.id.editTextName)

        setupBarcodeTypeSpinner()
        setupListeners()

        return view
    }

    private fun setupBarcodeTypeSpinner() {
        val barCodeTypes = BarcodeTypes.barCodes.map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            barCodeTypes
        )
        spinnerBarCodeTypes.setAdapter(adapter)

        spinnerBarCodeTypes.setOnItemClickListener { _, _, position, _ ->
            val selectedType = BarcodeTypes.barCodes[position]
            val maxLength = selectedType.length
            val filters =
                if (maxLength > 0) arrayOf(android.text.InputFilter.LengthFilter(maxLength)) else arrayOf()
            editTextData.filters = filters
            dataInputLayout.error = null
        }
    }

    private fun setupListeners() {
        buttonSelectImage.setOnClickListener {
            requestImagePermissions()
        }

        buttonSelectColor.setOnClickListener {
            showColorPickerDialog()
        }

        buttonGenerate.setOnClickListener {
            generateAndNavigateToCustomize()
        }
    }

    private fun requestImagePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestPermissions.launch(permissions)
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun showColorPickerDialog() {
        val colors = listOf(
            Color.BLACK to getString(R.string.black),
            Color.BLUE to getString(R.string.blue),
            Color.RED to getString(R.string.red),
            Color.GREEN to getString(R.string.green),
            Color.MAGENTA to getString(R.string.magenta),
            Color.CYAN to getString(R.string.cyan),
            Color.DKGRAY to getString(R.string.dark_gray)
        )

        val colorNames = colors.map { it.second }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_code_color)
            .setItems(colorNames) { _, which ->
                currentColor = colors[which].first
                colorPreview.setColorFilter(currentColor)
            }
            .show()
    }

    private fun isDataLengthValid(data: String, barcode: Barcode): Boolean {
        if (barcode.length == 0) return true
        return if (barcode.format == BarcodeFormat.EAN_13 || barcode.format == BarcodeFormat.EAN_8 ||
            barcode.format == BarcodeFormat.UPC_A || barcode.format == BarcodeFormat.UPC_E) {
            data.length == barcode.length || data.length == barcode.length - 1
        } else {
            data.length == barcode.length
        }
    }

    private fun generateAndNavigateToCustomize() {
        val data = editTextData.text.toString()
        val name = editTextName.text.toString()
        val selectedTypeName = spinnerBarCodeTypes.text.toString()

        val barcodeType = BarcodeTypes.barCodes.find { it.name == selectedTypeName }

        if (data.isEmpty()) {
            dataInputLayout.error = getString(R.string.error_empty_data)
            return
        }

        barcodeType?.let {
            if (!isDataLengthValid(data, it)) {
                dataInputLayout.error = getString(R.string.error_format_mismatch, it.name, it.length, data.length)
                return
            }
        }

        if(name.isEmpty()){
            showSnackbar(getString(R.string.error_empty_name))
            return
        }

        dataInputLayout.error = null

        val bitmap = barcodeType?.let {
            val width = 512
            val height = if (it.format == BarcodeFormat.QR_CODE) 512 else 200
            generateBarcode(data, it.format, width, height, currentColor, currentBackgroundColor, selectedLogo)
        }

        bitmap?.let {
            codeViewModel.setGeneratedCode(it)
            codeViewModel.setName(name)
            (requireActivity() as HomeActivity).replaceFragment(CustomizeCode(), true)
        } ?: showSnackbar(getString(R.string.error_generating_code))
    }

    private fun generateBarcode(
        data: String,
        format: BarcodeFormat,
        width: Int,
        height: Int,
        color: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE,
        logo: Bitmap? = null
    ): Bitmap? {
        return try {
            var content = data
            when (format) {
                BarcodeFormat.EAN_13 -> if (content.length == 13) content = content.substring(0, 12)
                BarcodeFormat.EAN_8 -> if (content.length == 8) content = content.substring(0, 7)
                BarcodeFormat.UPC_A -> if (content.length == 12) content = content.substring(0, 11)
                else -> {}
            }

            val bitMatrix = MultiFormatWriter().encode(content, format, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap[x, y] = if (bitMatrix.get(x, y)) color else backgroundColor
                }
            }

            if (format == BarcodeFormat.QR_CODE && logo != null) {
                addLogoToQR(bitmap, logo)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun addLogoToQR(qrBitmap: Bitmap, logo: Bitmap): Bitmap {
        val combined = qrBitmap.copy(qrBitmap.config!!, true)
        val canvas = Canvas(combined)

        val logoSize = (qrBitmap.width * 0.2f).toInt()
        val scaledLogo = logo.scale(logoSize, logoSize)

        val left = (qrBitmap.width - scaledLogo.width) / 2
        val top = (qrBitmap.height - scaledLogo.height) / 2

        canvas.drawBitmap(scaledLogo, left.toFloat(), top.toFloat(), null)
        return combined
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }

    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .show()
        }
    }
}