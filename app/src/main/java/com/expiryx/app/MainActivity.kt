package com.expiryx.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory((application as ProductApplication).repository)
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var emptyImage: ImageView
    private lateinit var emptyTitle: TextView
    private lateinit var emptySubtitle: TextView
    private lateinit var addButton: FloatingActionButton
    private lateinit var adapter: ProductAdapter
    private lateinit var searchView: SearchView
    private lateinit var btnSearch: ImageView
    private lateinit var btnSortBy: ImageView
    private lateinit var btnFavorite: ImageView

    // bottom nav icons
    private lateinit var navHome: ImageView
    private lateinit var navCart: ImageView
    private lateinit var navHistory: ImageView
    private lateinit var navSettings: ImageView

    private var allProducts: List<Product> = emptyList()
    private var showFavoritesOnly = false
    private var sortMode = SortMode.EXPIRY_ASC

    private val requestNotifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                scheduleAllProductNotifications()
            } else {
                Toast.makeText(this, "Notifications disabled by user", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NotificationUtils.createChannel(this)

        // ✅ Archive expired products into history at launch
        productViewModel.archiveExpiredProducts()

        recyclerView = findViewById(R.id.recyclerProducts)
        emptyState = findViewById(R.id.emptyStateContainer)
        emptyImage = findViewById(R.id.emptyStateImage)
        emptyTitle = findViewById(R.id.emptyStateTitle)
        emptySubtitle = findViewById(R.id.emptyStateSubtitle)
        addButton = findViewById(R.id.btnAddProduct)
        searchView = findViewById(R.id.searchView)
        btnSearch = findViewById(R.id.btnSearch)
        btnSortBy = findViewById(R.id.btnSortBy)
        btnFavorite = findViewById(R.id.btnFavorite)

        // bottom nav
        navHome = findViewById(R.id.navHome)
        navCart = findViewById(R.id.navCart)
        navHistory = findViewById(R.id.navHistory)
        navSettings = findViewById(R.id.navSettings)

        adapter = ProductAdapter(
            onFavoriteClick = { product ->
                val updated = product.copy(isFavorite = !product.isFavorite)
                productViewModel.update(updated)
            },
            onItemClick = { product ->
                val bottomSheet = ProductDetailBottomSheet.newInstance(product)
                bottomSheet.show(supportFragmentManager, "ProductDetailBottomSheet")
            },
            onDeleteLongPress = { product ->
                deleteProductWithConfirmation(product)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Observe products from DB
        productViewModel.allProducts.observe(this) { products ->
            allProducts = products
            refreshList()
            checkAndMaybeRequestNotificationPermission()
            scheduleAllProductNotifications()
        }

        // 🔍 Search button toggle
        btnSearch.setOnClickListener {
            if (searchView.visibility == View.VISIBLE) {
                closeSearchCompletely()
            } else {
                openSearch()
            }
        }

        // 🔍 Search input listeners
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val filtered = if (!newText.isNullOrBlank()) {
                    allProducts.filter { it.name.contains(newText, ignoreCase = true) }
                } else allProducts
                updateList(filtered, fromSearch = !newText.isNullOrBlank())
                return true
            }
        })

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus && searchView.query.isNullOrEmpty()) {
                closeSearchCompletely()
            }
        }

        // Clear search button
        val closeBtnId = androidx.appcompat.R.id.search_close_btn
        val closeBtn = searchView.findViewById<ImageView>(closeBtnId)
        closeBtn?.setOnClickListener {
            if (!searchView.query.isNullOrEmpty()) {
                searchView.setQuery("", false)
            } else {
                closeSearchCompletely()
            }
        }

        // ➕ Add product
        addButton.setOnClickListener {
            val bottomSheet = AddProductBottomSheet()
            bottomSheet.show(supportFragmentManager, "AddProductBottomSheet")
        }

        // 🔀 Sort menu
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

        // ❤️ Favorites toggle
        btnFavorite.setOnClickListener {
            showFavoritesOnly = !showFavoritesOnly
            btnFavorite.setImageResource(
                if (showFavoritesOnly) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_unfilled
            )
            refreshList()
        }

        // 🧭 Bottom nav
        navHome.setOnClickListener {
            highlightBottomNav(BottomTab.HOME)
        }
        navCart.setOnClickListener {
            Toast.makeText(this, "Store coming soon…", Toast.LENGTH_SHORT).show()
            highlightBottomNav(BottomTab.HOME)
        }
        navHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(0, 0)
            finish()
        }
        navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(0, 0)
            finish()
        }

        highlightBottomNav(BottomTab.HOME)
    }

    /** Notification permission (Android 13+) */
    private fun checkAndMaybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /** Schedule alarms for all products */
    private fun scheduleAllProductNotifications() {
        for (p in allProducts) {
            NotificationScheduler.scheduleForProduct(this, p)
        }
    }

    // --- Search helpers ---
    private fun openSearch() {
        searchView.visibility = View.VISIBLE
        searchView.isIconified = false
        searchView.requestFocus()
    }

    private fun closeSearchCompletely() {
        searchView.setQuery("", false)
        searchView.clearFocus()
        searchView.visibility = View.GONE
        refreshList()
    }

    protected fun highlightBottomNav(tab: BottomTab) {
        when (tab) {
            BottomTab.HOME -> {
                navHome.setImageResource(R.drawable.ic_home_filled)
                navCart.setImageResource(R.drawable.ic_cart)
                navHistory.setImageResource(R.drawable.ic_clock_unfilled)
                navSettings.setImageResource(R.drawable.ic_settings_unfilled)
            }
            BottomTab.CART -> {
                navHome.setImageResource(R.drawable.ic_home_unfilled)
                navCart.setImageResource(R.drawable.ic_cart)
                navHistory.setImageResource(R.drawable.ic_clock_unfilled)
                navSettings.setImageResource(R.drawable.ic_settings_unfilled)
            }
            BottomTab.HISTORY -> {
                navHome.setImageResource(R.drawable.ic_home_unfilled)
                navCart.setImageResource(R.drawable.ic_cart)
                navHistory.setImageResource(R.drawable.ic_clock_filled)
                navSettings.setImageResource(R.drawable.ic_settings_unfilled)
            }
            BottomTab.SETTINGS -> {
                navHome.setImageResource(R.drawable.ic_home_unfilled)
                navCart.setImageResource(R.drawable.ic_cart)
                navHistory.setImageResource(R.drawable.ic_clock_unfilled)
                navSettings.setImageResource(R.drawable.ic_settings_filled)
            }
        }
    }

    private fun refreshList() {
        var list = allProducts
        if (showFavoritesOnly) list = list.filter { it.isFavorite }

        list = when (sortMode) {
            SortMode.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            SortMode.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
            SortMode.EXPIRY_ASC -> list.sortedBy { it.expirationDate ?: Long.MAX_VALUE }
            SortMode.EXPIRY_DESC -> list.sortedByDescending { it.expirationDate ?: 0L }
            SortMode.QTY_ASC -> list.sortedBy { it.quantity }
            SortMode.QTY_DESC -> list.sortedByDescending { it.quantity }
            SortMode.FAVORITES_FIRST -> list.sortedByDescending { it.isFavorite }
        }

        val isSearching = !searchView.query.isNullOrEmpty()
        updateList(list, fromSearch = isSearching)
    }

    private fun updateList(products: List<Product>, fromSearch: Boolean = false) {
        if (products.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE

            when {
                allProducts.isEmpty() -> {
                    emptyImage.setImageResource(R.drawable.ic_carton_scan)
                    emptyTitle.text = "Scan Your First Product"
                    emptySubtitle.text = "To get started, scan a food item or enter the details manually."
                }
                showFavoritesOnly -> {
                    emptyImage.setImageResource(R.drawable.ic_favorites_empty)
                    emptyTitle.text = "No favorites yet"
                    emptySubtitle.text = "Mark products as favorite to quickly find them here."
                }
                fromSearch -> {
                    emptyImage.setImageResource(R.drawable.ic_search_empty)
                    emptyTitle.text = "No results found"
                    emptySubtitle.text = "Try adjusting your search or scanning a new product."
                }
                else -> {
                    emptyImage.setImageResource(R.drawable.ic_carton_scan)
                    emptyTitle.text = "Nothing here"
                    emptySubtitle.text = "Start adding products to see them here."
                }
            }
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
            .setPositiveButton("Delete") { _, _ ->
                productViewModel.delete(product)
                NotificationScheduler.cancelForProduct(this, product)
            }
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
        Toast.makeText(this, "${product.name} marked as used", Toast.LENGTH_SHORT).show()
        NotificationScheduler.cancelForProduct(this, product)
    }

    enum class SortMode {
        NAME_ASC, NAME_DESC,
        EXPIRY_ASC, EXPIRY_DESC,
        QTY_ASC, QTY_DESC,
        FAVORITES_FIRST
    }

    enum class BottomTab { HOME, CART, HISTORY, SETTINGS }
}
