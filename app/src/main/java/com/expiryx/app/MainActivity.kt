package com.expiryx.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory((application as ProductApplication).repository)
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var addButton: FloatingActionButton
    private lateinit var adapter: ProductAdapter
    private lateinit var searchView: SearchView
    private lateinit var btnSearch: ImageView
    private lateinit var btnSortBy: ImageView
    private lateinit var btnFavorite: ImageView

    private var allProducts: List<Product> = emptyList()
    private var showFavoritesOnly = false
    private var sortMode = SortMode.EXPIRY_ASC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerProducts)
        emptyState = findViewById(R.id.emptyStateContainer)
        addButton = findViewById(R.id.btnAddProduct)
        searchView = findViewById(R.id.searchView)
        btnSearch = findViewById(R.id.btnSearch)
        btnSortBy = findViewById(R.id.btnSortBy)
        btnFavorite = findViewById(R.id.btnFavorite)

        adapter = ProductAdapter(
            onFavoriteClick = { product ->
                val updated = product.copy(isFavorite = !product.isFavorite)
                productViewModel.update(updated)
            },
            onItemClick = { product ->
                val bottomSheet = ProductDetailBottomSheet.newInstance(product)
                bottomSheet.show(supportFragmentManager, "ProductDetailBottomSheet")
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Observe products from DB
        productViewModel.allProducts.observe(this) { products ->
            allProducts = products ?: emptyList()
            refreshList()
        }

        // 🔍 Toggle search
        btnSearch.setOnClickListener {
            searchView.visibility =
                if (searchView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // 🔍 Handle searching
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val filtered = if (!newText.isNullOrBlank()) {
                    allProducts.filter { it.name.contains(newText, ignoreCase = true) }
                } else allProducts
                updateList(filtered)
                return true
            }
        })

        // ➕ Add product
        addButton.setOnClickListener {
            val bottomSheet = AddProductBottomSheet()
            bottomSheet.show(supportFragmentManager, "AddProductBottomSheet")
        }

        // 🔀 Full sort menu
        btnSortBy.setOnClickListener { v ->
            val popup = PopupMenu(this, v)
            popup.menu.apply {
                add(0, 1, 0, "Expiry Date (Soonest First)")
                add(0, 2, 0, "Expiry Date (Latest First)")
                add(0, 3, 0, "Name (A–Z)")
                add(0, 4, 0, "Name (Z–A)")
                add(0, 5, 0, "Quantity (Low → High)")
                add(0, 6, 0, "Quantity (High → Low)")
                add(0, 7, 0, "Favorites First")
            }

            popup.setOnMenuItemClickListener { item ->
                sortMode = when (item.itemId) {
                    1 -> SortMode.EXPIRY_ASC
                    2 -> SortMode.EXPIRY_DESC
                    3 -> SortMode.NAME_ASC
                    4 -> SortMode.NAME_DESC
                    5 -> SortMode.QTY_ASC
                    6 -> SortMode.QTY_DESC
                    7 -> SortMode.FAVORITES_FIRST
                    else -> SortMode.EXPIRY_ASC
                }
                refreshList()
                Toast.makeText(this, "Sorted by ${item.title}", Toast.LENGTH_SHORT).show()
                true
            }
            popup.show()
        }

        // ❤️ Toggle favorites filter
        btnFavorite.setOnClickListener {
            showFavoritesOnly = !showFavoritesOnly
            btnFavorite.setImageResource(
                if (showFavoritesOnly) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_unfilled
            )
            refreshList()
        }
    }

    private fun refreshList() {
        var list = allProducts

        // filter favorites
        if (showFavoritesOnly) {
            list = list.filter { it.isFavorite }
        }

        // apply sorting
        list = when (sortMode) {
            SortMode.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            SortMode.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
            SortMode.EXPIRY_ASC -> list.sortedBy { it.expirationDate ?: Long.MAX_VALUE }
            SortMode.EXPIRY_DESC -> list.sortedByDescending { it.expirationDate ?: 0L }
            SortMode.QTY_ASC -> list.sortedBy { it.quantity }
            SortMode.QTY_DESC -> list.sortedByDescending { it.quantity }
            SortMode.FAVORITES_FIRST -> list.sortedByDescending { it.isFavorite }
        }

        updateList(list)
    }

    private fun updateList(products: List<Product>) {
        if (products.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            adapter.updateData(products)
        }
    }

    fun deleteProductWithConfirmation(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete ${product.name}?")
            .setPositiveButton("Delete") { _, _ -> productViewModel.delete(product) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun editProduct(product: Product) {
        val intent = Intent(this, ManualEntryActivity::class.java).apply {
            putExtra("product", product)
            putExtra("isEdit", true)
        }
        startActivity(intent)
    }
    fun markProductAsUsed(product: Product) {
        // TODO: Move product to history table
        Toast.makeText(this, "${product.name} marked as used", Toast.LENGTH_SHORT).show()
    }


    enum class SortMode {
        NAME_ASC, NAME_DESC,
        EXPIRY_ASC, EXPIRY_DESC,
        QTY_ASC, QTY_DESC,
        FAVORITES_FIRST
    }
}
