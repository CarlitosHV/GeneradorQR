package com.hardbug.escanerqr.views

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.scale
import androidx.core.graphics.set
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.hardbug.escanerqr.HomeActivity
import com.hardbug.escanerqr.R
import com.hardbug.escanerqr.models.Barcode
import com.hardbug.escanerqr.models.BarcodeTypes
import com.hardbug.escanerqr.viewmodels.CodeViewModel
import java.io.InputStream
import kotlin.math.sqrt

class AdvancedCreateFragment : Fragment() {
    private lateinit var codeViewModel: CodeViewModel
    private lateinit var editTextData: TextInputEditText
    private lateinit var dataInputLayout: TextInputLayout
    private lateinit var editTextName: TextInputEditText
    private lateinit var buttonGenerate: MaterialButton
    private lateinit var buttonSelectImage: MaterialButton
    private lateinit var buttonSelectColor: MaterialButton
    private lateinit var buttonSelectSecondaryColor: MaterialButton
    private lateinit var colorPreview: ImageView
    private lateinit var secondaryColorPreview: ImageView
    private lateinit var secondaryColorActionContainer: View
    private lateinit var spinnerBarCodeTypes: AutoCompleteTextView
    private lateinit var spinnerGradientType: AutoCompleteTextView
    private lateinit var cardPreview: View
    private lateinit var imagePreview: ImageView

    private var selectedLogo: Bitmap? = null
    private var currentColor = Color.BLACK
    private var secondaryColor = Color.BLACK
    private var currentBackgroundColor = Color.WHITE

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true || permissions[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }

        if (hasAccess) {
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
                    showSnackbar(getString(R.string.logo_seleccionado_correctamente))
                    triggerPreviewUpdate()
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
        buttonSelectSecondaryColor = view.findViewById(R.id.buttonSelectSecondaryColor)
        colorPreview = view.findViewById(R.id.colorPreview)
        secondaryColorPreview = view.findViewById(R.id.secondaryColorPreview)
        secondaryColorActionContainer = view.findViewById(R.id.secondaryColorActionContainer)
        spinnerBarCodeTypes = view.findViewById(R.id.spinnerBarCodeTypes)
        spinnerGradientType = view.findViewById(R.id.spinnerGradientType)
        editTextName = view.findViewById(R.id.editTextName)
        cardPreview = view.findViewById(R.id.cardPreview)
        imagePreview = view.findViewById(R.id.imagePreview)

        buttonSelectImage.visibility = View.GONE

        setupBarcodeTypeSpinner()
        setupGradientTypeSpinner()
        setupListeners()

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navCardHeight = resources.getDimensionPixelSize(R.dimen.fab_margin) * 6
            v.updatePadding(bottom = insets.bottom + navCardHeight)
            windowInsets
        }

