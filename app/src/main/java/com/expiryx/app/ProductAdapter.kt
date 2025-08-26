package com.expiryx.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

sealed class ProductListItem {
    data class Header(val title: String, val color: Int) : ProductListItem()
    data class ProductItem(val product: Product) : ProductListItem()
}

class ProductAdapter(
    private val onFavoriteClick: (Product) -> Unit,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ProductListItem> = emptyList()

    fun updateData(products: List<Product>) {
        val groupedItems = mutableListOf<ProductListItem>()

        val grouped = products
            .filterNot { isOlderThan7DaysExpired(it) } // exclude older than 7 days expired
            .groupBy { getSectionForProduct(it) }

        // Ordered headers
        val order = listOf(
            "Expired",
            "Expires in 24 hours",
            "Expires in 3 days",
            "Expires in 7 days",
            "Expires in 14 days",
            "Expires in 3 months",
            "Expires in 1 year or more"
        )

        order.forEach { section ->
            grouped.entries.find { it.key.first == section }?.let { entry ->
                groupedItems.add(ProductListItem.Header(entry.key.first, entry.key.second))
                entry.value.forEach { product ->
                    groupedItems.add(ProductListItem.ProductItem(product))
                }
            }
        }

        items = groupedItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ProductListItem.Header -> 0
            is ProductListItem.ProductItem -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_product, parent, false)
            ProductViewHolder(view)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ProductListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ProductListItem.ProductItem -> (holder as ProductViewHolder).bind(item.product)
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.headerTitle)
        private val headerBar: View = view.findViewById(R.id.headerBar)

        fun bind(header: ProductListItem.Header) {
            title.text = header.title
            headerBar.setBackgroundColor(header.color)
        }
    }

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.textProductName)
        private val notes: TextView = view.findViewById(R.id.textProductNotes)
        private val quantity: TextView = view.findViewById(R.id.textProductQuantity)
        private val favButton: ImageButton = view.findViewById(R.id.btnFavorite)
        private val favGlow: View = view.findViewById(R.id.favGlow)

        fun bind(product: Product) {
            name.text = product.name
            notes.text = product.notes ?: ""
            quantity.text = "x${product.quantity}"

            if (product.isFavorite) {
                favGlow.visibility = View.VISIBLE
                favButton.setImageResource(R.drawable.ic_heart_unfilled)
                favButton.setColorFilter(Color.parseColor("#E91E63"))
            } else {
                favGlow.visibility = View.GONE
                favButton.setImageResource(R.drawable.ic_heart_unfilled)
                favButton.clearColorFilter()
            }

            favButton.setOnClickListener { onFavoriteClick(product) }
            itemView.setOnClickListener { onItemClick(product) }
        }
    }

    private fun isOlderThan7DaysExpired(product: Product): Boolean {
        val today = LocalDate.now()
        val expiryDate = product.expirationDate?.let {
            Date(it).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        } ?: return false
        val daysDiff = ChronoUnit.DAYS.between(today, expiryDate).toInt()
        return daysDiff < -7
    }

    private fun getSectionForProduct(product: Product): Pair<String, Int> {
        val today = LocalDate.now()
        val expiryDate = product.expirationDate?.let {
            Date(it).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        } ?: today
        val daysDiff = ChronoUnit.DAYS.between(today, expiryDate).toInt()

        return when {
            daysDiff < 0 -> "Expired" to Color.parseColor("#D32F2F")
            daysDiff <= 1 -> "Expires in 24 hours" to Color.parseColor("#F57C00")
            daysDiff <= 3 -> "Expires in 3 days" to Color.parseColor("#FFA000")
            daysDiff <= 7 -> "Expires in 7 days" to Color.parseColor("#FBC02D")
            daysDiff <= 14 -> "Expires in 14 days" to Color.parseColor("#388E3C")
            daysDiff <= 90 -> "Expires in 3 months" to Color.parseColor("#1976D2")
            else -> "Expires in 1 year or more" to Color.parseColor("#7B1FA2")
        }
    }
}
