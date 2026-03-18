package com.hardbug.escanerqr.views

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.hardbug.escanerqr.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var barcodeResult: TextView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var snackView : View

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_camera, container, false)
        previewView = view.findViewById(R.id.previewView)
        barcodeResult = view.findViewById(R.id.barcodeResult)
        snackView = view.findViewById(R.id.fragment_camera)
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

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            // 1. Configurar Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // 2. Configurar ImageAnalysis para el escáner
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageProxy(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Desvincular todo antes de volver a vincular
                cameraProvider.unbindAll()
                
                // Vincular Preview y ImageAnalysis juntos al ciclo de vida
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                Snackbar.make(snackView, R.string.ErrorXCamara, Snackbar.LENGTH_LONG)
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: androidx.camera.core.ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let {
                            barcodeResult.text = it
                            // Si quieres mostrar el resultado, asegúrate de que sea visible
                            barcodeResult.visibility = View.VISIBLE
                        }
                    }
                }
                .addOnFailureListener {
                    // No mostramos Snackbar en cada fallo de escaneo para no saturar
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }
}