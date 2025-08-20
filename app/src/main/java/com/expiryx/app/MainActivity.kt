package com.expiryx.app

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recycler = findViewById<RecyclerView>(R.id.recyclerProducts)
        val emptyState = findViewById<View>(R.id.emptyStateContainer)
        val addButton = findViewById<ImageButton>(R.id.btnAddProduct)

        adapter = ProductAdapter(listOf())
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        val db = AppDatabase.getDatabase(this)

        // ✅ Observe LiveData
        db.productDao().getAll().observe(this) { products ->
            if (products.isEmpty()) {
                recycler.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
            } else {
                recycler.visibility = View.VISIBLE
                emptyState.visibility = View.GONE
                adapter.updateProducts(products) // refresh adapter
            }
        }

        // Open add-product bottom sheet
        addButton.setOnClickListener {
            val bottomSheet = AddProductBottomSheet()
            bottomSheet.show(supportFragmentManager, "AddProductBottomSheet")
        }
    }
}

