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
        val saveButton = findViewById<Button>(R.id.buttonUploadImage) // repurposed as Save
        val cancelButton = findViewById<Button>(R.id.buttonCancel)

        saveButton.text = "Save Product"

        // Open a DatePicker when tapping the expiration field
        expirationField.setOnClickListener {
            val cal = Calendar.getInstance()
            val dlg = DatePickerDialog(
                this,
                { _, y, m, d ->
                    val picked = Calendar.getInstance().apply {
                        set(Calendar.YEAR, y)
                        set(Calendar.MONTH, m)
                        set(Calendar.DAY_OF_MONTH, d)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
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

            // If user typed a date manually, try to parse it
            val expiryMillis = selectedExpiryMillis ?: run {
                val text = expirationField.text.toString().trim()
                if (text.isNotEmpty()) {
                    try { dateFormat.parse(text)?.time } catch (_: Exception) { null }
                } else null
            } ?: run {
                expirationField.error = "Pick an expiration date"
                return@setOnClickListener
            }

            val product = Product(
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
                db.productDao().insert(product)
                finish()
            }
        }

        cancelButton.setOnClickListener { finish() }
    }
}
