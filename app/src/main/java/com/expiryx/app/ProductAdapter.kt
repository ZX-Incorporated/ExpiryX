package com.expiryx.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

class ProductAdapter(private val products: List<Product>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    // Categories
    private val categorized = categorizeProducts(products)

    private fun categorizeProducts(products: List<Product>): Map<String, List<Product>> {
        val now = System.currentTimeMillis()
        val map = linkedMapOf<String, MutableList<Product>>(
            "Within 24 Hours" to mutableListOf(),
            "Within 3 Days" to mutableListOf(),
            "Within 14 Days" to mutableListOf(),
            "Within 3 Months" to mutableListOf(),
            "Within 1 Year+" to mutableListOf()
        )

        for (p in products) {
            val diff = TimeUnit.MILLISECONDS.toDays(p.expirationDate - now)

            val days = TimeUnit.MILLISECONDS.toDays(diff)

            // Exclude expired older than 7 days
            if (days < -7) continue

            when {
                days <= 1 -> map["Within 24 Hours"]?.add(p)
                days <= 3 -> map["Within 3 Days"]?.add(p)
                days <= 14 -> map["Within 14 Days"]?.add(p)
                days <= 90 -> map["Within 3 Months"]?.add(p)
                else -> map["Within 1 Year+"]?.add(p)
            }
        }
        return map
    }

    private val items: List<Any> = buildList {
        for ((category, list) in categorized) {
            if (list.isNotEmpty()) {
                add(category) // header
                addAll(list)  // items
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) TYPE_HEADER else TYPE_ITEM
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
        if (holder is HeaderViewHolder) {
            holder.bind(items[position] as String)
        } else if (holder is ProductViewHolder) {
            holder.bind(items[position] as Product)
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title = itemView.findViewById<TextView>(R.id.headerTitle)
        fun bind(category: String) {
            title.text = category
        }
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name = itemView.findViewById<TextView>(R.id.productName)
        private val expiry = itemView.findViewById<TextView>(R.id.productExpiry)

        fun bind(product: Product) {
            name.text = product.name
            expiry.text = "Expires: ${android.text.format.DateFormat.format("dd/MM/yyyy", product.expirationDate)}"
        }
    }
}
