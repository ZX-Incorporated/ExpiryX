package com.expiryx.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class BarcodeScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var progressBar: ProgressBar
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private val handled = AtomicBoolean(false)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)

        previewView = findViewById(R.id.previewView)
        progressBar = findViewById(R.id.progressBarScan)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E
                    )
                    .build()
                val scanner = BarcodeScanning.getClient(options)

                analysis?.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (handled.get()) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val mediaImage = imageProxy.image ?: run {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val candidate = barcodes.firstOrNull { bc ->
                                val v = bc.rawValue
                                v != null && v.all { it.isDigit() } && v.length in 8..14
                            }?.rawValue

                            if (candidate != null && handled.compareAndSet(false, true)) {
                                runOnUiThread {
                                    setLoading(true)
                                    Toast.makeText(this, "Barcode detected...", Toast.LENGTH_SHORT).show()
                                }
                                try {
                                    cameraProvider?.unbindAll()
                                } catch (_: Exception) {
                                }
                                fetchProductInfo(candidate)
                            }
                            imageProxy.close()
                        }
                        .addOnFailureListener { e ->
                            Log.e("BarcodeScanner", "Scan failed", e)
                            imageProxy.close()
                        }
                }

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

    private fun fetchProductInfo(barcode: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url("https://world.openfoodfacts.org/api/v2/product/$barcode.json")
            .build()

        ioScope.launch {
            try {
                val body = client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) null else resp.body?.string()
                }

                withContext(Dispatchers.Main) { setLoading(false) }

                if (body != null) {
                    val json = JSONObject(body)
                    if (json.optInt("status", 0) == 1) {
                        val productJson = json.getJSONObject("product")
                        val name = productJson.optString("product_name", "").trim()

                        // ✅ FIX: Changed 'notes' to 'brand'
                        val brand = productJson.optString("brands", "").takeIf { it.isNotBlank() }
                        val weight = productJson.optString("quantity", "").takeIf { it.isNotBlank() }
                        val imageUrl = productJson.optString("image_url", null)

                        val product = Product(
                            id = 0,
                            name = name,
                            expirationDate = null,
                            quantity = 1,
                            reminderDays = 3, // A sensible default
                            brand = brand,
                            weight = weight,
                            imageUri = imageUrl,
                            isFavorite = false
                        )

                        withContext(Dispatchers.Main) {
                            val intent = Intent(
                                this@BarcodeScannerActivity,
                                ManualEntryActivity::class.java
                            ).apply {
                                putExtra("product", product)
                                putExtra("isEdit", false)
                            }
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@BarcodeScannerActivity, "Product not found.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@BarcodeScannerActivity, "No response from server.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e("BarcodeScanner", "API failed", e)
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Toast.makeText(this@BarcodeScannerActivity, "Error fetching product info.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            analysis?.clearAnalyzer()
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
        }
        cameraExecutor.shutdown()
        ioScope.cancel()
    }
}