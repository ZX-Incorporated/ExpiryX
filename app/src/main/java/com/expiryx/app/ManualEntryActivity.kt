package com.expiryx.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ManualEntryActivity : AppCompatActivity() {

    private var selectedExpiryMillis: Long? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var editingProduct: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_entry)

        val nameField = findViewById<EditText>(R.id.editTextProductName)
        val expirationField = findViewById<EditText>(R.id.editTextExpirationDate)
        val quantityField = findViewById<EditText>(R.id.editTextQuantity)
        val reminderField = findViewById<EditText>(R.id.editTextReminder)
        val weightField = findViewById<EditText>(R.id.editTextWeight)
        val notesField = findViewById<EditText>(R.id.editTextNotes)
        val favoriteCheck = findViewById<CheckBox>(R.id.checkboxFavorite)
        val saveButton = findViewById<Button>(R.id.buttonUploadImage) // reused as Save
        val cancelButton = findViewById<Button>(R.id.buttonCancel)

        saveButton.text = "Save Product"

        // ✅ Check if editing
        editingProduct = intent.getParcelableExtra("product")
        editingProduct?.let { product ->
            nameField.setText(product.name)
            expirationField.setText(dateFormat.format(Date(product.expirationDate)))
            selectedExpiryMillis = product.expirationDate
            quantityField.setText(product.quantity.toString())
            reminderField.setText(product.reminderDays.toString())
            weightField.setText(product.weight ?: "")
            notesField.setText(product.notes ?: "")
            favoriteCheck.isChecked = product.isFavorite
        }

        expirationField.setOnClickListener {
            val cal = Calendar.getInstance()
            val dlg = DatePickerDialog(
                this,
                { _, y, m, d ->
                    val picked = Calendar.getInstance().apply {
                        set(y, m, d, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    selectedExpiryMillis = picked.timeInMillis
                    expirationField.setText(dateFormat.format(Date(selectedExpiryMillis!!)))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dlg.show()
        }

        saveButton.setOnClickListener {
            val name = nameField.text.toString().trim()
            if (name.isEmpty()) {
                nameField.error = "Name required"
                return@setOnClickListener
            }

            val expiryMillis = selectedExpiryMillis ?: run {
                val text = expirationField.text.toString().trim()
                if (text.isNotEmpty()) dateFormat.parse(text)?.time else null
            } ?: run {
                expirationField.error = "Pick an expiration date"
                return@setOnClickListener
            }

            val product = editingProduct?.copy(
                name = name,
                expirationDate = expiryMillis,
                quantity = quantityField.text.toString().toIntOrNull() ?: 1,
                reminderDays = reminderField.text.toString().toIntOrNull() ?: 0,
                weight = weightField.text.toString(),
                notes = notesField.text.toString(),
                isFavorite = favoriteCheck.isChecked
            ) ?: Product(
                name = name,
                expirationDate = expiryMillis,
                quantity = quantityField.text.toString().toIntOrNull() ?: 1,
                reminderDays = reminderField.text.toString().toIntOrNull() ?: 0,
                weight = weightField.text.toString(),
                notes = notesField.text.toString(),
                isFavorite = favoriteCheck.isChecked
            )

            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(this@ManualEntryActivity)
                if (editingProduct != null) {
                    db.productDao().update(product)
                } else {
                    db.productDao().insert(product)
                }
                finish()
            }
        }

        cancelButton.setOnClickListener { finish() }
    }
}
