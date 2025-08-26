package com.expiryx.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class BarcodeScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    // Gate to ensure we handle the scan **only once**
    private val handled = AtomicBoolean(false)

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)

        previewView = findViewById(R.id.previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            // ML Kit scanner setup (restrict to common retail formats)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E
                )
                .build()
            val scanner = BarcodeScanning.getClient(options)

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                // Do nothing if we already handled one
                if (handled.get()) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            if (handled.get()) {
                                imageProxy.close()
                                return@addOnSuccessListener
                            }

                            // Take the first valid barcode only
                            val candidate = barcodes.firstOrNull { bc ->
                                val v = bc.rawValue
                                v != null && v.all { it.isDigit() } && v.length in 8..14
                            }?.rawValue

                            if (candidate != null) {
                                onBarcodeDetected(candidate)
                            }
                            imageProxy.close()
                        }
                        .addOnFailureListener { e ->
                            Log.e("BarcodeScanner", "Barcode scan failed", e)
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                Log.e("BarcodeScanner", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun onBarcodeDetected(barcode: String) {
        // Ensure we run this exactly once
        if (!handled.compareAndSet(false, true)) return

        // Stop camera immediately
        cameraProvider?.unbindAll()

        fetchProductInfo(barcode)
    }

    private fun fetchProductInfo(barcode: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://world.openfoodfacts.org/api/v0/product/$barcode.json")
            .build()

        ioScope.launch {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (body != null) {
                    val json = JSONObject(body)
                    val status = json.optInt("status", 0)
                    if (status == 1) {
                        val productJson = json.getJSONObject("product")
                        val name = productJson.optString("product_name", "Unknown Product")
                        val quantity = 1
                        val notes = productJson.optString("brands", "")
                        val weight = productJson.optString("quantity", "")

                        val product = Product(
                            id = 0,
                            name = name,
                            expirationDate = null,
                            quantity = quantity,
                            reminderDays = 0,
                            notes = notes,
                            weight = weight,
                            imageUri = productJson.optString("image_url", null),
                            isFavorite = false
                        )

                        withContext(Dispatchers.Main) {
                            val intent = Intent(this@BarcodeScannerActivity, ManualEntryActivity::class.java).apply {
                                putExtra("product", product)
                                // ❌ DO NOT set "isEdit" → this makes it an INSERT
                            }
                            startActivity(intent)
                            finish() // ✅ prevents duplicate launches
                        }

                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@BarcodeScannerActivity, "Product not found", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@BarcodeScannerActivity, "Empty response", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e("BarcodeScanner", "API request failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BarcodeScannerActivity, "Error fetching product info", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        ioScope.cancel()
    }
}
