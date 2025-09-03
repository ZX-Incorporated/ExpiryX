package com.expiryx.app

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.concurrent.TimeUnit
import kotlin.math.floor

sealed class ProductListItem {
    data class Header(val title: String, val colorRes: Int) : ProductListItem()
    data class ProductItem(val product: Product) : ProductListItem()
}

class ProductAdapter(
    private val onFavoriteClick: (Product) -> Unit,
    private val onItemClick: (Product) -> Unit,
    private val onDeleteLongPress: (Product) -> Unit
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
        val textProductNotes: TextView = view.findViewById(R.id.textProductNotes)
        val textProductQuantity: TextView = view.findViewById(R.id.textProductQuantity)
        val btnFavorite: ImageButton = view.findViewById(R.id.btnFavorite)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ProductListItem.Header -> TYPE_HEADER
            is ProductListItem.ProductItem -> TYPE_PRODUCT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_product, parent, false)
            ProductViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ProductListItem.Header -> {
                val h = holder as HeaderViewHolder
                h.title.text = item.title
                h.bar.setBackgroundColor(
                    ContextCompat.getColor(h.itemView.context, item.colorRes)
                )
            }
            is ProductListItem.ProductItem -> {
                val h = holder as ProductViewHolder
                val product = item.product

                h.textProductName.text = product.name
                h.textProductNotes.text = product.notes ?: "No notes"
                h.textProductQuantity.text = "Qty: ${product.quantity}"

                h.btnFavorite.scaleType = ImageView.ScaleType.CENTER_INSIDE
                ImageViewCompat.setImageTintList(h.btnFavorite, null)
                h.btnFavorite.imageTintList = null
                h.btnFavorite.setColorFilter(null)

                h.btnFavorite.setImageResource(
                    if (product.isFavorite) R.drawable.ic_fav_filled
                    else R.drawable.ic_fav_unfilled
                )
                h.btnFavorite.setOnClickListener { onFavoriteClick(product) }

                when {
                    !product.imageUri.isNullOrBlank() && product.imageUri.startsWith("http") -> {
                        Glide.with(h.itemView.context)
                            .load(product.imageUri)
                            .placeholder(R.drawable.ic_placeholder)
                            .error(R.drawable.ic_placeholder)
                            .into(h.imageProduct)
                    }
                    !product.imageUri.isNullOrBlank() -> {
                        Glide.with(h.itemView.context)
                            .load(Uri.parse(product.imageUri))
                            .placeholder(R.drawable.ic_placeholder)
                            .error(R.drawable.ic_placeholder)
                            .into(h.imageProduct)
                    }
                    else -> h.imageProduct.setImageResource(R.drawable.ic_placeholder)
                }

                h.itemView.setOnClickListener { onItemClick(product) }
                h.itemView.setOnLongClickListener {
                    onDeleteLongPress(product)
                    true
                }
            }
        }
    }

    /** Build grouped list based on day difference from today */
    fun updateData(products: List<Product>) {
        val grouped = mutableListOf<ProductListItem>()
        val now = System.currentTimeMillis()
        val startToday = startOfDay(now)

        fun dayDiff(expiryMillis: Long?): Long? {
            expiryMillis ?: return null
            val startExpiry = startOfDay(expiryMillis)
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

        addGroup("Expired", R.color.red) {
            val d = dayDiff(it.expirationDate); d != null && d < 0
        }
        addGroup("Expiring today", R.color.orange) {
            val d = dayDiff(it.expirationDate); d != null && d == 0L
        }
        addGroup("Expiring in 1 day", R.color.yellow) {
            val d = dayDiff(it.expirationDate); d != null && d == 1L
        }
        addGroup("Expiring in 3 days", R.color.green) {
            val d = dayDiff(it.expirationDate); d != null && d in 2L..3L
        }
        addGroup("Expiring in 14 days", R.color.blue) {
            val d = dayDiff(it.expirationDate); d != null && d in 4L..14L
        }
        addGroup("Expiring in 3 months", R.color.teal_200) {
            val d = dayDiff(it.expirationDate); d != null && d in 15L..90L
        }
        addGroup("Expiring in 3–12 months", R.color.purple) {
            val d = dayDiff(it.expirationDate); d != null && d in 91L..365L
        }
        addGroup("Expiring in 1 year or more", R.color.gray) {
            val d = dayDiff(it.expirationDate); d != null && d > 365L
        }
        addGroup("No expiry set", R.color.gray) { it.expirationDate == null }

        submitList(grouped)
    }

    private fun startOfDay(ts: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = ts
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}

private class ProductListDiffCallback : DiffUtil.ItemCallback<ProductListItem>() {
    override fun areItemsTheSame(oldItem: ProductListItem, newItem: ProductListItem): Boolean {
        return when {
            oldItem is ProductListItem.Header && newItem is ProductListItem.Header ->
                oldItem.title == newItem.title && oldItem.colorRes == newItem.colorRes
            oldItem is ProductListItem.ProductItem && newItem is ProductListItem.ProductItem ->
                oldItem.product.id == newItem.product.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ProductListItem, newItem: ProductListItem): Boolean {
        return when {
            oldItem is ProductListItem.Header && newItem is ProductListItem.Header ->
                oldItem == newItem
            oldItem is ProductListItem.ProductItem && newItem is ProductListItem.ProductItem ->
                oldItem.product == newItem.product
            else -> false
        }
    }
}
