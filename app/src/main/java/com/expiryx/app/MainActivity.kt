package com.expiryx.app

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var addButton: FloatingActionButton
    private lateinit var adapter: ProductAdapter

    // Use the ViewModel *factory* because ProductViewModel needs a DAO
    private val productViewModel: ProductViewModel by viewModels {
        val dao = AppDatabase.getDatabase(this).productDao()
        ProductViewModelFactory(dao)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerProducts)
        emptyState = findViewById(R.id.emptyStateContainer)
        addButton = findViewById(R.id.btnAddProduct)

        adapter = ProductAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Observe DB changes via LiveData
        productViewModel.allProducts.observe(this) { products ->
            if (products.isNullOrEmpty()) {
                recyclerView.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyState.visibility = View.GONE
                adapter.submitList(products)
            }
        }

        addButton.setOnClickListener {
            AddProductBottomSheet().show(supportFragmentManager, "AddProductBottomSheet")
        }
    }
}
