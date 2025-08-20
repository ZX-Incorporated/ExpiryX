package com.expiryx.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.*

class ProductDetailBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(product: Product): ProductDetailBottomSheet {
            val fragment = ProductDetailBottomSheet()
            val args = Bundle().apply { putParcelable("product", product) }
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var product: Product

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        product = requireArguments().getParcelable("product")!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // ✅ Inflate the correct layout
        val view = inflater.inflate(R.layout.bottom_sheet_product_detail, container, false)

        // ✅ Always use view.findViewById, not just findViewById
        val name = view.findViewById<TextView>(R.id.txtDetailName)
        val expiry = view.findViewById<TextView>(R.id.txtDetailExpiry)
        val qty = view.findViewById<TextView>(R.id.txtDetailQuantity)
        val deleteBtn = view.findViewById<Button>(R.id.btnDelete)
        val editBtn = view.findViewById<Button>(R.id.btnEdit)

        // Fill details
        name.text = product.name
        expiry.text = "Expires on: ${
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(product.expirationDate))
        }"
        qty.text = "Quantity: ${product.quantity}"

        // Delete product
        deleteBtn.setOnClickListener {
            (activity as? MainActivity)?.deleteProductWithConfirmation(product)
            dismiss()
        }

        // Edit product
        editBtn.setOnClickListener {
            (activity as? MainActivity)?.editProduct(product)
            dismiss()
        }

        return view
    }
}
