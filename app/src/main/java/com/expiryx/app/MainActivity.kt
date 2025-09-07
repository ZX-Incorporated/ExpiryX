package com.expiryx.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.expiryx.app.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ProductAdapter

    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory((application as ProductApplication).repository)
    }

    private var allProducts: List<Product> = emptyList()
    private var showFavoritesOnly = false

    enum class SortMode {
        EXPIRY_ASC, EXPIRY_DESC,
        ALPHA_AZ, ALPHA_ZA,
        ADDED_ASC, ADDED_DESC,
        QTY_ASC, QTY_DESC,
        FAVORITES_FIRST
    }

    private var sortMode: SortMode = SortMode.EXPIRY_ASC

    private val manualEntryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {}
            // Launch AddProductBottomSheet with the selected URI using newInstance
            AddProductBottomSheet.newInstance(uri).show(supportFragmentManager, "AddProductWithUriTag")
        }

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

        setupRecycler()
        setupObservers()
        setupListeners()
        highlightBottomNav(BottomTab.HOME)

        checkAndMaybeRequestNotificationPermission()
    }

    private fun setupRecycler() {
        adapter = ProductAdapter(
            onFavoriteClick = { p -> productViewModel.update(p.copy(isFavorite = !p.isFavorite)) },
            onItemClick = { p -> ProductDetailBottomSheet.newInstance(p).show(supportFragmentManager, "Detail") },
            onDeleteLongPress = { deleteProductWithConfirmation(it) }
        )
        binding.recyclerProducts.layoutManager = LinearLayoutManager(this)
        binding.recyclerProducts.adapter = adapter
    }

    private fun setupObservers() {
        productViewModel.allProducts.observe(this) { products ->
            allProducts = products
            refreshList()
            scheduleAllProductNotifications()
            productViewModel.archiveExpiredProducts()
        }
    }
    fun editProduct(product: Product) {
        val intent = Intent(this, ManualEntryActivity::class.java).apply {
            putExtra("product", product)
            putExtra("imageUri", product.imageUri)
        }
        startActivity(intent)
    }


    private fun setupListeners() {
        binding.btnAddProduct.setOnClickListener { showAddProductOptions() }
        binding.btnSortBy.setOnClickListener { showSortOptions(it) }

        binding.btnFavorite.setOnClickListener {
            showFavoritesOnly = !showFavoritesOnly
            binding.btnFavorite.setImageResource(
                if (showFavoritesOnly) R.drawable.ic_heart_filled else R.drawable.ic_heart_unfilled
            )
            refreshList()
        }

        binding.btnSearch.setOnClickListener {
            if (binding.searchView.visibility == View.VISIBLE) closeSearchCompletely() else openSearch()
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val filtered = if (!newText.isNullOrBlank()) {
                    val query = newText.lowercase(Locale.getDefault())
                    allProducts.filter { product ->
                        product.name.lowercase(Locale.getDefault()).contains(query) ||
                        (product.brand?.lowercase(Locale.getDefault())?.contains(query) ?: false) ||
                        (product.barcode?.lowercase(Locale.getDefault())?.contains(query) ?: false)
                    }
                } else allProducts
                updateList(filtered, fromSearch = !newText.isNullOrBlank())
                return true
            }
        })
        binding.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.searchView.query.isNullOrEmpty()) closeSearchCompletely()
        }

        val closeBtn = binding.searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeBtn?.setOnClickListener {
            if (!binding.searchView.query.isNullOrEmpty()) binding.searchView.setQuery("", false)
            else closeSearchCompletely()
        }

        binding.navHomeWrapper.setOnClickListener { highlightBottomNav(BottomTab.HOME) }
        binding.navCartWrapper.setOnClickListener { Toast.makeText(this, "Store feature coming soon!", Toast.LENGTH_SHORT).show() }
        binding.navHistoryWrapper.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(0, 0)
            finish()
        }
        binding.navSettingsWrapper.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private fun showAddProductOptions() {
        // This is the primary way to show the AddProductBottomSheet for general use.
        // The pickImageLauncher in MainActivity is a specific case for when an image is ALREADY chosen by MainActivity itself.
        AddProductBottomSheet.newInstance().show(supportFragmentManager, "AddProductGeneralTag")
    }

    private fun showSortOptions(anchor: View) {
        val popup = android.widget.PopupMenu(this, anchor)
        popup.menu.apply {
            add(0, 1, 0, "Expiry Soonest")
            add(0, 2, 0, "Expiry Latest")
            add(0, 3, 0, "Name A–Z")
            add(0, 4, 0, "Name Z–A")
            add(0, 5, 0, "Quantity Low→High")
            add(0, 6, 0, "Quantity High→Low")
            add(0, 7, 0, "Favorites First")
            add(0, 8, 0, "Added: Oldest First")
            add(0, 9, 0, "Added: Newest First")
        }
        popup.setOnMenuItemClickListener { item ->
            sortMode = when (item.itemId) {
                1 -> SortMode.EXPIRY_ASC
                2 -> SortMode.EXPIRY_DESC
                3 -> SortMode.ALPHA_AZ
                4 -> SortMode.ALPHA_ZA
                5 -> SortMode.QTY_ASC
                6 -> SortMode.QTY_DESC
                7 -> SortMode.FAVORITES_FIRST
                8 -> SortMode.ADDED_ASC
                9 -> SortMode.ADDED_DESC
                else -> SortMode.EXPIRY_ASC
            }
            refreshList()
            true
        }
        popup.show()
    }

    private fun refreshList() {
        var list = allProducts
        if (showFavoritesOnly) list = list.filter { it.isFavorite }

        list = when (sortMode) {
            SortMode.ALPHA_AZ -> list.sortedBy { it.name.lowercase(Locale.getDefault()) }
            SortMode.ALPHA_ZA -> list.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
            SortMode.EXPIRY_ASC -> list.sortedBy { it.expirationDate ?: Long.MAX_VALUE }
            SortMode.EXPIRY_DESC -> list.sortedByDescending { it.expirationDate ?: 0L }
            SortMode.QTY_ASC -> list.sortedBy { it.quantity }
            SortMode.QTY_DESC -> list.sortedByDescending { it.quantity }
            SortMode.FAVORITES_FIRST -> list.sortedByDescending { it.isFavorite }
            SortMode.ADDED_ASC -> list.sortedBy { it.dateAdded }
            SortMode.ADDED_DESC -> list.sortedByDescending { it.dateAdded }
        }

        val isSearching = !binding.searchView.query.isNullOrEmpty()
        updateList(list, fromSearch = isSearching)
    }

    private fun updateList(products: List<Product>, fromSearch: Boolean = false) {
        if (allProducts.isEmpty()) {
            binding.recyclerProducts.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.VISIBLE
            binding.emptyStateTitle.text = "Scan Your First Product"
            binding.emptyStateSubtitle.text = "To get started, scan a food item or enter the details manually."
        } else if (products.isEmpty()) {
            binding.recyclerProducts.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.VISIBLE
            binding.emptyStateTitle.text = if (showFavoritesOnly) "No Favorites Yet" else "No Results Found"
            binding.emptyStateSubtitle.text = ""
        } else {
            binding.recyclerProducts.visibility = View.VISIBLE
            binding.emptyStateContainer.visibility = View.GONE
            adapter.updateData(products, sortMode)
        }
    }

    fun deleteProductWithConfirmation(product: Product) {
        androidx.appcompat.app.AlertDialog.Builder(this)
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
        lifecycleScope.launch {
            productViewModel.markAsUsed(product)
            NotificationScheduler.cancelForProduct(this@MainActivity, product)
            Toast.makeText(this@MainActivity, "${product.name} moved to history (Used)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSearch() {
        binding.searchView.visibility = View.VISIBLE
        binding.searchView.isIconified = false
        binding.searchView.requestFocus()
    }

    private fun closeSearchCompletely() {
        binding.searchView.setQuery("", false)
        binding.searchView.clearFocus()
        binding.searchView.visibility = View.GONE
        refreshList()
    }

    private fun highlightBottomNav(tab: BottomTab) {
        binding.navHome.setImageResource(if (tab == BottomTab.HOME) R.drawable.ic_home_filled else R.drawable.ic_home_unfilled)
        binding.navHistory.setImageResource(if (tab == BottomTab.HISTORY) R.drawable.ic_clock_filled else R.drawable.ic_clock_unfilled) // Corrected based on your provided XML resource names
        binding.navSettings.setImageResource(if (tab == BottomTab.SETTINGS) R.drawable.ic_settings_filled else R.drawable.ic_settings_unfilled) // Corrected
        binding.navCart.setImageResource(R.drawable.ic_cart)
    }

    private fun scheduleAllProductNotifications() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            lifecycleScope.launch {
                // Assuming your ProductViewModel has a way to get all products,
                // for example, through a LiveData or a suspend function.
                // Adjust this line based on how your ViewModel exposes the product list.
                val products = productViewModel.allProducts.value // Or some other way to get the list
                products?.let {
                    // Now, you need to decide what to do with these products.
                    // For example, if you have a NotificationScheduler class:
                    // NotificationScheduler.scheduleNotificationsForProducts(this@MainActivity, it)
                    // Or if scheduleNotificationsForProducts is a global/extension function:
                    // scheduleNotificationsForProducts(this@MainActivity, it)

                    // For now, let's assume you want to iterate and schedule for each
                    it.forEach { product ->
                        NotificationScheduler.scheduleForProduct(this@MainActivity, product)
                    }
                }
            }
        }
    }


    private fun checkAndMaybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                scheduleAllProductNotifications() // Already granted
            }
        } else {
            scheduleAllProductNotifications() // No runtime permission needed for older Android versions
        }
    }

    // Enum for Bottom Navigation Tabs (assuming you have this or similar)
    enum class BottomTab { HOME, CART, HISTORY, SETTINGS }
}
