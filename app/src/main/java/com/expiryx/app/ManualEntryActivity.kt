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
import java.text.SimpleDateFormat
import java.util.*
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import java.util.regex.*

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
    private var productBarcode: String? = null
    private var selectedWeightUnit: String = "g"

    private val safeTextFilter = InputFilter { source, _, _, _, _, _ ->
        val allowed = Pattern.compile("^[a-zA-Z0-9 .,'&()\\-]*$")
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
                } catch (_: SecurityException) { /* ignore */ }
                selectedImageUri = it.toString()
                Glide.with(this).load(it).into(binding.imageProductPreview)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManualEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFilters()
        setupWeightUnitSpinner()
        loadProductData()
        setupListeners()
    }

    private fun setupFilters() {
        binding.editTextProductName.filters = arrayOf(safeTextFilter, InputFilter.LengthFilter(50))
        binding.editTextBrand.filters = arrayOf(safeTextFilter, InputFilter.LengthFilter(50))
        // Quantity: Max 4 digits (1-9999). Numerical check in saveProduct().
        binding.editTextQuantity.filters = arrayOf(InputFilter.LengthFilter(4))
        // Weight: Max 5 digits (1–99999). Numerical check in saveProduct().
        binding.editTextWeight.filters = arrayOf(InputFilter.LengthFilter(5))
        // Reminder: Max 3 digits (1–365). Numerical check in saveProduct().
        binding.editTextReminder.filters = arrayOf(InputFilter.LengthFilter(3))
    }

    private fun setupWeightUnitSpinner() {
        val weightUnits = arrayOf("g", "ml")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, weightUnits)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerWeightUnit.adapter = adapter

        binding.spinnerWeightUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedWeightUnit = weightUnits[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadProductData() {
        editingProduct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("product", Product::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("product")
        }

        selectedImageUri = intent.getStringExtra("imageUri") ?: editingProduct?.imageUri
        productBarcode = intent.getStringExtra("barcode") ?: editingProduct?.barcode

        editingProduct?.let { product ->
            binding.editTextProductName.setText(product.name)
            binding.editTextBrand.setText(product.brand)
            product.expirationDate?.let {
                expiryMillis = it
                binding.editTextExpirationDate.setText(dateFormat.format(Date(it)))
            }
            binding.editTextQuantity.setText(product.quantity.toString())
            binding.editTextReminder.setText(product.reminderDays.toString())
            binding.editTextWeight.setText(product.weight?.toString() ?: "")
            binding.checkboxFavorite.isChecked = product.isFavorite

            // Set weight unit spinner
            val weightUnitPosition = if (product.weightUnit == "ml") 1 else 0
            binding.spinnerWeightUnit.setSelection(weightUnitPosition)
            selectedWeightUnit = product.weightUnit
        }

        if (!productBarcode.isNullOrBlank()) {
            binding.textViewBarcodeValue.text = getString(R.string.barcode_label) + " " + productBarcode
            binding.textViewBarcodeValue.visibility = View.VISIBLE
        } else {
            binding.textViewBarcodeValue.visibility = View.GONE
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
                val calendar = Calendar.getInstance().apply {
                    set(y, m, d, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
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

        if (binding.editTextExpirationDate.text.toString().trim().isEmpty()) {
            binding.editTextExpirationDate.error = "Expiry date is required"
            return
        }
        val finalExpiryMillis = expiryMillis ?: return

        val brand = binding.editTextBrand.text.toString().trim().takeIf { it.isNotBlank() }

        val qtyString = binding.editTextQuantity.text.toString()
        val qtyInt = qtyString.toIntOrNull()
        if (qtyInt == null || qtyInt < 1 || qtyInt > 9999) {
            binding.editTextQuantity.error = "Quantity must be between 1 and 9999"
            return
        }

        // Reminder validation (1–365)
        val reminderString = binding.editTextReminder.text.toString()
        val reminder = reminderString.toIntOrNull()
        if (reminder == null || reminder < 1 || reminder > 365) {
            binding.editTextReminder.error = "Reminder must be between 1 and 365 days"
            return
        }

        // Weight validation (optional, 1–99999)
        val weightString = binding.editTextWeight.text.toString()
        val parsedWeight = weightString.toIntOrNull()
        val finalWeight: Int?
        if (weightString.isNotBlank()) {
            if (parsedWeight == null || parsedWeight < 1 || parsedWeight > 99999) {
                binding.editTextWeight.error = "Weight must be between 1 and 99999"
                return
            }
            finalWeight = parsedWeight
        } else {
            finalWeight = null // Optional
        }

        val currentTime = System.currentTimeMillis()
        val isEditing = editingProduct != null && editingProduct!!.id != 0

        val product = Product(
            id = editingProduct?.id ?: 0,
            name = name,
            brand = brand,
            expirationDate = finalExpiryMillis,
            quantity = qtyInt,
            reminderDays = reminder,
            weight = finalWeight,
            weightUnit = selectedWeightUnit,
            imageUri = selectedImageUri,
            isFavorite = binding.checkboxFavorite.isChecked,
            barcode = productBarcode,
            dateAdded = editingProduct?.dateAdded ?: currentTime,
            dateModified = if (isEditing) currentTime else null
        )

        if (isEditing) {
            productViewModel.update(product)
            editingProduct?.let { NotificationScheduler.cancelForProduct(this, it) }
            NotificationScheduler.scheduleForProduct(this, product)
            Toast.makeText(this, "Product updated", Toast.LENGTH_SHORT).show()
        } else {
            productViewModel.insert(product)
            NotificationScheduler.scheduleForProduct(this, product)
            Toast.makeText(this, "Product saved", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}

