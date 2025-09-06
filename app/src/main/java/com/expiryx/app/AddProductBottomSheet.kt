package com.expiryx.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        } catch (_: SecurityException) {}
        analyseImageForBarcode(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.bottom_sheet_add_product, container, false)

        val optionManual: LinearLayout = view.findViewById(R.id.optionManual)
        val optionCamera: LinearLayout = view.findViewById(R.id.optionCamera)
        val optionUpload: LinearLayout = view.findViewById(R.id.optionUpload)
        val optionCancel: View = view.findViewById(R.id.optionCancel)
        progressBar = view.findViewById(R.id.progressBarUpload)

        optionManual.setOnClickListener {
            startActivity(Intent(requireContext(), ManualEntryActivity::class.java).apply {
                putExtra("isEdit", false)
            })
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getParcelable<Uri>(ARG_INITIAL_IMAGE_URI)?.let { uri ->
            // If a URI was passed via arguments, analyze it directly
            analyseImageForBarcode(uri)
        }
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
                    if (!code.isNullOrBlank()) fetchProductInfo(code, uri)
                    else {
                        showLoading(false)
                        Toast.makeText(requireContext(), "No barcode found in image", Toast.LENGTH_SHORT).show()
                        dismissAllowingStateLoss() // Dismiss if no barcode found when launched with URI
                    }
                }
                .addOnFailureListener {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Failed to analyse image", Toast.LENGTH_SHORT).show()
                    dismissAllowingStateLoss() // Dismiss on failure when launched with URI
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(requireContext(), "Error reading image", Toast.LENGTH_SHORT).show()
            dismissAllowingStateLoss() // Dismiss on error when launched with URI
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
                    dismissAllowingStateLoss()
                    return@launch
                }

                val json = JSONObject(body)
                if (json.optInt("status") == 1) {
                    val prod = json.getJSONObject("product")
                    val name = prod.optString("product_name", "").trim()
                    val apiImage = prod.optString("image_url", null)

                    val product = Product(
                        id = 0,
                        name = name,
                        expirationDate = null,
                        quantity = 1,
                        reminderDays = 0,
                        brand = prod.optString("brands", "").takeIf { it.isNotBlank() },
                        weight = prod.optString("quantity", "").takeIf { it.isNotBlank() },
                        imageUri = apiImage ?: uploadedImage.toString(),
                        isFavorite = false
                    )

                    showLoading(false)
                    startActivity(Intent(requireContext(), ManualEntryActivity::class.java).apply {
                        putExtra("product", product)
                        putExtra("isEdit", false)
                    })
                    dismissAllowingStateLoss()
                } else {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Product not found", Toast.LENGTH_SHORT).show()
                    dismissAllowingStateLoss()
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(requireContext(), "Error fetching product info", Toast.LENGTH_SHORT).show()
                dismissAllowingStateLoss()
            }
        }
    }

    companion object {
        private const val ARG_INITIAL_IMAGE_URI = "initial_image_uri"

        fun newInstance(initialImageUri: Uri? = null): AddProductBottomSheet {
            val fragment = AddProductBottomSheet()
            initialImageUri?.let {
                val args = Bundle()
                args.putParcelable(ARG_INITIAL_IMAGE_URI, it)
                fragment.arguments = args
            }
            return fragment
        }
    }
}
