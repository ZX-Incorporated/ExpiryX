package com.expiryx.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.expiryx.app.databinding.BottomsheetHistoryDetailBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.*

class HistoryDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetHistoryDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var history: History
    private lateinit var viewModel: HistoryViewModel

    // Date formatting helpers (mirrored from ProductDetailBottomSheet)
    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = (requireActivity() as HistoryActivity).viewModel
        arguments?.let {
            history = it.getParcelable("history")!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetHistoryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        populateUI(history)
        setupListeners(history)
    }

    private fun populateUI(h: History) {
        // Image
        Glide.with(requireContext())
            .load(h.imageUri)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder) // Ensure an error placeholder is also set
            .into(binding.imageHistoryDetail)

        // Core fields
        binding.textHistoryName.text = h.productName
        binding.textHistoryBrand.text = h.brand.takeIf { !it.isNullOrBlank() } ?: "No brand"
        binding.textHistoryBrand.visibility = if (h.brand.isNullOrBlank()) View.GONE else View.VISIBLE

        binding.textHistoryExpiry.text = "Expiry: ${h.expirationDate?.let { formatDate(it) } ?: "N/A"}"
        binding.textHistoryQuantity.text = "Quantity: ${h.quantity}"
        binding.textHistoryWeight.text = "Weight: ${h.weight.takeIf { !it.isNullOrBlank() } ?: "-"}"
        binding.textHistoryWeight.visibility = if (h.weight.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.textHistoryFavourite.text = "Favourite: ${if (h.isFavorite) "Yes" else "No"}"

        // Barcode display
        if (!h.barcode.isNullOrBlank()) {
            // Assuming R.string.barcode_label exists, e.g., "Barcode:"
            binding.txtHistoryBarcode.text = "${getString(R.string.barcode_label)} ${h.barcode}" 
            binding.txtHistoryBarcode.visibility = View.VISIBLE
        } else {
            binding.txtHistoryBarcode.visibility = View.GONE
        }

        // Timestamps
        // Assuming R.string.added_label exists, e.g., "Added:"
        binding.txtHistoryDateAdded.text = "${getString(R.string.added_label)} ${formatDateTime(h.dateAdded)}"
        
        if (h.dateModified != null) {
            // Assuming R.string.modified_label exists, e.g., "Modified:"
            binding.txtHistoryDateModified.text = "${getString(R.string.modified_label)} ${formatDateTime(h.dateModified)}"
            binding.txtHistoryDateModified.visibility = View.VISIBLE
        } else {
            binding.txtHistoryDateModified.visibility = View.GONE
        }
    }

    private fun setupListeners(history: History) {
        // Actions based on type
        when (history.action) {
            "Deleted" -> {
                binding.btnPrimary.text = "Restore"
                binding.btnPrimary.setOnClickListener {
                    viewModel.restoreDeleted(history) // This will create a Product with updated dateModified
                    dismiss()
                }
            }
            "Used" -> {
                binding.btnPrimary.text = "Un-use"
                binding.btnPrimary.setOnClickListener {
                    viewModel.unuse(history) // This will create a Product with updated dateModified
                    dismiss()
                }
            }
            "Expired" -> {
                binding.btnPrimary.text = "Change Expiry & Restore"
                binding.btnPrimary.setOnClickListener { showDatePicker(history) } // Pass history to showDatePicker
            }
        }

        // Permanent delete
        binding.btnSecondary.text = "Permanently Delete"
        binding.btnSecondary.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Permanently Delete")
                .setMessage("Are you sure you want to permanently delete ${history.productName}? This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.permanentlyDelete(history)
                    dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showDatePicker(historyForRestore: History) { // Accept History object
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val calendar = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59); set(Calendar.MILLISECOND, 999) }
                val newExpiryMillis = calendar.timeInMillis
                // Pass the original history item to be restored with a new expiry
                viewModel.changeExpiry(historyForRestore, newExpiryMillis) 
                Toast.makeText(requireContext(), "Expiry updated & restored", Toast.LENGTH_SHORT).show()
                dismiss()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(history: History): HistoryDetailBottomSheet {
            return HistoryDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable("history", history)
                }
            }
        }
    }
}
