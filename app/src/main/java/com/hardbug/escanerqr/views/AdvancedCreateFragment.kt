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
import com.google.zxing.WriterException
import com.google.zxing.oned.Code128Writer
import com.google.zxing.qrcode.QRCodeWriter
import com.hardbug.escanerqr.HomeActivity
import com.hardbug.escanerqr.R
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
    private lateinit var imageViewCode: ImageView
    private lateinit var logoPreview: ImageView
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
                    selectedLogo?.let {
                        logoPreview.setImageBitmap(it)
                        logoPreview.visibility = View.VISIBLE
                        updateCodePreview()
                    }
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
        imageViewCode = view.findViewById(R.id.imageViewCode)
        logoPreview = view.findViewById(R.id.logoPreview)
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
            updateCodePreview()
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

        editTextData.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                updateCodePreview()
            }
        }
    }

    private fun requestImagePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            )
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
            Color.BLACK to "Negro",
            Color.BLUE to "Azul",
            Color.RED to "Rojo",
            Color.GREEN to "Verde",
            Color.MAGENTA to "Magenta",
            Color.CYAN to "Cian",
            Color.DKGRAY to "Gris Oscuro"
        )

        val colorNames = colors.map { it.second }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleccionar color")
            .setItems(colorNames) { _, which ->
                currentColor = colors[which].first
                colorPreview.setColorFilter(currentColor)
                updateCodePreview()
            }
            .show()
    }

    private fun generateAndNavigateToCustomize() {
        val data = editTextData.text.toString()
        val name = editTextName.text.toString()
        val selectedTypeName = spinnerBarCodeTypes.text.toString()
        
        val barcodeType = BarcodeTypes.barCodes.find { it.name == selectedTypeName }

        if (data.isEmpty()) {
            dataInputLayout.error = "Ingresa datos para codificar"
            return
        }

        barcodeType?.let {
            if (it.length > 0 && data.length != it.length) {
                dataInputLayout.error = "El formato para ${it.name} requiere exactamente ${it.length} caracteres (tienes ${data.length})"
                return
            }
        }

        if(name.isEmpty()){
            showSnackbar("Debes ingresar un nombre para el código")
            return
        }

        dataInputLayout.error = null

        val bitmap = when (selectedTypeName) {
            "QRCode" -> generateQRCode(data, currentColor, currentBackgroundColor, selectedLogo)
            "Code 128" -> generateCode128(data)
            else -> generateQRCode(data, currentColor, currentBackgroundColor, selectedLogo)
        }

        bitmap?.let {
            codeViewModel.setGeneratedCode(it)
            codeViewModel.setName(name)
            (requireActivity() as HomeActivity).replaceFragment(CustomizeCode(), true)
        } ?: showSnackbar("Error al generar el código")
    }

    private fun generateQRCode(
        data: String,
        color: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE,
        logo: Bitmap? = null
    ): Bitmap? {
        return try {
            val size = 512
            val bitMatrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap[x, y] = if (bitMatrix[x, y]) color else backgroundColor
                }
            }

            logo?.let { addLogoToQR(bitmap, it) } ?: bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    private fun generateCode128(data: String): Bitmap? {
        return try {
            val width = 512
            val height = 200
            val bitMatrix = Code128Writer().encode(data, BarcodeFormat.CODE_128, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap[x, y] = if (bitMatrix[x, y]) currentColor else currentBackgroundColor
                }
            }

            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
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

    private fun updateCodePreview() {
        val data = editTextData.text.toString()
        val selectedTypeName = spinnerBarCodeTypes.text.toString()
        val barcodeType = BarcodeTypes.barCodes.find { it.name == selectedTypeName }

        if (data.isNotEmpty()) {
            if (barcodeType != null && barcodeType.length > 0 && data.length != barcodeType.length) {
                imageViewCode.setImageResource(R.drawable.baseline_qr_code_24)
                imageViewCode.setColorFilter(Color.LTGRAY)
                return
            }

            val previewSize = 200
            val bitmap = when (selectedTypeName) {
                "QRCode" -> generateQRCode(
                                data,
                                currentColor,
                                currentBackgroundColor,
                                selectedLogo?.scale(previewSize / 5, previewSize / 5)
                            )?.scale(previewSize, previewSize, false)
                "Code 128" -> generateCode128(data)?.scale(previewSize, previewSize / 2, false)
                else -> null
            }

            bitmap?.let {
                imageViewCode.clearColorFilter()
                imageViewCode.setImageBitmap(it)
            }
        }
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