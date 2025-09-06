package com.expiryx.app

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.expiryx.app.databinding.ActivityMainBinding
import com.expiryx.app.databinding.DialogProductDetailsBinding
import com.expiryx.app.databinding.DialogQuantityInputBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var productAdapter: ProductAdapter
    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory((application as ProductApplication).repository)
    }

    private var allProductsList: List<Product> = listOf()
    private var currentSortMode: SortMode = SortMode.EXPIRY_ASC
    private var showFavoritesOnly = false // Filter state for favorites

    // Activity result launcher for ManualEntryActivity
    private val manualEntryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // No specific action needed here if LiveData handles updates
                // Optionally, refresh list or show a message
            }
        }

    // Enum for sort modes
    enum class SortMode {
        EXPIRY_ASC, EXPIRY_DESC, ALPHA_AZ, ALPHA_ZA, ADDED_ASC, ADDED_DESC
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Initial load (default sort: EXPIRY_ASC)
        applyFiltersAndSort()
        highlightCurrentTab()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onFavoriteClick = { product ->
                // Toggle favorite status and update in ViewModel
                val updatedProduct = product.copy(isFavorite = !product.isFavorite)
                productViewModel.update(updatedProduct)
                // Optionally: provide immediate visual feedback if LiveData update isn't fast enough
            },
            onItemClick = { product ->
                showProductDetailsDialog(product)
            },
            onDeleteLongPress = { product ->
                showDeleteConfirmationDialog(product)
            }
        )
        binding.recyclerViewProducts.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = productAdapter
        }
    }

    private fun setupObservers() {
        productViewModel.allProducts.observe(this, Observer { products ->
            allProductsList = products ?: listOf()
            applyFiltersAndSort() // Update list when data changes
        })
    }

    private fun setupListeners() {
        binding.fabAddProduct.setOnClickListener { showAddProductOptions() }
        binding.btnSort.setOnClickListener { showSortOptions(it) }
        binding.btnFavorite.setOnClickListener { toggleFavoritesFilter() } // Listener for favorite filter
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFiltersAndSort()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Bottom Nav Listeners
        setupBottomNav()
    }

    private fun applyFiltersAndSort() {
        var filteredList = allProductsList

        // Apply favorite filter
        if (showFavoritesOnly) {
            filteredList = filteredList.filter { it.isFavorite }
        }

        // Apply search filter
        val searchQuery = binding.editTextSearch.text.toString().trim()
        if (searchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        (it.brand?.contains(searchQuery, ignoreCase = true) ?: false)
            }
        }

        // Apply sort
        val sortedList = when (currentSortMode) {
            SortMode.EXPIRY_ASC -> filteredList.sortedWith(compareBy(nullsLast()) { it.expirationDate })
            SortMode.EXPIRY_DESC -> filteredList.sortedWith(compareByDescending(nullsLast()) { it.expirationDate })
            SortMode.ALPHA_AZ -> filteredList.sortedBy { it.name }
            SortMode.ALPHA_ZA -> filteredList.sortedByDescending { it.name }
            SortMode.ADDED_ASC -> filteredList.sortedBy { it.id } // Assuming id reflects addition order
            SortMode.ADDED_DESC -> filteredList.sortedByDescending { it.id }
        }
        productAdapter.updateData(sortedList, currentSortMode)
    }

    private fun showSortOptions(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.menu_sort, popup.menu)

        // Set checkable and checked for the current sort mode
        when (currentSortMode) {
            SortMode.EXPIRY_ASC -> popup.menu.findItem(R.id.sort_expiry_asc).isChecked = true
            SortMode.EXPIRY_DESC -> popup.menu.findItem(R.id.sort_expiry_desc).isChecked = true
            SortMode.ALPHA_AZ -> popup.menu.findItem(R.id.sort_alpha_az).isChecked = true
            SortMode.ALPHA_ZA -> popup.menu.findItem(R.id.sort_alpha_za).isChecked = true
            SortMode.ADDED_ASC -> popup.menu.findItem(R.id.sort_added_asc).isChecked = true
            SortMode.ADDED_DESC -> popup.menu.findItem(R.id.sort_added_desc).isChecked = true
        }

        popup.setOnMenuItemClickListener { item ->
            currentSortMode = when (item.itemId) {
                R.id.sort_expiry_asc -> SortMode.EXPIRY_ASC
                R.id.sort_expiry_desc -> SortMode.EXPIRY_DESC
                R.id.sort_alpha_az -> SortMode.ALPHA_AZ
                R.id.sort_alpha_za -> SortMode.ALPHA_ZA
                R.id.sort_added_asc -> SortMode.ADDED_ASC
                R.id.sort_added_desc -> SortMode.ADDED_DESC
                else -> currentSortMode
            }
            item.isChecked = true
            applyFiltersAndSort()
            true
        }
        popup.show()
    }

    private fun showProductDetailsDialog(product: Product) {
        val dialogBinding = DialogProductDetailsBinding.inflate(LayoutInflater.from(this))
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_background))
            .create()

        dialogBinding.textProductName.text = product.name
        dialogBinding.textProductBrand.text = "Brand: ${product.brand ?: "N/A"}"
        dialogBinding.textProductWeight.text = "Weight: ${product.weight ?: "N/A"}"
        dialogBinding.textProductQuantity.text = "Quantity: ${product.quantity}"

        val dateFormat = SimpleDateFormat("dd MMM yyyy (EEE)", Locale.getDefault())
        dialogBinding.textExpirationDate.text = "Expires: ${
            product.expirationDate?.let { dateFormat.format(Date(it)) } ?: "Not set"
        }"

        dialogBinding.textReminderDays.text = "Reminder: ${product.reminderDays} days before"
        dialogBinding.chipFavorite.isChecked = product.isFavorite
        dialogBinding.chipFavorite.chipIcon = ContextCompat.getDrawable(
            this,
            if (product.isFavorite) R.drawable.ic_fav_filled else R.drawable.ic_fav_unfilled
        )
        dialogBinding.chipFavorite.text = if (product.isFavorite) "Favorited" else "Add to Favorites"


        if (!product.imageUri.isNullOrBlank()) {
            Glide.with(this).load(Uri.parse(product.imageUri)).placeholder(R.drawable.ic_placeholder).into(dialogBinding.imageProduct)
        } else {
            dialogBinding.imageProduct.setImageResource(R.drawable.ic_placeholder)
        }

        // Determine pill background and text based on expiry
        val today = Calendar.getInstance()
        val expiryCal = Calendar.getInstance().apply { product.expirationDate?.let { timeInMillis = it } }

        when {
            product.expirationDate == null -> {
                dialogBinding.pillStatus.visibility = View.GONE // Hide if no expiry date
            }

            expiryCal.before(today) -> { // Expired
                dialogBinding.pillStatus.text = "Expired"
                dialogBinding.pillStatus.background = ContextCompat.getDrawable(this, R.drawable.pill_expired_bg)
                dialogBinding.pillStatus.setTextColor(Color.WHITE)
            }

            else -> { // Not expired
                val daysUntilExpiry = ((expiryCal.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                dialogBinding.pillStatus.text = when {
                    daysUntilExpiry == 0 -> "Expires Today"
                    daysUntilExpiry == 1 -> "Expires Tomorrow"
                    else -> "Expires in $daysUntilExpiry days"
                }
                // Use a default or less urgent background for non-expired
                dialogBinding.pillStatus.background = ContextCompat.getDrawable(this, R.drawable.pill_default_bg)
                dialogBinding.pillStatus.setTextColor(ContextCompat.getColor(this, R.color.default_pill_text_color)) // Define this color
            }
        }


        dialogBinding.chipFavorite.setOnClickListener {
            val updatedProduct = product.copy(isFavorite = !product.isFavorite)
            productViewModel.update(updatedProduct)
            // Update chip UI immediately
            dialogBinding.chipFavorite.isChecked = updatedProduct.isFavorite
            dialogBinding.chipFavorite.chipIcon = ContextCompat.getDrawable(
                this,
                if (updatedProduct.isFavorite) R.drawable.ic_fav_filled else R.drawable.ic_fav_unfilled
            )
            dialogBinding.chipFavorite.text = if (updatedProduct.isFavorite) "Favorited" else "Add to Favorites"
        }

        dialogBinding.buttonEdit.setOnClickListener {
            editProduct(product)
            dialog.dismiss()
        }

        dialogBinding.buttonMarkAsUsed.setOnClickListener {
            lifecycleScope.launch {
                productViewModel.markAsUsed(product)
                Toast.makeText(this@MainActivity, "${product.name} marked as used.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialogBinding.buttonDelete.setOnClickListener {
            dialog.dismiss() // Dismiss this dialog first
            showDeleteConfirmationDialog(product) // Then show delete confirmation
        }

        dialogBinding.buttonClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(product: Product) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete ${product.name}?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                productViewModel.delete(product)
                Toast.makeText(this, "${product.name} deleted", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showAddProductOptions() {
        // Inflate the bottom sheet layout
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_add_product, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(bottomSheetView)

        // Find views in the bottom sheet
        val optionScanBarcode = bottomSheetView.findViewById<View>(R.id.option_scan_barcode)
        val optionManualEntry = bottomSheetView.findViewById<View>(R.id.option_manual_entry)
        val optionCloudSearch = bottomSheetView.findViewById<View>(R.id.option_cloud_search)
        val optionDeviceUpload = bottomSheetView.findViewById<View>(R.id.option_upload_from_device)


        optionScanBarcode.setOnClickListener {
            // Start BarcodeScannerActivity
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            manualEntryLauncher.launch(intent) // Use launcher for potential result
            dialog.dismiss()
        }

        optionManualEntry.setOnClickListener {
            // Start ManualEntryActivity for a new product
            val intent = Intent(this, ManualEntryActivity::class.java)
            intent.putExtra("isEdit", false) // Explicitly indicate it's not an edit
            manualEntryLauncher.launch(intent)
            dialog.dismiss()
        }

        optionCloudSearch.setOnClickListener {
            Toast.makeText(this, "Cloud Search coming soon!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        optionDeviceUpload.setOnClickListener {
            Toast.makeText(this, "Upload from Device coming soon!", Toast.LENGTH_SHORT).show()
            // Example: Intent to pick an image or file, then process it
            // val intent = Intent(Intent.ACTION_GET_CONTENT)
            // intent.type = "*/*" // Or specific MIME types
            // startActivityForResult(intent, REQUEST_CODE_UPLOAD)
            dialog.dismiss()
        }
        dialog.show()
    }


    private fun editProduct(product: Product) {
        val intent = Intent(this, ManualEntryActivity::class.java).apply {
            putExtra("product", product as Parcelable) // Make sure Product is Parcelable
            putExtra("isEdit", true) // Indicate that this is an edit operation
            // If image URI is already persisted and accessible via product.imageUri,
            // no need to pass it separately unless it's a temporary URI.
        }
        manualEntryLauncher.launch(intent)
    }

    private fun toggleFavoritesFilter() {
        showFavoritesOnly = !showFavoritesOnly
        // Update the icon based on the filter state
        binding.btnFavorite.setImageResource(if (showFavoritesOnly) R.drawable.ic_heart_filled else R.drawable.ic_heart_unfilled)
        applyFiltersAndSort() // Re-apply filters and sort
    }

    private fun setupBottomNav() {
        binding.navHomeWrapper.setOnClickListener { /* Already on Home */ }
        binding.navCartWrapper.setOnClickListener {
            Toast.makeText(this, "Store feature coming soon!", Toast.LENGTH_SHORT).show()
            // Example: startActivity(Intent(this, StoreActivity::class.java))
            // overridePendingTransition(0,0) // No transition
        }
        binding.navHistoryWrapper.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            overridePendingTransition(0, 0)
        }
        binding.navSettingsWrapper.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(0, 0)
        }
    }

    private fun highlightCurrentTab() {
        // Assuming you have methods to set filled/unfilled icons for all tabs
        binding.navHome.setImageResource(R.drawable.ic_home_filled) // Assuming filled version
        binding.navCart.setImageResource(R.drawable.ic_cart) // Default/unfilled
        binding.navHistory.setImageResource(R.drawable.ic_clock_unfilled)
        binding.navSettings.setImageResource(R.drawable.ic_settings_unfilled)
    }
}
