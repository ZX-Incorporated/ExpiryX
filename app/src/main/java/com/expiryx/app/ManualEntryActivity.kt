package com.expiryx.app

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ManualEntryActivity : AppCompatActivity() {
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

        val saveButton = findViewById<Button>(R.id.buttonUploadImage) // reuse this button for now

        saveButton.text = "Save Product"

        saveButton.setOnClickListener {
            val product = Product(
                name = nameField.text.toString(),
                expirationDate = expirationField.text.toString(),
                quantity = quantityField.text.toString().toIntOrNull() ?: 1,
                reminderDays = reminderField.text.toString().toIntOrNull() ?: 0,
                weight = weightField.text.toString(),
                notes = notesField.text.toString(),
                isFavorite = favoriteCheck.isChecked
            )

            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(this@ManualEntryActivity)
                db.productDao().insert(product)
                finish() // close after saving
            }
        }

        val cancelButton = findViewById<Button>(R.id.buttonCancel)
        cancelButton.setOnClickListener {
            finish()
        }
    }
}
