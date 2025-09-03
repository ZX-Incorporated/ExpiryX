package com.expiryx.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.expiryx.app.databinding.ActivityHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter

    private val viewModel: ProductViewModel by viewModels {
        ProductViewModelFactory((application as ProductApplication).repository)
    }

    private var fullList: List<History> = emptyList()
    private var sortIndex: Int = 0
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // highlight nav icons
        binding.navHome.setImageResource(R.drawable.ic_home_unfilled)
        binding.navCart.setImageResource(R.drawable.ic_cart)
        binding.navHistory.setImageResource(R.drawable.ic_clock_filled)
        binding.navSettings.setImageResource(R.drawable.ic_settings_unfilled)

        // setup RecyclerView
        adapter = HistoryAdapter(emptyList())
        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = adapter

        setupSearch()
        setupSort()
        setupBottomNav()

        // observe data
        viewModel.allHistory.observe(this) { list ->
            fullList = list ?: emptyList()
            applyFilters()
        }
    }

    // ---------- SEARCH ----------
    private fun setupSearch() {
        binding.btnSearch.setOnClickListener { openSearch() }

        // close icon (X) tapped in SearchView
        binding.searchViewHistory.setOnCloseListener {
            closeSearch()
            true
        }

        // if search loses focus and empty, close it
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
        // keep topBar visible — show search below it
        binding.searchViewHistory.visibility = View.VISIBLE
        binding.searchViewHistory.isIconified = false
        binding.searchViewHistory.requestFocus()

        // hide counters + sort while searching
        binding.countersLayout.visibility = View.GONE
        binding.layoutSortHistory.root.visibility = View.GONE

        showKeyboard()
    }

    private fun closeSearch() {
        binding.searchViewHistory.setQuery("", false)
        binding.searchViewHistory.clearFocus()
        binding.searchViewHistory.visibility = View.GONE

        // restore counters + sort row
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
                    R.id.sort_name -> 3
                    R.id.sort_quantity_asc -> 4
                    R.id.sort_quantity_desc -> 5
                    R.id.sort_expiry_soon -> 6
                    R.id.sort_expiry_late -> 7
                    R.id.sort_favourites -> 8
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
            // already here
        }
        binding.navSettingsWrapper.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    // ---------- FILTERS & SORT ----------
    private fun applyFilters() {
        var filtered = fullList

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            val qLower = q.lowercase(Locale.getDefault())
            filtered = filtered.filter { h ->
                val nameMatch = h.productName.contains(q, ignoreCase = true)
                val actionMatch = h.action.contains(q, ignoreCase = true)
                val notesMatch = h.notes?.contains(q, ignoreCase = true) ?: false
                val weightMatch = h.weight?.contains(q, ignoreCase = true) ?: false
                val quantityMatch = h.quantity.toString().contains(q)
                val expiryMatch = h.expirationDate?.let { formatDate(it).lowercase(Locale.getDefault()).contains(qLower) } ?: false
                val timestampMatch = formatDateTime(h.timestamp).lowercase(Locale.getDefault()).contains(qLower)

                nameMatch || actionMatch || notesMatch || weightMatch || quantityMatch || expiryMatch || timestampMatch
            }
        }

        filtered = when (sortIndex) {
            0 -> filtered.sortedByDescending { it.timestamp }
            1 -> filtered.sortedBy { it.timestamp }
            2 -> filtered.sortedByDescending { it.action == "Expired" }
            3 -> filtered.sortedBy { it.productName.lowercase(Locale.getDefault()) }
            4 -> filtered.sortedBy { it.quantity }
            5 -> filtered.sortedByDescending { it.quantity }
            6 -> filtered.sortedBy { it.expirationDate ?: Long.MAX_VALUE }
            7 -> filtered.sortedByDescending { it.expirationDate ?: Long.MIN_VALUE }
            8 -> filtered.sortedByDescending { it.isFavorite }
            else -> filtered
        }

        updateUI(filtered)
    }

    private fun updateUI(list: List<History>) {
        adapter.updateData(list)

        val isSearching = searchQuery.isNotEmpty()
        val hasResults = list.isNotEmpty()

        // Hide counters + sort row while searching
        binding.countersLayout.visibility = if (isSearching) View.GONE else View.VISIBLE
        binding.layoutSortHistory.root.visibility = if (isSearching) View.GONE else View.VISIBLE

        if (hasResults) {
            binding.recyclerHistory.visibility = View.VISIBLE
            binding.historyPlaceholder.visibility = View.GONE
        } else {
            binding.recyclerHistory.visibility = View.GONE
            binding.historyPlaceholder.visibility = View.VISIBLE
        }

        if (!isSearching) {
            val expiredCount = fullList.count { it.action == "Expired" }
            val usedCount = fullList.count { it.action == "Used" }

            binding.textExpiredCount.text = "$expiredCount Expired"
            binding.textConsumedCount.text = "$usedCount Consumed"
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
