package com.expiryx.app

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val addButton = findViewById<ImageButton>(R.id.btnAddProduct)
        addButton.setOnClickListener {
            Toast.makeText(this, "Add product clicked", Toast.LENGTH_SHORT).show()
            // TODO: Open popup for OCR / Barcode / Manual Entry
        }
    }
}
