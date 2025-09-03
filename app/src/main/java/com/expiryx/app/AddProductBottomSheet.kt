package com.expiryx.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AddProductBottomSheet : BottomSheetDialogFragment() {

    private var progressBar: ProgressBar? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some providers don't support persistable permission; ignore gracefully.
        }
        analyseImageForBarcode(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_add_product, container, false)

        val optionManual: LinearLayout = view.findViewById(R.id.optionManual)
        val optionCamera: LinearLayout = view.findViewById(R.id.optionCamera)
        val optionUpload: LinearLayout = view.findViewById(R.id.optionUpload)
        val optionCancel: View = view.findViewById(R.id.optionCancel)
        progressBar = view.findViewById(R.id.progressBarUpload)

        optionManual.setOnClickListener {
            startActivity(Intent(requireContext(), ManualEntryActivity::class.java))
            dismissAllowingStateLoss()
        }
        optionCamera.setOnClickListener {
            startActivity(Intent(requireContext(), BarcodeScannerActivity::class.java))
            dismissAllowingStateLoss()
        }
        optionUpload.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }
        optionCancel.setOnClickListener { dismissAllowingStateLoss() }

        return view
    }

    private fun showLoading(show: Boolean) {
        progressBar?.isVisible = show
        isCancelable = !show
    }

    private fun analyseImageForBarcode(uri: Uri) {
        showLoading(true)
        try {
            val image = InputImage.fromFilePath(requireContext(), uri)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13,
                    com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_8,
                    com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A,
                    com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E
                ).build()
            val scanner = BarcodeScanning.getClient(options)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val code = barcodes.firstOrNull()?.rawValue
                    if (!code.isNullOrBlank()) {
                        fetchProductInfo(code, uri)
                    } else {
                        showLoading(false)
                        Toast.makeText(
                            requireContext(),
                            "No barcode found in image",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Failed to analyse image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(requireContext(), "Error reading image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchProductInfo(barcode: String, uploadedImage: Uri) {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url("https://world.openfoodfacts.org/api/v0/product/$barcode.json")
            .build()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val body = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) null else response.body?.string()
                }

                if (body == null) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Product not found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val json = JSONObject(body)
                if (json.optInt("status") == 1) {
                    val prod = json.getJSONObject("product")
                    val apiImage = prod.optString("image_url", null)
                    val name = prod.optString("product_name", "").trim()

                    // If name is blank, jump to manual entry but carry image + barcode
                    if (name.isEmpty()) {
                        showLoading(false)
                        val intent = Intent(requireContext(), ManualEntryActivity::class.java).apply {
                            putExtra("isEdit", false)
                            putExtra("product", Product(
                                id = 0,
                                name = "",
                                expirationDate = null,
                                quantity = 1,
                                reminderDays = 0,
                                notes = prod.optString("brands", "").takeIf { it.isNotBlank() },
                                weight = prod.optString("quantity", "").takeIf { it.isNotBlank() },
                                imageUri = apiImage ?: uploadedImage.toString(),
                                isFavorite = false
                            ))
                        }
                        startActivity(intent)
                        dismissAllowingStateLoss()
                        return@launch
                    }

                    val product = Product(
                        id = 0,
                        name = name,
                        expirationDate = null,
                        quantity = 1,
                        reminderDays = 0,
                        notes = prod.optString("brands", "").takeIf { it.isNotBlank() },
                        weight = prod.optString("quantity", "").takeIf { it.isNotBlank() },
                        imageUri = apiImage ?: uploadedImage.toString(),
                        isFavorite = false
                    )
                    showLoading(false)
                    val intent = Intent(requireContext(), ManualEntryActivity::class.java).apply {
                        putExtra("product", product)
                        putExtra("isEdit", false)
                    }
                    startActivity(intent)
                    dismissAllowingStateLoss()
                } else {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Product not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(
                    requireContext(),
                    "Error fetching product info",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
