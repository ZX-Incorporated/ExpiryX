package com.expiryx.app

import android.content.Intent // Added
import android.net.Uri // Added
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast // Added for error handling
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor

sealed class ProductListItem {
    data class Header(val title: String, val colorRes: Int) : ProductListItem()
    data class ProductItem(val product: Product) : ProductListItem()
}

class ProductAdapter(
    private val onFavoriteClick: (Product) -> Unit,
    private val onItemClick: (Product) -> Unit,
    private val onDeleteLongPress: (Product) -> Unit,
    private val onBrowseClick: (Product) -> Unit // Added
) : ListAdapter<ProductListItem, RecyclerView.ViewHolder>(ProductListDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PRODUCT = 1
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bar: View = view.findViewById(R.id.headerBar)
        val title: TextView = view.findViewById(R.id.headerTitle)
    }

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageProduct: ImageView = view.findViewById(R.id.imageProduct)
        val textProductName: TextView = view.findViewById(R.id.textProductName)
        val textProductBrand: TextView = view.findViewById(R.id.textProductBrand)
        val textProductExpiry: TextView = view.findViewById(R.id.textProductExpiry) // Added
        val textProductQuantity: TextView = view.findViewById(R.id.textProductQuantity)
        val btnFavorite: ImageButton = view.findViewById(R.id.btnFavorite)
        val btnOpenInBrowser: ImageButton = view.findViewById(R.id.btnOpenInBrowser) // Added
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ProductListItem.Header -> TYPE_HEADER
            is ProductListItem.ProductItem -> TYPE_PRODUCT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
            ProductViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ProductListItem.Header -> {
                val h = holder as HeaderViewHolder
                h.title.text = item.title
                h.bar.setBackgroundColor(h.itemView.context.getColor(item.colorRes))
            }
            is ProductListItem.ProductItem -> {
                val h = holder as ProductViewHolder
                val product = item.product

                h.textProductName.text = product.name

                val brandText = product.brand?.takeIf { it.isNotBlank() }
                if (!brandText.isNullOrBlank()) {
                    h.textProductBrand.text = brandText
                    h.textProductBrand.visibility = View.VISIBLE
                } else {
                    h.textProductBrand.visibility = View.GONE
                }

                if (product.expirationDate != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    h.textProductExpiry.text = "Expires: ${sdf.format(Date(product.expirationDate))}"
                } else {
                    h.textProductExpiry.text = "Expires: N/A"
                }
                h.textProductExpiry.visibility = View.VISIBLE // Always show this field

                h.textProductQuantity.text = "Qty: ${product.quantity}"
                h.btnFavorite.setImageResource(if (product.isFavorite) R.drawable.ic_fav_filled else R.drawable.ic_fav_unfilled)
                h.btnFavorite.setOnClickListener { onFavoriteClick(product) }

                // Set up browser button
                h.btnOpenInBrowser.setOnClickListener { onBrowseClick(product) } // Added

                Glide.with(h.itemView.context)
                    .load(if (product.imageUri.isNullOrBlank()) R.drawable.ic_placeholder else Uri.parse(product.imageUri))
                    .error(R.drawable.ic_placeholder)
                    .into(h.imageProduct)

                h.itemView.setOnClickListener { onItemClick(product) }
                h.itemView.setOnLongClickListener {
                    onDeleteLongPress(product)
                    true
                }
            }
        }
    }

    /**
     * ✅ MAJOR FIX: This function now accepts the sort mode.
     * It will group by date if the sort mode is EXPIRY_ASC or EXPIRY_DESC.
     * Otherwise, it will show a simple, correctly sorted list.
     */
    fun updateData(products: List<Product>, sortMode: MainActivity.SortMode) {
        val listItems = if (sortMode == MainActivity.SortMode.EXPIRY_ASC || sortMode == MainActivity.SortMode.EXPIRY_DESC) {
            createGroupedList(products, sortMode == MainActivity.SortMode.EXPIRY_DESC)
        } else {
            createFlatList(products)
        }
        submitList(listItems)
    }

    private fun createFlatList(products: List<Product>): List<ProductListItem> {
        return products.map { ProductListItem.ProductItem(it) }
    }

    private fun createGroupedList(products: List<Product>, reverseOrder: Boolean = false): List<ProductListItem> {
        val grouped = mutableListOf<ProductListItem>()
        val now = System.currentTimeMillis()
        val startToday = getStartOfDay(now)

        fun dayDiff(expiryMillis: Long?): Long? {
            expiryMillis ?: return null
            val startExpiry = getStartOfDay(expiryMillis)
            val diffMs = startExpiry - startToday
            return floor(diffMs.toDouble() / DAY_MS).toLong()
        }

        fun addGroup(title: String, colorRes: Int, condition: (Product) -> Boolean) {
            val filtered = products.filter(condition)
            if (filtered.isNotEmpty()) {
                grouped.add(ProductListItem.Header(title, colorRes))
                grouped.addAll(filtered.map { ProductListItem.ProductItem(it) })
            }
        }

        if (reverseOrder) {
            addGroup("No expiry date", R.color.gray) { it.expirationDate == null }
            addGroup("Expiring in 1+ year", R.color.gray) { val d = dayDiff(it.expirationDate); d != null && d > 365L }
            addGroup("Expiring in 3-12 months", R.color.purple) { val d = dayDiff(it.expirationDate); d != null && d in 91L..365L }
            addGroup("Expiring in 15-90 days", R.color.teal_200) { val d = dayDiff(it.expirationDate); d != null && d in 15L..90L }
            addGroup("Expiring in 4-14 days", R.color.blue) { val d = dayDiff(it.expirationDate); d != null && d in 4L..14L }
            addGroup("Expiring in 2-3 days", R.color.green) { val d = dayDiff(it.expirationDate); d != null && d in 2L..3L }
            addGroup("Expiring tomorrow", R.color.yellow) { val d = dayDiff(it.expirationDate); d != null && d == 1L }
            addGroup("Expiring today", R.color.orange) { val d = dayDiff(it.expirationDate); d != null && d == 0L }
            addGroup("Expired", R.color.red) { val d = dayDiff(it.expirationDate); d != null && d < 0 }
        } else {
            addGroup("Expired", R.color.red) { val d = dayDiff(it.expirationDate); d != null && d < 0 }
            addGroup("Expiring today", R.color.orange) { val d = dayDiff(it.expirationDate); d != null && d == 0L }
            addGroup("Expiring tomorrow", R.color.yellow) { val d = dayDiff(it.expirationDate); d != null && d == 1L }
            addGroup("Expiring in 2-3 days", R.color.green) { val d = dayDiff(it.expirationDate); d != null && d in 2L..3L }
            addGroup("Expiring in 4-14 days", R.color.blue) { val d = dayDiff(it.expirationDate); d != null && d in 4L..14L }
            addGroup("Expiring in 15-90 days", R.color.teal_200) { val d = dayDiff(it.expirationDate); d != null && d in 15L..90L }
            addGroup("Expiring in 3-12 months", R.color.purple) { val d = dayDiff(it.expirationDate); d != null && d in 91L..365L }
            addGroup("Expiring in 1+ year", R.color.gray) { val d = dayDiff(it.expirationDate); d != null && d > 365L }
            addGroup("No expiry date", R.color.gray) { it.expirationDate == null }
        }

        return grouped
    }

    private fun getStartOfDay(ts: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

private class ProductListDiffCallback : DiffUtil.ItemCallback<ProductListItem>() {
    override fun areItemsTheSame(oldItem: ProductListItem, newItem: ProductListItem): Boolean {
        return when {
            oldItem is ProductListItem.Header && newItem is ProductListItem.Header -> oldItem.title == newItem.title
            oldItem is ProductListItem.ProductItem && newItem is ProductListItem.ProductItem -> oldItem.product.id == newItem.product.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ProductListItem, newItem: ProductListItem): Boolean {
        return oldItem == newItem
    }
}
