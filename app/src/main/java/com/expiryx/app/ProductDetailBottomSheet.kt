package com.expiryx.app

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.*

class ProductDetailBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_PRODUCT = "product"

        fun newInstance(product: Product): ProductDetailBottomSheet {
            return ProductDetailBottomSheet().apply {
                arguments = Bundle().apply { putParcelable(ARG_PRODUCT, product) }
            }
        }
    }

    private var product: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        product = arguments?.getParcelable(ARG_PRODUCT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_product_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val img: ImageView = view.findViewById(R.id.imgProductDetail)
        val name: TextView = view.findViewById(R.id.txtDetailName)
        val notes: TextView = view.findViewById(R.id.txtDetailNotes)
        val expiry: TextView = view.findViewById(R.id.txtDetailExpiry)
        val qty: TextView = view.findViewById(R.id.txtDetailQuantity)
        val weight: TextView = view.findViewById(R.id.txtDetailWeight)
        val reminder: TextView = view.findViewById(R.id.txtDetailReminder)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)

        val p = product ?: return

        // --- Image ---
        when {
            !p.imageUri.isNullOrBlank() && p.imageUri!!.startsWith("http", ignoreCase = true) -> {
                Glide.with(requireContext())
                    .load(p.imageUri)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(img)
            }
            !p.imageUri.isNullOrBlank() -> {
                Glide.with(requireContext())
                    .load(Uri.parse(p.imageUri))
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(img)
            }
            else -> img.setImageResource(R.drawable.ic_carton_scan)
        }

        // --- Core fields ---
        name.text = p.name
        // --- Memo / Notes ---
        if (!p.notes.isNullOrBlank()) {
            notes.text = p.notes
            notes.visibility = View.VISIBLE
        } else {
            notes.visibility = View.GONE
        }


        expiry.text = "Expires on: " + (p.expirationDate?.let { formatDate(it) } ?: "N/A")
        qty.text = "Quantity: ${p.quantity}"

        // --- Optional: Weight & Reminder ---
        // If your Product already has these, assign them here.
        // Otherwise we hide the rows so UI looks clean.
        var weightShown = false
        var reminderShown = false

        // Try to read common field names via Java reflection (no kotlin-reflect dependency).
        // If those fields don't exist in your Product, these will just fail silently and we'll hide the views.
        runCatching {
            val f = p.javaClass.getDeclaredField("weight")
            f.isAccessible = true
            val value = (f.get(p) as? String)?.takeIf { it.isNotBlank() }
            if (value != null) {
                weight.text = "Weight: $value"
                weightShown = true
            }
        }
        runCatching {
            val f = p.javaClass.getDeclaredField("reminderDays")
            f.isAccessible = true
            val value = (f.get(p) as? Int)
            if (value != null) {
                reminder.text = "Reminder: $value day${if (value == 1) "" else "s"} before"
                reminderShown = true
            }
        }

        // hide if not present
        weight.isVisible = weightShown
        reminder.isVisible = reminderShown

        val btnMarkAsUsed: Button = view.findViewById(R.id.btnMarkAsUsed)

        btnMarkAsUsed.setOnClickListener {
            product?.let { p ->
                // Later: move product to history
                (activity as? MainActivity)?.markProductAsUsed(p)
            }
            dismiss()
        }


        // --- Actions ---
        btnDelete.setOnClickListener {
            (activity as? MainActivity)?.deleteProductWithConfirmation(p)
            dismiss()
        }

        btnEdit.setOnClickListener {
            (activity as? MainActivity)?.editProduct(p)
            dismiss()
        }
    }

    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
