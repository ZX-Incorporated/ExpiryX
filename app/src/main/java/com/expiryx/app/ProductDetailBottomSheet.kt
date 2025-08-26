package com.expiryx.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.*

class ProductDetailBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_PRODUCT = "product"

        fun newInstance(product: Product): ProductDetailBottomSheet {
            val fragment = ProductDetailBottomSheet()
            val args = Bundle()
            args.putParcelable(ARG_PRODUCT, product)
            fragment.arguments = args
            return fragment
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
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_product_detail, container, false)

        val img: ImageView = view.findViewById(R.id.imgProductDetail)
        val name: TextView = view.findViewById(R.id.txtDetailName)
        val expiry: TextView = view.findViewById(R.id.txtDetailExpiry)
        val qty: TextView = view.findViewById(R.id.txtDetailQuantity)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)

        product?.let {
            Glide.with(requireContext())
                .load(it.imageUri ?: R.drawable.ic_placeholder)
                .into(img)

            name.text = it.name
            expiry.text = "Expires on: " + (it.expirationDate?.let { d ->
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(d))
            } ?: "N/A")
            qty.text = "Quantity: ${it.quantity}"
        }

        // Delete product with confirmation via MainActivity
        btnDelete.setOnClickListener {
            product?.let { p ->
                (activity as? MainActivity)?.deleteProductWithConfirmation(p)
            }
            dismiss()
        }

        // Edit product via MainActivity
        btnEdit.setOnClickListener {
            product?.let { p ->
                (activity as? MainActivity)?.editProduct(p)
            }
            dismiss()
        }

        return view
    }
}
