package com.hardbug.escanerqr.views

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hardbug.escanerqr.HomeActivity
import com.hardbug.escanerqr.database.AppDatabase
import com.hardbug.escanerqr.models.ImageCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class CustomizeCode : Fragment() {
    private lateinit var codeViewModel: CodeViewModel
    private lateinit var imageViewCode: ImageView
    private lateinit var buttonSave: MaterialButton
    private lateinit var buttonShare: MaterialButton
    private var nameCode: String = "QR_Code"

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
        setupViewModel()
        setupViews(view)
        return view
    }

    private fun setupViews(view: View) {
        imageViewCode = view.findViewById(R.id.imageViewGeneratedCode)
        buttonSave = view.findViewById(R.id.buttonSave)
        buttonShare = view.findViewById(R.id.buttonShare)

        buttonSave.setOnClickListener { saveImage(nameCode) }
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

        codeViewModel.name.observe(viewLifecycleOwner) { n ->
            if (!n.isNullOrEmpty()) {
                nameCode = n
            }
        }
    }

    private fun saveImage(name: String) {
        codeViewModel.generatedCode.value?.let {
            val fileName = "Code_${name}_${System.currentTimeMillis()}.png"
            saveImageLauncher.launch(fileName)
        }
    }

    private fun saveBitmapToUri(uri: Uri) {
        val bitmap = codeViewModel.generatedCode.value ?: return
        val currentContext = context?.applicationContext ?: return
        val currentNameCode = nameCode

        CoroutineScope(Dispatchers.IO).launch {
            var success = false
            try {
                val metadata = getMetaDataFromUri(uri)
                currentContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                if (success) {
                    val imageCode = ImageCode().apply {
                        imageCodeUuid = UUID.randomUUID().toString()
                        this.name = currentNameCode
                        urlPath = uri.toString()
                        this.metaData = metadata
                    }
                    val db = AppDatabase.getDatabase(currentContext)
                    db.imageCodeDao().insertImageCode(imageCode)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                success = false
            }

            launch(Dispatchers.Main) {
                if (success) {
                    showSnackbar(getString(R.string.image_saved_successfully))
                    navigateToHome()
                } else {
                    showSnackbar(getString(R.string.error_saving_image))
                }
            }
        }
    }

    private fun shareImage() {
        codeViewModel.generatedCode.value?.let { bitmap ->
            try {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, getImageUri(bitmap))
                    type = "image/png"
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_code)))
                navigateToHome()
            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar("Error al compartir el código")
            }
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
        return Uri.parse(path)
    }

    private fun navigateToHome() {
        (requireActivity() as? HomeActivity)?.let { activity ->
            activity.replaceFragment(HomeFragment())
            activity.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.page_home
        }
    }

    private fun enableButtons(enabled: Boolean) {
        buttonSave.isEnabled = enabled
        buttonShare.isEnabled = enabled
    }

    private fun showSnackbar(message: String) {
        val rootView = activity?.findViewById<View>(android.R.id.content)
        rootView?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .show()
        }
    }

    private fun getMetaDataFromUri(uri: Uri): String {
        val currentContext = context ?: return "No metadata available"
        val cursor = currentContext.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                val name = if (displayNameIndex != -1) it.getString(displayNameIndex) else "Unknown"
                val size = if (sizeIndex != -1) it.getLong(sizeIndex) else -1L
                return "Name: $name, Size: $size bytes"
            }
        }
        return "No metadata available"
    }

}