        return view
    }

    private fun setupBarcodeTypeSpinner() {
        val barCodeTypeNames = BarcodeTypes.barCodes.map { getString(it.nameResId) }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, barCodeTypeNames)
        spinnerBarCodeTypes.setAdapter(adapter)

        spinnerBarCodeTypes.setOnItemClickListener { _, _, position, _ ->
            val selectedType = BarcodeTypes.barCodes[position]
            val maxLength = selectedType.length
            val filters = if (maxLength > 0) arrayOf(android.text.InputFilter.LengthFilter(maxLength)) else arrayOf()
            editTextData.filters = filters
            dataInputLayout.error = null

            if (selectedType.format == BarcodeFormat.QR_CODE) {
                buttonSelectImage.visibility = View.VISIBLE
            } else {
                buttonSelectImage.visibility = View.GONE
                selectedLogo = null
            }
            triggerPreviewUpdate()
        }
    }

    private fun setupGradientTypeSpinner() {
        val types = listOf(getString(R.string.flat), getString(R.string.diagonal), getString(R.string.radial))
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, types)
        spinnerGradientType.setAdapter(adapter)
        spinnerGradientType.setText(types[0], false)

        spinnerGradientType.setOnItemClickListener { _, _, position, _ ->
            secondaryColorActionContainer.visibility = if (position == 0) View.GONE else View.VISIBLE
            triggerPreviewUpdate()
        }
    }

    private fun setupListeners() {
        buttonSelectImage.setOnClickListener { requestImagePermissions() }
        buttonSelectColor.setOnClickListener { showColorPicker(true) }
        buttonSelectSecondaryColor.setOnClickListener { showColorPicker(false) }
        buttonGenerate.setOnClickListener { generateAndNavigateToCustomize() }

        editTextData.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                triggerPreviewUpdate()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun triggerPreviewUpdate() {
        val data = editTextData.text.toString()
        val selectedTypeName = spinnerBarCodeTypes.text.toString()
        val selectedGradient = spinnerGradientType.text.toString()
        val barcodeType = BarcodeTypes.barCodes.find { getString(it.nameResId) == selectedTypeName }

        if (data.isBlank() || barcodeType == null) {
            cardPreview.visibility = View.GONE
            return
        }

        if (!isDataLengthValid(data, barcodeType)) {
            cardPreview.visibility = View.GONE
            return
        }

        val width = 300
        val height = if (barcodeType.format == BarcodeFormat.QR_CODE) 300 else 120
        val bitmap = generateBarcode(data, barcodeType.format, width, height, currentColor, secondaryColor, selectedGradient, currentBackgroundColor, selectedLogo)
        
        bitmap?.let {
            imagePreview.setImageBitmap(it)
            cardPreview.visibility = View.VISIBLE
        } ?: run {
            cardPreview.visibility = View.GONE
        }
    }

    private fun showColorPicker(isPrimary: Boolean) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_color_picker, null)
        val viewSelectedColor: View = dialogView.findViewById(R.id.viewSelectedColor)
        val sliderHue: Slider = dialogView.findViewById(R.id.sliderHue)
        val editTextHex: TextInputEditText = dialogView.findViewById(R.id.editTextHex)

        val density = resources.displayMetrics.density
        val colors = IntArray(360) { i -> Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f)) }
        val hueGradient = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors).apply {
            cornerRadius = 100f
        }
        
        val horizontalInset = (16 * density).toInt()
        val verticalInset = (18 * density).toInt()
        sliderHue.background = InsetDrawable(hueGradient, horizontalInset, verticalInset, horizontalInset, verticalInset)
        sliderHue.trackActiveTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        sliderHue.trackInactiveTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        sliderHue.trackHeight = (12 * density).toInt()

        var tempColor = if (isPrimary) currentColor else secondaryColor
        var isUpdating = false

        fun updateUIFromColor(color: Int) {
            if (isUpdating) return
            isUpdating = true
            viewSelectedColor.setBackgroundColor(color)
            editTextHex.setText(String.format("#%06X", 0xFFFFFF and color))
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            sliderHue.value = hsv[0]
            isUpdating = false
        }

        updateUIFromColor(tempColor)

        sliderHue.addOnChangeListener { _, value, _ ->
            if (!isUpdating) {
                isUpdating = true
                tempColor = Color.HSVToColor(floatArrayOf(value, 1f, 1f))
                viewSelectedColor.setBackgroundColor(tempColor)
                editTextHex.setText(String.format("#%06X", 0xFFFFFF and tempColor))
                isUpdating = false
            }
        }

        editTextHex.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 7 || s?.length == 6) {
                    try {
                        val color = Color.parseColor(if (s.startsWith("#")) s.toString() else "#$s")
                        if (!isUpdating) {
                            isUpdating = true
                            tempColor = color
                            viewSelectedColor.setBackgroundColor(tempColor)
                            val hsv = FloatArray(3)
                            Color.colorToHSV(tempColor, hsv)
                            sliderHue.value = hsv[0]
                            isUpdating = false
                        }
                    } catch (e: Exception) {}
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isPrimary) R.string.select_code_color else R.string.select_gradient_color)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                if (isPrimary) {
                    currentColor = tempColor
                    colorPreview.setColorFilter(currentColor)
                } else {
                    secondaryColor = tempColor
                    secondaryColorPreview.setColorFilter(secondaryColor)
                }
                triggerPreviewUpdate()
            }
            .setNegativeButton(R.string.delete_negative, null)
            .show()
    }

    private fun requestImagePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
        } else {
            requestPermissions.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
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
        val selectedGradient = spinnerGradientType.text.toString()

        val barcodeType = BarcodeTypes.barCodes.find { getString(it.nameResId) == selectedTypeName }

        if (data.isEmpty()) {
            dataInputLayout.error = getString(R.string.error_empty_data)
            return
        }

        barcodeType?.let {
            if (!isDataLengthValid(data, it)) {
                dataInputLayout.error = getString(R.string.error_format_mismatch, getString(it.nameResId), it.length, data.length)
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
            generateBarcode(data, it.format, width, height, currentColor, secondaryColor, selectedGradient, currentBackgroundColor, selectedLogo)
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
        color: Int,
        secColor: Int,
        gradientType: String,
        backgroundColor: Int,
        logo: Bitmap?
    ): Bitmap? {
        return try {
            var content = data
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

            when (format) {
                BarcodeFormat.EAN_13 -> if (content.length == 13) content = content.substring(0, 12)
                BarcodeFormat.EAN_8 -> if (content.length == 8) content = content.substring(0, 7)
                BarcodeFormat.UPC_A -> if (content.length == 12) content = content.substring(0, 11)
                else -> {}
            }

            val bitMatrix = MultiFormatWriter().encode(content, format, width, height, hints)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (bitMatrix.get(x, y)) {
                        bitmap[x, y] = getGradientColor(x, y, width, height, color, secColor, gradientType)
                    } else {
                        bitmap[x, y] = backgroundColor
                    }
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

    private fun getGradientColor(x: Int, y: Int, w: Int, h: Int, start: Int, end: Int, type: String): Int {
        if (type == getString(R.string.flat)) return start
        
        val ratio = when (type) {
            getString(R.string.diagonal) -> (x + y).toFloat() / (w + h).toFloat()
            getString(R.string.radial) -> {
                val cx = w / 2f
                val cy = h / 2f
                val dist = sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toDouble()).toFloat()
                val maxDist = sqrt((cx * cx + cy * cy).toDouble()).toFloat()
                (dist / maxDist).coerceIn(0f, 1f)
            }
            else -> 0f
        }
        return interpolateColor(start, end, ratio)
    }

    private fun interpolateColor(c1: Int, c2: Int, ratio: Float): Int {
        val r = (Color.red(c1) * (1 - ratio) + Color.red(c2) * ratio).toInt()
        val g = (Color.green(c1) * (1 - ratio) + Color.green(c2) * ratio).toInt()
        val b = (Color.blue(c1) * (1 - ratio) + Color.blue(c2) * ratio).toInt()
        return Color.rgb(r, g, b)
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