package com.expiryx.app

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class ManualEntryActivity : AppCompatActivity() {

    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory((application as ProductApplication).repository)
    }

    private lateinit var nameInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var quantityInput: EditText
    private lateinit var reminderInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var favoriteCheck: CheckBox
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var imagePreview: ImageView

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        isLenient = false
    }
    private var editingProduct: Product? = null
    private var expiryMillis: Long? = null
    private var selectedImageUri: String? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {}
                selectedImageUri = it.toString()
                Glide.with(this).load(it).into(imagePreview)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_entry)

        nameInput = findViewById(R.id.editTextProductName)
        dateInput = findViewById(R.id.editTextExpirationDate)
        quantityInput = findViewById(R.id.editTextQuantity)
        reminderInput = findViewById(R.id.editTextReminder)
        weightInput = findViewById(R.id.editTextWeight)
        notesInput = findViewById(R.id.editTextNotes)
        favoriteCheck = findViewById(R.id.checkboxFavorite)
        saveButton = findViewById(R.id.buttonSaveProduct)
        cancelButton = findViewById(R.id.buttonCancel)
        imagePreview = findViewById(R.id.imageProductPreview)

        val isEdit = intent.getBooleanExtra("isEdit", false)
        editingProduct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("product", Product::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra("product")
        }

        selectedImageUri = intent.getStringExtra("imageUri") ?: editingProduct?.imageUri

        editingProduct?.let { product ->
            if (product.name.isNotBlank()) nameInput.setText(product.name)
            product.expirationDate?.let {
                expiryMillis = it
                dateInput.setText(dateFormat.format(Date(it)))
            }
            quantityInput.setText(product.quantity.toString())
            reminderInput.setText(product.reminderDays.toString())
            weightInput.setText(product.weight ?: "")
            notesInput.setText(product.notes ?: "")
            favoriteCheck.isChecked = product.isFavorite
        }

        if (!selectedImageUri.isNullOrBlank()) {
            Glide.with(this).load(Uri.parse(selectedImageUri)).into(imagePreview)
        } else {
            imagePreview.setImageResource(R.drawable.ic_placeholder)
        }

        imagePreview.setOnClickListener { pickImageLauncher.launch(arrayOf("image/*")) }
        imagePreview.setOnLongClickListener {
            selectedImageUri = null
            imagePreview.setImageResource(R.drawable.ic_placeholder)
            Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show()
            true
        }

        dateInput.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d, 23, 59, 59) // end of day
                cal.set(Calendar.MILLISECOND, 999)
                expiryMillis = cal.timeInMillis
                dateInput.setText(dateFormat.format(cal.time))
            }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]).show()
        }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                nameInput.error = "Name required"; return@setOnClickListener
            }

            val expiry = expiryMillis ?: run {
                val txt = dateInput.text.toString().trim()
                if (txt.isEmpty()) {
                    dateInput.error = "Expiry date required"; return@setOnClickListener
                }
                try {
                    // Parse -> set to end-of-day for that date
                    val parsed = dateFormat.parse(txt) ?: throw ParseException("Invalid", 0)
                    val cal = Calendar.getInstance().apply {
                        time = parsed
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    cal.timeInMillis
                } catch (_: ParseException) {
                    dateInput.error = "Invalid date (use dd/MM/yyyy)"
                    return@setOnClickListener
                }
            }

            val product = Product(
                id = editingProduct?.id ?: 0,
                name = name,
                expirationDate = expiry,
                quantity = quantityInput.text.toString().toIntOrNull() ?: 1,
                reminderDays = reminderInput.text.toString().toIntOrNull() ?: 0,
                notes = notesInput.text.toString().takeIf { it.isNotEmpty() },
                weight = weightInput.text.toString().takeIf { it.isNotEmpty() },
                imageUri = selectedImageUri,
                isFavorite = favoriteCheck.isChecked
            )

            if (isEdit) {
                productViewModel.update(product)
                Toast.makeText(this, "Product updated", Toast.LENGTH_SHORT).show()
            } else {
                productViewModel.insert(product)
                Toast.makeText(this, "Product saved", Toast.LENGTH_SHORT).show()
            }
            finish()
        }

        cancelButton.setOnClickListener { finish() }
    }
}
