package com.expiryx.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory(AppDatabase.getDatabase(this).productDao())
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var addButton: FloatingActionButton
    private lateinit var adapter: ProductAdapter
    private lateinit var searchView: SearchView
    private lateinit var btnSearch: ImageView

    private var allProducts: List<Product> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerProducts)
        emptyState = findViewById(R.id.emptyStateContainer)
        addButton = findViewById(R.id.btnAddProduct)
        searchView = findViewById(R.id.searchView)
        btnSearch = findViewById(R.id.btnSearch)

        adapter = ProductAdapter(
            onItemClick = { product ->
                val bottomSheet = ProductDetailBottomSheet.newInstance(product)
                bottomSheet.show(supportFragmentManager, "ProductDetailBottomSheet")
            },
            onFavoriteClick = { product ->
                val updated = product.copy(isFavorite = !product.isFavorite)
                productViewModel.update(updated)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Observe products
        productViewModel.allProducts.observe(this) { products ->
            allProducts = products ?: emptyList()
            updateList(allProducts)
        }

        // Toggle search view visibility
        btnSearch.setOnClickListener {
            searchView.visibility =
                if (searchView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Search filtering
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val filtered = if (!newText.isNullOrBlank()) {
                    allProducts.filter { it.name.contains(newText, ignoreCase = true) }
                } else allProducts
                updateList(filtered)
                return true
            }
        })

        addButton.setOnClickListener {
            val bottomSheet = AddProductBottomSheet()
            bottomSheet.show(supportFragmentManager, "AddProductBottomSheet")
        }
    }

    private fun updateList(products: List<Product>) {
        if (products.isNullOrEmpty()) {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            adapter.submitList(mapToListItems(products))
        }
    }

    private fun mapToListItems(products: List<Product>): List<ProductListItem> {
        val items = mutableListOf<ProductListItem>()
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L

        // Expired (last 7 days)
        val expired = products.filter { it.expirationDate < now && it.expirationDate >= now - 7 * oneDay }
        if (expired.isNotEmpty()) {
            items.add(ProductListItem.Header("Expired (7 days)", R.color.red))
            expired.forEach { items.add(ProductListItem.Item(it)) }
        }

        // Expires in 24h
        val day1 = products.filter { it.expirationDate in now..(now + oneDay) }
        if (day1.isNotEmpty()) {
            items.add(ProductListItem.Header("Expires in 24 hours", R.color.orange))
            day1.forEach { items.add(ProductListItem.Item(it)) }
        }

        // Expires in 3 days
        val day3 = products.filter { it.expirationDate in (now + oneDay)..(now + 3 * oneDay) }
        if (day3.isNotEmpty()) {
            items.add(ProductListItem.Header("Expires in 3 days", R.color.yellow))
            day3.forEach { items.add(ProductListItem.Item(it)) }
        }

        // Expires in 14 days
        val day14 = products.filter { it.expirationDate in (now + 3 * oneDay)..(now + 14 * oneDay) }
        if (day14.isNotEmpty()) {
            items.add(ProductListItem.Header("Expires in 14 days", R.color.green))
            day14.forEach { items.add(ProductListItem.Item(it)) }
        }

        // Expires in 3 months
        val month3 = products.filter { it.expirationDate in (now + 14 * oneDay)..(now + 90 * oneDay) }
        if (month3.isNotEmpty()) {
            items.add(ProductListItem.Header("Expires in 3 months", R.color.blue))
            month3.forEach { items.add(ProductListItem.Item(it)) }
        }

        // More than a year
        val year = products.filter { it.expirationDate > now + 365 * oneDay }
        if (year.isNotEmpty()) {
            items.add(ProductListItem.Header("Expires in more than a year", R.color.purple))
            year.forEach { items.add(ProductListItem.Item(it)) }
        }

        return items
    }

    fun deleteProductWithConfirmation(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete ${product.name}?")
            .setPositiveButton("Delete") { _, _ ->
                productViewModel.delete(product)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun editProduct(product: Product) {
        val intent = Intent(this, ManualEntryActivity::class.java).apply {
            putExtra("product", product)
        }
        startActivity(intent)
    }
}