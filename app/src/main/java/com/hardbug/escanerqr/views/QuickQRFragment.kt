package com.hardbug.escanerqr.views

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_quick_qr, container, false)
        codeViewModel = ViewModelProvider(requireActivity())[CodeViewModel::class.java]

        editTextData = view.findViewById(R.id.editTextData)
        editTextName = view.findViewById(R.id.editTextName)
        buttonGenerate = view.findViewById(R.id.buttonGenerate)

        buttonGenerate.setOnClickListener {
            generateAndNavigate()
        }

        return view
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

        val bitmap = generateQRCode(data)
        bitmap?.let {
            codeViewModel.setGeneratedCode(it)
            codeViewModel.setName(name)
            (requireActivity() as HomeActivity).replaceFragment(CustomizeCode(), true)
        } ?: showSnackbar(getString(R.string.error_generating_code))
    }

    private fun generateQRCode(data: String): Bitmap? {
        return try {
            val size = 512
            val bitMatrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size)
            val bitmap = createBitmap(size, size)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
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