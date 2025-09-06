package com.expiryx.app

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.expiryx.app.databinding.ActivityManualEntryBinding
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class ManualEntryActivity : AppCompatActivity() {

    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory((application as ProductApplication).repository)
    }
    private lateinit var binding: ActivityManualEntryBinding

    private val dateFormat =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }

    private var editingProduct: Product? = null
    private var expiryMillis: Long? = null
    private var selectedImageUri: String? = null

    // Regex filter for safe text input
    private val safeTextFilter = InputFilter { source, _, _, _, _, _ ->
        val allowed = Pattern.compile("^[a-zA-Z0-9 .,'&()\\-]*$") // Allowed hyphen
        if (!allowed.matcher(source).matches()) "" else source
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) { /* ignore */
                }
                selectedImageUri = it.toString()
                Glide.with(this).load(it).into(binding.imageProductPreview)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManualEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFilters()
        loadProductData()
        setupListeners()
    }

    private fun setupFilters() {
        binding.editTextProductName.filters = arrayOf(safeTextFilter, InputFilter.LengthFilter(50))
        binding.editTextBrand.filters = arrayOf(safeTextFilter, InputFilter.LengthFilter(50))
        binding.editTextWeight.filters = arrayOf(InputFilter.LengthFilter(10))
    }

    private fun loadProductData() {
        editingProduct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("product", Product::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("product")
        }

        selectedImageUri = intent.getStringExtra("imageUri") ?: editingProduct?.imageUri

        editingProduct?.let { product ->
            binding.editTextProductName.setText(product.name)
            binding.editTextBrand.setText(product.brand)
            product.expirationDate?.let {
                expiryMillis = it
                binding.editTextExpirationDate.setText(dateFormat.format(Date(it)))
            }
            binding.editTextQuantity.setText(product.quantity.toString())
            binding.editTextReminder.setText(product.reminderDays.toString())
            binding.editTextWeight.setText(product.weight)
            binding.checkboxFavorite.isChecked = product.isFavorite
        }

        if (!selectedImageUri.isNullOrBlank()) {
            Glide.with(this).load(Uri.parse(selectedImageUri))
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(binding.imageProductPreview)
        } else {
            binding.imageProductPreview.setImageResource(R.drawable.ic_placeholder)
        }
    }

    private fun setupListeners() {
        binding.imageProductPreview.setOnClickListener { pickImageLauncher.launch(arrayOf("image/*")) }
        binding.imageProductPreview.setOnLongClickListener {
            selectedImageUri = null
            binding.imageProductPreview.setImageResource(R.drawable.ic_placeholder)
            Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show()
            true
        }

        binding.editTextExpirationDate.setOnClickListener { showDatePicker() }
        binding.buttonSaveProduct.setOnClickListener { saveProduct() }
        binding.buttonCancel.setOnClickListener { finish() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        editingProduct?.expirationDate?.let { cal.timeInMillis = it }
        DatePickerDialog(
            this,
            { _, y, m, d ->
                val calendar = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59); set(Calendar.MILLISECOND, 999) }
                expiryMillis = calendar.timeInMillis
                binding.editTextExpirationDate.setText(dateFormat.format(calendar.time))
            },
            cal[Calendar.YEAR],
            cal[Calendar.MONTH],
            cal[Calendar.DAY_OF_MONTH]
        ).show()
    }

    private fun saveProduct() {
        val name = binding.editTextProductName.text.toString().trim()
        if (name.isEmpty()) {
            binding.editTextProductName.error = "Product name is required"
            return
        }

        // Validate date
        if (binding.editTextExpirationDate.text.toString().trim().isEmpty()) {
            binding.editTextExpirationDate.error = "Expiry date is required"
            return
        }
        val finalExpiryMillis = expiryMillis ?: return // Should be set by now if date is not empty

        val brand = binding.editTextBrand.text.toString().trim().takeIf { it.isNotBlank() }
        val qty = binding.editTextQuantity.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
        val reminder = binding.editTextReminder.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0
        val weight = binding.editTextWeight.text.toString().trim().takeIf { it.isNotBlank() }

        val product = Product(
            id = editingProduct?.id ?: 0,
            name = name,
            brand = brand,
            expirationDate = finalExpiryMillis,
            quantity = qty,
            reminderDays = reminder,
            weight = weight,
            imageUri = selectedImageUri,
            isFavorite = binding.checkboxFavorite.isChecked
        )

        if (editingProduct != null) {
            productViewModel.update(product)
            Toast.makeText(this, "Product updated", Toast.LENGTH_SHORT).show()
        } else {
            productViewModel.insert(product)
            Toast.makeText(this, "Product saved", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}