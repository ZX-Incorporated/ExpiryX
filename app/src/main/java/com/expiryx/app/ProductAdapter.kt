package com.expiryx.app

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductAdapter(
    private val onItemClick: (Product) -> Unit,
    private val onFavoriteClick: (Product) -> Unit
) : ListAdapter<ProductListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ProductListItem.Header -> TYPE_HEADER
            is ProductListItem.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_product, parent, false)
                ProductViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ProductListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ProductListItem.Item -> (holder as ProductViewHolder).bind(item.product)
        }
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.headerTitle)
        private val bar: View = view.findViewById(R.id.headerBar)

        fun bind(header: ProductListItem.Header) {
            title.text = header.title
            bar.setBackgroundColor(
                ContextCompat.getColor(itemView.context, header.colorRes)
            )
        }
    }

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.textProductName)
        private val notes: TextView = view.findViewById(R.id.textProductNotes)
        private val qty: TextView = view.findViewById(R.id.textProductQuantity)
        private val image: ImageView = view.findViewById(R.id.imageProduct)
        private val favorite: ImageButton = view.findViewById(R.id.btnFavorite)
        private val leftAccent: View = view.findViewById(R.id.leftAccent)
        private val favGlow: View = view.findViewById(R.id.favGlow)

        fun bind(product: Product) {
            name.text = product.name
            notes.text = product.notes ?: ""
            qty.text = "x${product.quantity}"

            // Load image if available
            if (!product.imageUri.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(Uri.parse(product.imageUri))
                    .placeholder(R.drawable.ic_placeholder)
                    .into(image)
            } else {
                image.setImageResource(R.drawable.ic_placeholder)
            }

            // Handle favorite glow
            favGlow.visibility = if (product.isFavorite) View.VISIBLE else View.GONE
            favorite.setImageResource(
                if (product.isFavorite) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_unfilled
            )

            // Accent bar color based on expiry
            val now = System.currentTimeMillis()
            val oneDay = 24 * 60 * 60 * 1000L
            val colorRes = when {
                product.expirationDate < now -> R.color.red
                product.expirationDate <= now + oneDay -> R.color.orange
                product.expirationDate <= now + 3 * oneDay -> R.color.yellow
                product.expirationDate <= now + 14 * oneDay -> R.color.green
                product.expirationDate <= now + 90 * oneDay -> R.color.blue
                else -> R.color.purple
            }
            leftAccent.setBackgroundColor(ContextCompat.getColor(itemView.context, colorRes))

            // Click listeners
            itemView.setOnClickListener { onItemClick(product) }
            favorite.setOnClickListener { onFavoriteClick(product) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ProductListItem>() {
        override fun areItemsTheSame(oldItem: ProductListItem, newItem: ProductListItem): Boolean {
            return if (oldItem is ProductListItem.Item && newItem is ProductListItem.Item) {
                oldItem.product.id == newItem.product.id
            } else if (oldItem is ProductListItem.Header && newItem is ProductListItem.Header) {
                oldItem.title == newItem.title
            } else false
        }

        override fun areContentsTheSame(oldItem: ProductListItem, newItem: ProductListItem): Boolean {
            return oldItem == newItem
        }
    }
}
