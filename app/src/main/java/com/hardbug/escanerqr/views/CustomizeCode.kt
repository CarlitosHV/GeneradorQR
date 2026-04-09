package com.hardbug.escanerqr.views

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.hardbug.escanerqr.R
import com.hardbug.escanerqr.viewmodels.CodeViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hardbug.escanerqr.HomeActivity
import com.hardbug.escanerqr.database.AppDatabase
import com.hardbug.escanerqr.models.ImageCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CustomizeCode : Fragment() {
    private lateinit var codeViewModel: CodeViewModel
    private lateinit var imageViewCode: ImageView
    private lateinit var buttonSave: MaterialButton
    private lateinit var buttonShare: MaterialButton
    private var nameCode: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_customize_code, container, false)
        nameCode = getString(R.string.default_qr_name)
        setupViewModel()
        setupViews(view)
        return view
    }

    private fun setupViews(view: View) {
        imageViewCode = view.findViewById(R.id.imageViewGeneratedCode)
        buttonSave = view.findViewById(R.id.buttonSave)
        buttonShare = view.findViewById(R.id.buttonShare)

        buttonSave.setOnClickListener { saveToInternalStorage() }
        buttonShare.setOnClickListener { shareImage() }
    }

    private fun setupViewModel() {
        codeViewModel = ViewModelProvider(requireActivity())[CodeViewModel::class.java]

        codeViewModel.generatedCode.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                imageViewCode.setImageBitmap(bitmap)
                enableButtons(true)
            } else {
                imageViewCode.setImageResource(R.drawable.baseline_qr_code_24)
                enableButtons(false)
            }
        }

        codeViewModel.name.observe(viewLifecycleOwner) { n ->
            if (!n.isNullOrEmpty()) {
                nameCode = n
            }
        }
    }

    private fun saveToInternalStorage() {
        val bitmap = codeViewModel.generatedCode.value ?: return
        val currentContext = context?.applicationContext ?: return
        val currentNameCode = nameCode

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val directory = File(currentContext.filesDir, "codes")
                    if (!directory.exists()) directory.mkdirs()

                    val file = File(directory, "QR_${UUID.randomUUID()}.png")
                    
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    val imageCode = ImageCode().apply {
                        imageCodeUuid = UUID.randomUUID().toString()
                        this.name = currentNameCode
                        urlPath = Uri.fromFile(file).toString()
                        this.metaData = "PNG"
                    }
                    val db = AppDatabase.getDatabase(currentContext)
                    db.imageCodeDao().insertImageCode(imageCode)
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }

            if (result) {
                showSnackbar(getString(R.string.image_saved_successfully))
                navigateToHome()
            } else {
                showSnackbar(getString(R.string.error_saving_image))
            }
        }
    }

    private fun shareImage() {
        val bitmap = codeViewModel.generatedCode.value ?: return
        val context = requireContext()

        lifecycleScope.launch {
            val uri = withContext(Dispatchers.IO) {
                try {
                    val cachePath = File(context.cacheDir, "images")
                    cachePath.mkdirs()
                    val file = File(cachePath, "shared_qr.png")
                    FileOutputStream(file).use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    FileProvider.getUriForFile(context, "com.hardbug.escanerqr.fileprovider", file)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            if (uri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "image/png"
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_code)))
            } else {
                showSnackbar(getString(R.string.error_sharing_code))
            }
        }
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
}
