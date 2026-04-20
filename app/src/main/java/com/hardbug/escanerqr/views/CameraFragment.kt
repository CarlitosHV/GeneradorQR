package com.hardbug.escanerqr.views

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.hardbug.escanerqr.R
import com.hardbug.escanerqr.database.AppDatabase
import com.hardbug.escanerqr.models.ImageCode
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var barcodeResult: TextView
    private lateinit var btnFlash: MaterialButton
    private lateinit var btnGallery: MaterialButton
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var snackView : View
    
    private var cameraControl: CameraControl? = null
    private var isFlashOn = false
    private var isScanning = true

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Snackbar.make(snackView, R.string.NoPermissionCamera, Snackbar.LENGTH_LONG)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                processImageFromUri(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_camera, container, false)
        previewView = view.findViewById(R.id.previewView)
        barcodeResult = view.findViewById(R.id.barcodeResult)
        btnFlash = view.findViewById(R.id.btnFlash)
        btnGallery = view.findViewById(R.id.btnGallery)
        snackView = view.findViewById(R.id.fragment_camera)
        
        setupFlashButton()
        setupGalleryButton()

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navHeight = resources.getDimensionPixelSize(R.dimen.fab_margin) * 4
            
            val bottomMargin = insets.bottom + navHeight + resources.getDimensionPixelSize(R.dimen.fab_margin)
            
            btnFlash.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                this.bottomMargin = bottomMargin
            }
            btnGallery.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                this.bottomMargin = bottomMargin
            }
            
            windowInsets
        }
        
        return  view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupFlashButton() {
        btnFlash.setOnClickListener {
            toggleFlash()
        }
    }

    private fun setupGalleryButton() {
        btnGallery.setIconResource(R.drawable.ic_gallery)
        btnGallery.setOnClickListener {
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun processImageFromUri(uri: Uri) {
        try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val image = InputImage.fromBitmap(bitmap, 0)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val barcode = barcodes[0]
                        barcode.rawValue?.let { result ->
                            vibrate()
                            saveToHistory(result, "GALLERY")
                            showResultDialog(result)
                        }
                    } else {
                        Toast.makeText(requireContext(), "No se encontró ningún código en la imagen", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleFlash() {
        val cameraControl = cameraControl ?: return
        isFlashOn = !isFlashOn
        cameraControl.enableTorch(isFlashOn)
        
        btnFlash.setIconResource(
            if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
        )
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageProxy(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageAnalysis
                )
                cameraControl = camera.cameraControl

            } catch (exc: Exception) {
                Snackbar.make(snackView, R.string.ErrorXCamara, Snackbar.LENGTH_LONG)
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: androidx.camera.core.ImageProxy) {
        if (!isScanning) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty() && isScanning) {
                        val barcode = barcodes[0]
                        barcode.rawValue?.let { result ->
                            isScanning = false
                            activity?.runOnUiThread {
                                vibrate()
                                saveToHistory(result, "CAMERA")
                                showResultDialog(result)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun saveToHistory(content: String, origin: String) {
        val db = AppDatabase.getDatabase(requireContext())
        val scannedCode = ImageCode().apply {
            imageCodeUuid = UUID.randomUUID().toString()
            name = content
            urlPath = content
            metaData = origin
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            db.imageCodeDao().insertImageCode(scannedCode)
        }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun showResultDialog(result: String) {
        val isUrl = result.startsWith("http://") || result.startsWith("https://")
        
        MaterialAlertDialogBuilder(requireContext(), R.style.CustomMaterialAlertDialog)
            .setTitle(R.string.scan_result)
            .setMessage(result)
            .setPositiveButton(if (isUrl) R.string.open_link else R.string.copy) { dialog, _ ->
                if (isUrl) {
                    openUrl(result)
                } else {
                    copyToClipboard(result)
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.delete_negative) { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener {
                isScanning = true
            }
            .show()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Scanned Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), R.string.text_copied, Toast.LENGTH_SHORT).show()
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }
}