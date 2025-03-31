package com.hardbug.escanerqr.views

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.hardbug.escanerqr.R
import com.hardbug.escanerqr.viewmodels.CodeViewModel
import java.io.ByteArrayOutputStream
import androidx.core.net.toUri

class CustomizeCode : Fragment() {
    private lateinit var codeViewModel: CodeViewModel
    private lateinit var imageViewCode: ImageView
    private lateinit var buttonSave: MaterialButton
    private lateinit var buttonShare: MaterialButton

    private val saveImageLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        uri?.let { saveBitmapToUri(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_customize_code, container, false)
        setupViews(view)
        setupViewModel()
        return view
    }

    private fun setupViews(view: View) {
        imageViewCode = view.findViewById(R.id.imageViewGeneratedCode)
        buttonSave = view.findViewById(R.id.buttonSave)
        buttonShare = view.findViewById(R.id.buttonShare)

        buttonSave.setOnClickListener { saveImage() }
        buttonShare.setOnClickListener { shareImage() }
    }

    private fun setupViewModel() {
        codeViewModel = ViewModelProvider(requireActivity())[CodeViewModel::class.java]

        codeViewModel.generatedCode.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                imageViewCode.setImageBitmap(bitmap)
                enableButtons(true)
            } else {
                imageViewCode.setImageResource(R.drawable.ic_launcher)
                enableButtons(false)
            }
        }
    }

    private fun saveImage() {
        codeViewModel.generatedCode.value?.let { bitmap ->
            val fileName = "QRCode_${System.currentTimeMillis()}.png"
            saveImageLauncher.launch(fileName)
        }
    }

    private fun saveBitmapToUri(uri: Uri) {
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                codeViewModel.generatedCode.value?.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    outputStream
                )
                showSnackbar(getString(R.string.image_saved_successfully))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar(getString(R.string.error_saving_image))
        }
    }

    private fun shareImage() {
        codeViewModel.generatedCode.value?.let { bitmap ->
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, getImageUri(bitmap))
                type = "image/png"
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_code)))
        }
    }

    private fun getImageUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            requireContext().contentResolver,
            bitmap,
            "QRCode_${System.currentTimeMillis()}",
            null
        )
        return path.toUri()
    }

    private fun enableButtons(enabled: Boolean) {
        buttonSave.isEnabled = enabled
        buttonShare.isEnabled = enabled
    }

    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .show()
        }
    }
}