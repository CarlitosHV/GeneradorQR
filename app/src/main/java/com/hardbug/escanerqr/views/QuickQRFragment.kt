package com.hardbug.escanerqr.views

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.hardbug.escanerqr.HomeActivity
import com.hardbug.escanerqr.R
import com.hardbug.escanerqr.viewmodels.CodeViewModel

class QuickQRFragment : Fragment() {

    private lateinit var codeViewModel: CodeViewModel
    private lateinit var editTextData: TextInputEditText
    private lateinit var editTextName: TextInputEditText
    private lateinit var buttonGenerate: MaterialButton
    private lateinit var cardPreview: View
    private lateinit var imagePreview: ImageView
    private var currentPreview: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_quick_qr, container, false)
        codeViewModel = ViewModelProvider(requireActivity())[CodeViewModel::class.java]

        editTextData = view.findViewById(R.id.editTextData)
        editTextName = view.findViewById(R.id.editTextName)
        buttonGenerate = view.findViewById(R.id.buttonGenerate)
        cardPreview = view.findViewById(R.id.cardPreview)
        imagePreview = view.findViewById(R.id.imagePreview)

        setupListeners()

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navCardHeight = resources.getDimensionPixelSize(R.dimen.fab_margin) * 6
            v.updatePadding(bottom = insets.bottom + navCardHeight)
            windowInsets
        }

        return view
    }

    private fun setupListeners() {
        buttonGenerate.setOnClickListener {
            generateAndNavigate()
        }

        editTextData.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updatePreview(data: String) {
        if (data.isBlank()) {
            cardPreview.visibility = View.GONE
            currentPreview = null
            return
        }

        val bitmap = generateQRCode(data, 300)
        bitmap?.let {
            currentPreview = it
            imagePreview.setImageBitmap(it)
            cardPreview.visibility = View.VISIBLE
        } ?: run {
            cardPreview.visibility = View.GONE
        }
    }

    private fun generateAndNavigate() {
        val data = editTextData.text.toString()
        val name = editTextName.text.toString()

        if (data.isEmpty()) {
            showSnackbar(getString(R.string.error_empty_data))
            return
        }
        if (name.isEmpty()) {
            showSnackbar(getString(R.string.error_empty_name))
            return
        }

        val bitmap = generateQRCode(data, 512)
        bitmap?.let {
            codeViewModel.setGeneratedCode(it)
            codeViewModel.setName(name)
            (requireActivity() as HomeActivity).replaceFragment(CustomizeCode(), true)
        } ?: showSnackbar(getString(R.string.error_generating_code))
    }

    private fun generateQRCode(data: String, size: Int): Bitmap? {
        return try {
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            
            val bitMatrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = createBitmap(size, size)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            bitmap
        } catch (e: WriterException) {
            null
        }
    }

    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .show()
        }
    }
}
