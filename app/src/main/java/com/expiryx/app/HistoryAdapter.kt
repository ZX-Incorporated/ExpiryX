package com.expiryx.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onItemClick: (History) -> Unit,
    private val onItemLongPress: (History) -> Unit
) : ListAdapter<History, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageHistoryProduct: ImageView = view.findViewById(R.id.imageHistoryProduct)
        val textHistoryProduct: TextView = view.findViewById(R.id.textHistoryProduct)
        val textHistoryAction: TextView = view.findViewById(R.id.textHistoryAction)
        val textHistoryDate: TextView = view.findViewById(R.id.textHistoryDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = getItem(position)

        holder.textHistoryProduct.text = item.productName
        holder.textHistoryAction.text = item.action

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.textHistoryDate.text = sdf.format(Date(item.timestamp))

        Glide.with(holder.imageHistoryProduct.context)
            .load(item.imageUri)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .into(holder.imageHistoryProduct)

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener {
            onItemLongPress(item)
            true
        }
    }

    fun updateData(newItems: List<History>) {
        submitList(newItems)
    }
}

private class HistoryDiffCallback : DiffUtil.ItemCallback<History>() {
    override fun areItemsTheSame(oldItem: History, newItem: History): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: History, newItem: History): Boolean {
        return oldItem == newItem
    }
}