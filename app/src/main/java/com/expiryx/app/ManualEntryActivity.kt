package com.expiryx.app

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var editingProduct: Product? = null
    private var expiryMillis: Long? = null

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

        // Check if product passed
        editingProduct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("product", Product::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("product")
        }

        val imageUri = intent.getStringExtra("imageUri")
        if (imageUri != null) {
            imagePreview.setImageURI(Uri.parse(imageUri))
        }

        editingProduct?.let { product ->
            nameInput.setText(product.name)
            product.expirationDate?.let { dateInput.setText(dateFormat.format(Date(it))) }
            quantityInput.setText(product.quantity.toString())
            reminderInput.setText(product.reminderDays.toString())
            weightInput.setText(product.weight ?: "")
            notesInput.setText(product.notes ?: "")
            favoriteCheck.isChecked = product.isFavorite
            product.imageUri?.let { imagePreview.setImageURI(Uri.parse(it)) }
        }

        dateInput.setOnClickListener {
            val cal = Calendar.getInstance()
            val picker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    cal.set(year, month, day, 0, 0)
                    expiryMillis = cal.timeInMillis
                    dateInput.setText(dateFormat.format(cal.time))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            picker.show()
        }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                nameInput.error = "Name required"
                return@setOnClickListener
            }

            val expiry = expiryMillis
                ?: dateInput.text.toString().takeIf { it.isNotEmpty() }?.let {
                    dateFormat.parse(it)?.time
                }

            if (expiry == null) {
                dateInput.error = "Expiry date required"
                return@setOnClickListener
            }

            val newProduct = Product(
                id = editingProduct?.id ?: 0,
                name = name,
                expirationDate = expiry,
                quantity = quantityInput.text.toString().toIntOrNull() ?: 1,
                reminderDays = reminderInput.text.toString().toIntOrNull() ?: 0,
                notes = notesInput.text.toString().takeIf { it.isNotEmpty() },
                weight = weightInput.text.toString().takeIf { it.isNotEmpty() },
                imageUri = editingProduct?.imageUri ?: imageUri,
                isFavorite = favoriteCheck.isChecked
            )

            if (editingProduct == null) {
                productViewModel.insert(newProduct)
                Toast.makeText(this, "Product saved", Toast.LENGTH_SHORT).show()
            } else {
                productViewModel.update(newProduct)
                Toast.makeText(this, "Product updated", Toast.LENGTH_SHORT).show()
            }

            finish()
        }

        cancelButton.setOnClickListener { finish() }
    }
}
