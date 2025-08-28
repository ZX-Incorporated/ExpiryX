package com.expiryx.app

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
        val btnMarkAsUsed: Button = view.findViewById(R.id.btnMarkAsUsed)

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
            else -> img.setImageResource(R.drawable.ic_placeholder)
        }

        // --- Core fields ---
        name.text = p.name

        if (!p.notes.isNullOrBlank()) {
            notes.visibility = View.VISIBLE
            notes.text = p.notes
        } else {
            notes.visibility = View.GONE
        }

        expiry.text = "Expires on: " + (p.expirationDate?.let { formatDate(it) } ?: "N/A")
        qty.text = "Quantity: ${p.quantity}"

        // Optional fields
        if (!p.weight.isNullOrBlank()) {
            weight.visibility = View.VISIBLE
            weight.text = "Weight: ${p.weight}"
        } else {
            weight.visibility = View.GONE
        }

        if (p.reminderDays > 0) {
            reminder.visibility = View.VISIBLE
            reminder.text = "Reminder: ${p.reminderDays} day${if (p.reminderDays == 1) "" else "s"} before"
        } else {
            reminder.visibility = View.GONE
        }

        btnMarkAsUsed.setOnClickListener {
            product?.let { pr -> (activity as? MainActivity)?.markProductAsUsed(pr) }
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

    // --- ADDED: Make bottom sheet wrap content height ---
    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.let { bottomSheetDialog ->
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                it.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
