package com.expiryx.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.expiryx.app.databinding.ActivityHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter

    // ✅ FIX: Removed 'private' to allow access from BottomSheet
    val viewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory((application as ProductApplication).repository)
    }

    private var fullList: List<History> = emptyList()
    private var sortIndex: Int = 0
    private var searchQuery: String = ""

    private var showExpired = true
    private var showConsumed = true
    private var showDeleted = true
    private var onlyFavourites = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Highlight nav icons
        binding.navHome.setImageResource(R.drawable.ic_home_unfilled)
        binding.navCart.setImageResource(R.drawable.ic_cart)
        binding.navHistory.setImageResource(R.drawable.ic_clock_filled)
        binding.navSettings.setImageResource(R.drawable.ic_settings_unfilled)

        // Setup RecyclerView
        adapter = HistoryAdapter(
            onItemClick = { h -> HistoryDetailBottomSheet.newInstance(h).show(supportFragmentManager, "HistoryDetail") },
            onItemLongPress = { h ->
                AlertDialog.Builder(this)
                    .setTitle("Permanently Delete")
                    .setMessage("Are you sure you want to permanently delete ${h.productName}? This cannot be undone.")
                    .setPositiveButton("Delete") { _, _ -> viewModel.permanentlyDelete(h) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = adapter

        setupSearch()
        setupSort()
        setupFilter()
        setupBottomNav()

        // Observe data
        viewModel.allHistory.observe(this) { list ->
            fullList = list ?: emptyList()
            applyFilters()
        }
    }

    // ---------- SEARCH ----------
    private fun setupSearch() {
        binding.btnSearch.setOnClickListener { openSearch() }

        binding.searchViewHistory.setOnCloseListener {
            closeSearch()
            true
        }

        binding.searchViewHistory.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus && searchQuery.isEmpty()) closeSearch()
        }

        binding.searchViewHistory.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery = query.orEmpty()
                applyFilters()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText.orEmpty()
                applyFilters()
                return true
            }
        })
    }

    private fun openSearch() {
        binding.searchViewHistory.visibility = View.VISIBLE
        binding.searchViewHistory.isIconified = false
        binding.searchViewHistory.requestFocus()

        binding.countersLayout.visibility = View.GONE
        binding.layoutSortHistory.root.visibility = View.GONE

        showKeyboard()
    }

    private fun closeSearch() {
        binding.searchViewHistory.setQuery("", false)
        binding.searchViewHistory.clearFocus()
        binding.searchViewHistory.visibility = View.GONE

        binding.countersLayout.visibility = View.VISIBLE
        binding.layoutSortHistory.root.visibility = View.VISIBLE

        searchQuery = ""
        applyFilters()
        hideKeyboard()
    }

    private fun showKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.searchViewHistory.findFocus(), InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    // ---------- SORT ----------
    private fun setupSort() {
        val sortBinding = binding.layoutSortHistory
        val sortLayout = sortBinding.root
        val sortText: TextView = sortBinding.textSortHistory

        sortLayout.setOnClickListener {
            val popup = PopupMenu(this, sortLayout)
            popup.menuInflater.inflate(R.menu.menu_sort_history, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                sortIndex = when (item.itemId) {
                    R.id.sort_date_desc -> 0
                    R.id.sort_date_asc -> 1
                    R.id.sort_expired_first -> 2
                    R.id.sort_name_asc -> 3
                    R.id.sort_name_desc -> 4
                    R.id.sort_quantity_asc -> 5
                    R.id.sort_quantity_desc -> 6
                    R.id.sort_expiry_soon -> 7
                    R.id.sort_expiry_late -> 8
                    R.id.sort_favourites -> 9
                    else -> 0
                }
                sortText.text = item.title
                applyFilters()
                true
            }
            popup.show()
        }
    }

    // ---------- NAV ----------
    private fun setupBottomNav() {
        binding.navHomeWrapper.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(0, 0)
            finish()
        }
        binding.navCartWrapper.setOnClickListener {
            Toast.makeText(this, "Store coming soon…", Toast.LENGTH_SHORT).show()
        }
        binding.navHistoryWrapper.setOnClickListener {
            // Already here
        }
        binding.navSettingsWrapper.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    // ---------- FILTERS & SORT ----------
    private fun setupFilter() {
        binding.btnFilter.setOnClickListener { v ->
            val popup = PopupMenu(this, v)
            popup.menuInflater.inflate(R.menu.menu_filter_history, popup.menu)

            popup.menu.findItem(R.id.filter_expired).isChecked = showExpired
            popup.menu.findItem(R.id.filter_consumed).isChecked = showConsumed
            popup.menu.findItem(R.id.filter_deleted).isChecked = showDeleted
            popup.menu.findItem(R.id.filter_favourites).isChecked = onlyFavourites

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.filter_expired -> {
                        if (showExpired && !(showConsumed || showDeleted)) {
                            Toast.makeText(this, "At least one type must be selected", Toast.LENGTH_SHORT).show()
                        } else {
                            showExpired = !showExpired
                            item.isChecked = showExpired
                            applyFilters()
                        }
                    }
                    R.id.filter_consumed -> {
                        if (showConsumed && !(showExpired || showDeleted)) {
                            Toast.makeText(this, "At least one type must be selected", Toast.LENGTH_SHORT).show()
                        } else {
                            showConsumed = !showConsumed
                            item.isChecked = showConsumed
                            applyFilters()
                        }
                    }
                    R.id.filter_deleted -> {
                        if (showDeleted && !(showExpired || showConsumed)) {
                            Toast.makeText(this, "At least one type must be selected", Toast.LENGTH_SHORT).show()
                        } else {
                            showDeleted = !showDeleted
                            item.isChecked = showDeleted
                            applyFilters()
                        }
                    }
                    R.id.filter_favourites -> {
                        onlyFavourites = !onlyFavourites
                        item.isChecked = onlyFavourites
                        applyFilters()
                    }
                }
                // Return true to close the menu, false to keep it open.
                // The default behavior is to close, which is fine here.
                true
            }
            popup.show()
        }
    }

    private fun applyFilters() {
        var filtered = fullList

        // Type filter
        filtered = filtered.filter { h ->
            (h.action == "Expired" && showExpired) ||
                    (h.action == "Used" && showConsumed) ||
                    (h.action == "Deleted" && showDeleted)
        }

        // Favourites filter
        if (onlyFavourites) {
            filtered = filtered.filter { it.isFavorite }
        }

        // Search filter
        if (searchQuery.isNotBlank()) {
            val qLower = searchQuery.lowercase(Locale.getDefault())
            filtered = filtered.filter { h ->
                val nameMatch = h.productName.contains(qLower, ignoreCase = true)
                val actionMatch = h.action.contains(qLower, ignoreCase = true)
                val brandMatch = h.brand?.contains(qLower, ignoreCase = true) ?: false
                val weightMatch = h.weight?.contains(qLower, ignoreCase = true) ?: false
                val quantityMatch = h.quantity.toString().contains(qLower)
                val expiryMatch = h.expirationDate?.let { formatDate(it).contains(qLower) } ?: false
                val timestampMatch = formatDateTime(h.timestamp).contains(qLower)

                nameMatch || actionMatch || brandMatch || weightMatch || quantityMatch || expiryMatch || timestampMatch
            }
        }

        // Sorting
        filtered = when (sortIndex) {
            0 -> filtered.sortedByDescending { it.timestamp } // Newest first
            1 -> filtered.sortedBy { it.timestamp } // Oldest first
            2 -> filtered.sortedWith(compareByDescending<History> { it.action == "Expired" }.thenByDescending { it.timestamp }) // Expired first
            3 -> filtered.sortedBy { it.productName.lowercase(Locale.getDefault()) } // Name A-Z
            4 -> filtered.sortedByDescending { it.productName.lowercase(Locale.getDefault()) } // Name Z-A
            5 -> filtered.sortedBy { it.quantity } // Quantity low-high
            6 -> filtered.sortedByDescending { it.quantity } // Quantity high-low
            7 -> filtered.sortedBy { it.expirationDate ?: Long.MAX_VALUE } // Expiry soonest
            8 -> filtered.sortedByDescending { it.expirationDate ?: Long.MIN_VALUE } // Expiry latest
            9 -> filtered.sortedWith(compareByDescending<History> { it.isFavorite }.thenByDescending { it.timestamp }) // Favourites first
            else -> filtered.sortedByDescending { it.timestamp } // Default case
        }

        updateUI(filtered)
    }

    private fun updateUI(list: List<History>) {
        adapter.updateData(list)

        val isSearching = searchQuery.isNotEmpty()
        val hasResults = list.isNotEmpty()

        binding.countersLayout.visibility = if (isSearching) View.GONE else View.VISIBLE
        binding.layoutSortHistory.root.visibility = if (isSearching) View.GONE else View.VISIBLE

        if (hasResults) {
            binding.recyclerHistory.visibility = View.VISIBLE
            binding.historyPlaceholder.visibility = View.GONE
            binding.historyNoResults.visibility = View.GONE
            binding.historyNoFavourites.visibility = View.GONE
        } else {
            binding.recyclerHistory.visibility = View.GONE

            when {
                searchQuery.isNotEmpty() -> {
                    binding.historyNoResults.visibility = View.VISIBLE
                    binding.historyPlaceholder.visibility = View.GONE
                    binding.historyNoFavourites.visibility = View.GONE
                }
                onlyFavourites -> {
                    binding.historyNoFavourites.visibility = View.VISIBLE
                    binding.historyPlaceholder.visibility = View.GONE
                    binding.historyNoResults.visibility = View.GONE
                }
                else -> {
                    binding.historyPlaceholder.visibility = View.VISIBLE
                    binding.historyNoResults.visibility = View.GONE
                    binding.historyNoFavourites.visibility = View.GONE
                }
            }
        }

        if (!isSearching) {
            val expiredCount = fullList.count { it.action == "Expired" }
            val usedCount = fullList.count { it.action == "Used" }
            val deletedCount = fullList.count { it.action == "Deleted" }

            binding.textExpiredCount.text = "$expiredCount Expired"
            binding.textConsumedCount.text = "$usedCount Consumed"
            binding.textDeletedCount.text = "$deletedCount Deleted"
        }
    }

    // ---------- Helpers ----------
    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}