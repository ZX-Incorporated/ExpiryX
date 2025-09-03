// app/src/main/java/com/expiryx/app/HistoryAdapter.kt
package com.expiryx.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private var items: List<History>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtName: TextView = view.findViewById(R.id.textHistoryProduct)
        val txtAction: TextView = view.findViewById(R.id.textHistoryAction)
        val txtDate: TextView = view.findViewById(R.id.textHistoryDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = items[position]
        holder.txtName.text = item.productName
        holder.txtAction.text = item.action
        holder.txtDate.text = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", item.timestamp)
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<History>) {
        items = newItems
        notifyDataSetChanged()
    }
}
