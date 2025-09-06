package com.expiryx.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.expiryx.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory((application as ProductApplication).repository)
    }
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ProductAdapter

    private var allProducts: List<Product> = emptyList()
    private var showFavoritesOnly = false
    private var sortMode = SortMode.EXPIRY_ASC

    private val requestNotifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) scheduleAllProductNotifications()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationUtils.createChannel(this)
        productViewModel.archiveExpiredProducts()

        setupRecyclerView()
        setupListeners()

        productViewModel.allProducts.observe(this) { products ->
            allProducts = products
            applyFiltersAndSort()
            checkAndMaybeRequestNotificationPermission()
            scheduleAllProductNotifications()
        }

        highlightBottomNav(BottomTab.HOME)
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(
            onFavoriteClick = { p -> productViewModel.update(p.copy(isFavorite = !p.isFavorite)) },
            onItemClick = { p -> ProductDetailBottomSheet.newInstance(p).show(supportFragmentManager, "Detail") },
            onDeleteLongPress = { deleteProductWithConfirmation(it) }
        )
        binding.recyclerProducts.layoutManager = LinearLayoutManager(this)
        binding.recyclerProducts.adapter = adapter
    }

    private fun setupListeners() {
        // --- Top Bar Actions ---
        binding.btnSearch.setOnClickListener {
            if (binding.searchView.visibility == View.VISIBLE) closeSearch() else openSearch()
        }
        binding.btnSortBy.setOnClickListener { showSortMenu() }
        binding.btnFavorite.setOnClickListener { toggleFavoritesFilter() }

        // --- Search View ---
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                hideKeyboard()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                applyFiltersAndSort()
                return true
            }
        })
        binding.searchView.setOnCloseListener {
            closeSearch()
            true
        }

        // --- FAB ---
        binding.btnAddProduct.setOnClickListener {
            AddProductBottomSheet().show(supportFragmentManager, "AddProduct")
        }

        // --- Bottom Navigation ---
        binding.navHome.setOnClickListener { highlightBottomNav(BottomTab.HOME) }
        binding.navCart.setOnClickListener { Toast.makeText(this, "Store coming soon…", Toast.LENGTH_SHORT).show() }
        binding.navHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(0, 0); finish()
        }
        binding.navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(0, 0); finish()
        }
    }

    private fun openSearch() {
        binding.searchView.visibility = View.VISIBLE
        binding.searchView.isIconified = false
        binding.searchView.requestFocus()
        showKeyboard()
    }

    private fun closeSearch() {
        binding.searchView.setQuery("", false)
        binding.searchView.clearFocus()
        binding.searchView.visibility = View.GONE
        hideKeyboard()
        applyFiltersAndSort() // Re-apply filters without the search query
    }

    private fun toggleFavoritesFilter() {
        showFavoritesOnly = !showFavoritesOnly
        binding.btnFavorite.setImageResource(if (showFavoritesOnly) R.drawable.ic_heart_filled else R.drawable.ic_heart_unfilled)
        applyFiltersAndSort()
    }

    private fun showSortMenu() {
        val popup = PopupMenu(this, binding.btnSortBy)
        popup.menuInflater.inflate(R.menu.menu_sort_main, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            sortMode = when (item.itemId) {
                R.id.sort_expiry_soon -> SortMode.EXPIRY_ASC
                R.id.sort_expiry_late -> SortMode.EXPIRY_DESC
                R.id.sort_name_asc -> SortMode.NAME_ASC
                R.id.sort_name_desc -> SortMode.NAME_DESC
                R.id.sort_quantity_asc -> SortMode.QTY_ASC
                R.id.sort_quantity_desc -> SortMode.QTY_DESC
                R.id.sort_favourites -> SortMode.FAVORITES_FIRST
                else -> SortMode.EXPIRY_ASC
            }
            applyFiltersAndSort()
            true
        }
        popup.show()
    }

    private fun applyFiltersAndSort() {
        var processedList = allProducts

        // 1. Filter by favorites if toggled
        if (showFavoritesOnly) {
            processedList = processedList.filter { it.isFavorite }
        }

        // 2. Filter by search query
        val query = binding.searchView.query.toString().trim()
        if (query.isNotBlank()) {
            processedList = processedList.filter { product ->
                product.name.contains(query, ignoreCase = true) ||
                        (product.brand?.contains(query, ignoreCase = true) ?: false) ||
                        (product.weight?.contains(query, ignoreCase = true) ?: false)
            }
        }

        // 3. Sort the results
        processedList = when (sortMode) {
            SortMode.NAME_ASC -> processedList.sortedBy { it.name.lowercase() }
            SortMode.NAME_DESC -> processedList.sortedByDescending { it.name.lowercase() }
            SortMode.EXPIRY_ASC -> processedList.sortedBy { it.expirationDate ?: Long.MAX_VALUE }
            SortMode.EXPIRY_DESC -> processedList.sortedByDescending { it.expirationDate ?: Long.MIN_VALUE }
            SortMode.QTY_ASC -> processedList.sortedBy { it.quantity }
            SortMode.QTY_DESC -> processedList.sortedByDescending { it.quantity }
            SortMode.FAVORITES_FIRST -> {
                processedList.sortedWith(
                    compareByDescending<Product> { it.isFavorite }
                        .thenBy { it.expirationDate ?: Long.MAX_VALUE }
                )
            }
        }

        // 4. Update the UI
        updateListUI(processedList)
    }

    private fun updateListUI(products: List<Product>) {
        // ✅ MINOR FIX: Pass the sortMode to the adapter
        adapter.updateData(products, sortMode)

        if (products.isEmpty()) {
            binding.recyclerProducts.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.VISIBLE
            binding.emptyStateTitle.text = when {
                binding.searchView.query.isNotEmpty() -> "No results found"
                showFavoritesOnly -> "No favorites yet"
                else -> "No products yet"
            }
        } else {
            binding.recyclerProducts.visibility = View.VISIBLE
            binding.emptyStateContainer.visibility = View.GONE
        }
    }

    private fun checkAndMaybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                requestNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun scheduleAllProductNotifications() {
        allProducts.forEach { NotificationScheduler.scheduleForProduct(this, it) }
    }

    fun deleteProductWithConfirmation(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete ${product.name}?")
            .setPositiveButton("Delete") { _, _ ->
                productViewModel.delete(product)
                NotificationScheduler.cancelForProduct(this, product)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun markProductAsUsed(product: Product) {
        productViewModel.markAsUsed(product)
        Toast.makeText(this, "${product.name} marked as used", Toast.LENGTH_SHORT).show()
    }

    fun editProduct(product: Product) {
        val intent = Intent(this, ManualEntryActivity::class.java).apply {
            putExtra("isEdit", true)
            putExtra("product", product)
        }
        startActivity(intent)
    }

    private fun highlightBottomNav(tab: BottomTab) {
        binding.navHome.setImageResource(if (tab == BottomTab.HOME) R.drawable.ic_home_filled else R.drawable.ic_home_unfilled)
        binding.navHistory.setImageResource(if (tab == BottomTab.HISTORY) R.drawable.ic_clock_filled else R.drawable.ic_clock_unfilled)
        binding.navSettings.setImageResource(if (tab == BottomTab.SETTINGS) R.drawable.ic_settings_filled else R.drawable.ic_settings_unfilled)
        binding.navCart.setImageResource(R.drawable.ic_cart)
    }

    private fun showKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    enum class SortMode { NAME_ASC, NAME_DESC, EXPIRY_ASC, EXPIRY_DESC, QTY_ASC, QTY_DESC, FAVORITES_FIRST }
    enum class BottomTab { HOME, CART, HISTORY, SETTINGS }
}