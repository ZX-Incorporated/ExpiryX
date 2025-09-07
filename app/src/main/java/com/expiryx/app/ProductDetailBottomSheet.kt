package com.expiryx.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.expiryx.app.databinding.BottomSheetProductDetailBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.*

class ProductDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetProductDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductViewModel by activityViewModels {
        ProductViewModelFactory((requireActivity().application as ProductApplication).repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val product = arguments?.getParcelable<Product>(ARG_PRODUCT) ?: return
        populateUI(product)
        setupListeners(product)
    }

    private fun populateUI(p: Product) {
        // --- Image ---
        Glide.with(requireContext())
            .load(if (p.imageUri.isNullOrBlank()) R.drawable.ic_placeholder else Uri.parse(p.imageUri))
            .error(R.drawable.ic_placeholder)
            .into(binding.imgProductDetail)

        // --- Core fields ---
        binding.txtDetailName.text = p.name
        binding.txtDetailBrand.text = p.brand
        binding.txtDetailBrand.visibility = if (p.brand.isNullOrBlank()) View.GONE else View.VISIBLE

        binding.txtDetailExpiry.text = "Expires on: ${p.expirationDate?.let { formatDate(it) } ?: "N/A"}"
        binding.txtDetailQuantity.text = "Quantity: ${p.quantity}"

        // --- Optional fields ---
        if (p.weight != null) {
            binding.txtDetailWeight.text = "Weight: ${p.weight} ${p.weightUnit}"
            binding.txtDetailWeight.visibility = View.VISIBLE
        } else {
            binding.txtDetailWeight.visibility = View.GONE
        }

        binding.txtDetailReminder.text = "Reminder: ${p.reminderDays} day${if (p.reminderDays == 1) "" else "s"} before"
        binding.txtDetailReminder.visibility = if (p.reminderDays > 0) View.VISIBLE else View.GONE

        // --- Barcode display ---
        if (!p.barcode.isNullOrBlank()) {
            binding.txtDetailBarcode.text = "${getString(R.string.barcode_label)} ${p.barcode}"
            binding.txtDetailBarcode.visibility = View.VISIBLE
        } else {
            binding.txtDetailBarcode.visibility = View.GONE
        }

        // --- Timestamps ---
        binding.txtDetailDateAdded.text = "${getString(R.string.added_label)} ${formatDateTime(p.dateAdded)}"
        
        if (p.dateModified != null) {
            binding.txtDetailDateModified.text = "${getString(R.string.modified_label)} ${formatDateTime(p.dateModified)}"
            binding.txtDetailDateModified.visibility = View.VISIBLE
        } else {
            binding.txtDetailDateModified.visibility = View.GONE
        }
    }

    private fun setupListeners(p: Product) {
        val hostActivity = activity as? MainActivity
        binding.btnMarkAsUsed.setOnClickListener {
            hostActivity?.markProductAsUsed(p)
            dismiss()
        }
        binding.btnDelete.setOnClickListener {
            hostActivity?.deleteProductWithConfirmation(p)
            dismiss()
        }
        binding.btnEdit.setOnClickListener {
            hostActivity?.editProduct(p)
            dismiss()
        }
        binding.btnOpenInBrowser.setOnClickListener {
            openProductInBrowser(p.name)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun openProductInBrowser(productName: String) {
        try {
            val searchQuery = Uri.encode(productName)
            val searchUrl = "https://www.google.com/search?q=$searchQuery"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
            startActivity(intent)
        } catch (e: Exception) {
            // Handle case where no browser is available
            android.widget.Toast.makeText(requireContext(), "No browser available", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ARG_PRODUCT = "product"
        fun newInstance(product: Product) = ProductDetailBottomSheet().apply {
            arguments = Bundle().apply { putParcelable(ARG_PRODUCT, product) }
        }
    }
}