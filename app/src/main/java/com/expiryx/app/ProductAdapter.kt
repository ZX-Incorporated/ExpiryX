package com.expiryx.app

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

sealed class ProductListItem {
    data class Header(val title: String, val colorRes: Int) : ProductListItem()
    data class ProductItem(val product: Product) : ProductListItem()
}

class ProductAdapter(
    private val onFavoriteClick: (Product) -> Unit,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ProductListItem> = emptyList()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PRODUCT = 1
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
        return when (items[position]) {
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
        when (val item = items[position]) {
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

                h.btnFavorite.setImageResource(
                    if (product.isFavorite) R.drawable.ic_heart_filled
                    else R.drawable.ic_heart_unfilled
                )
                h.btnFavorite.setOnClickListener { onFavoriteClick(product) }

                when {
                    !product.imageUri.isNullOrBlank() && product.imageUri.startsWith("http") -> {
                        Glide.with(h.itemView.context)
                            .load(product.imageUri)
                            .placeholder(R.drawable.ic_placeholder)
                            .into(h.imageProduct)
                    }
                    !product.imageUri.isNullOrBlank() -> {
                        Glide.with(h.itemView.context)
                            .load(Uri.parse(product.imageUri))
                            .placeholder(R.drawable.ic_placeholder)
                            .into(h.imageProduct)
                    }
                    else -> h.imageProduct.setImageResource(R.drawable.ic_placeholder)
                }

                h.itemView.setOnClickListener { onItemClick(product) }
                h.itemView.setOnLongClickListener {
                    (h.itemView.context as? MainActivity)?.deleteProductWithConfirmation(product)
                    true
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(products: List<Product>) {
        val grouped = mutableListOf<ProductListItem>()
        val now = System.currentTimeMillis()

        fun addGroup(title: String, colorRes: Int, condition: (Product) -> Boolean) {
            val filtered = products.filter(condition)
            if (filtered.isNotEmpty()) {
                grouped.add(ProductListItem.Header(title, colorRes))
                grouped.addAll(filtered.map { ProductListItem.ProductItem(it) })
            }
        }

        addGroup("Expired", R.color.red) {
            it.expirationDate != null && it.expirationDate < now
        }
        addGroup("Expiring in 24 hours", R.color.orange) {
            it.expirationDate != null &&
                    it.expirationDate in now..(now + 24 * 60 * 60 * 1000)
        }
        addGroup("Expiring in 3 days", R.color.yellow) {
            it.expirationDate != null &&
                    it.expirationDate in (now + 24 * 60 * 60 * 1000)..(now + 3 * 24 * 60 * 60 * 1000)
        }
        addGroup("Expiring in 14 days", R.color.green) {
            it.expirationDate != null &&
                    it.expirationDate in (now + 3 * 24 * 60 * 60 * 1000)..(now + 14 * 24 * 60 * 60 * 1000)
        }
        addGroup("Expiring in 3 months", R.color.blue) {
            it.expirationDate != null &&
                    it.expirationDate in (now + 14 * 24 * 60 * 60 * 1000)..(now + 90L * 24 * 60 * 60 * 1000)
        }
        addGroup("Expiring in 1 year or more", R.color.purple) {
            it.expirationDate != null &&
                    it.expirationDate >= (now + 365L * 24 * 60 * 60 * 1000)
        }
        addGroup("No expiry set", R.color.gray) { it.expirationDate == null }

        items = grouped
        notifyDataSetChanged()
    }
}
