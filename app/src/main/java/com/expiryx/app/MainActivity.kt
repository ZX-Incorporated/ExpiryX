package com.expiryx.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var emptyState: View
    private lateinit var db: AppDatabase
    private lateinit var addButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recycler = findViewById(R.id.recyclerProducts)
        emptyState = findViewById(R.id.emptyStateContainer)
        addButton = findViewById(R.id.btnAddProduct) // ← hook up the plus button
        db = AppDatabase.getDatabase(this)

        // Observe product list
        db.productDao().getAll().observe(this, Observer { products ->
            if (products.isNullOrEmpty()) {
                recycler.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
            } else {
                recycler.visibility = View.VISIBLE
                emptyState.visibility = View.GONE
                recycler.layoutManager = LinearLayoutManager(this)
                recycler.adapter = ProductAdapter(products)
            }
        })

        // Handle Plus button click → open bottom sheet
        addButton.setOnClickListener {
            val bottomSheet = AddProductBottomSheet()
            bottomSheet.show(supportFragmentManager, "AddProductBottomSheet")
        }
    }
}
