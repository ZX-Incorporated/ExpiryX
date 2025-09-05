package com.expiryx.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.expiryx.app.databinding.BottomsheetHistoryDetailBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetHistoryDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var history: History
    private lateinit var viewModel: HistoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        history = requireArguments().getParcelable("history")!!
        viewModel = (requireActivity() as HistoryActivity).viewModel
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
        // --- Basic info ---
        binding.textHistoryName.text = history.productName
        binding.textHistoryNotes.text = history.notes ?: "No notes"
        binding.textHistoryQuantity.text = "Quantity: ${history.quantity}"
        binding.textHistoryWeight.text = "Weight: ${history.weight ?: "-"}"
        binding.textHistoryFavourite.text = "Favourite: ${history.isFavorite}"

        history.expirationDate?.let {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.textHistoryExpiry.text = "Expiry: ${sdf.format(Date(it))}"
        } ?: run {
            binding.textHistoryExpiry.text = "No expiry date"
        }

        Glide.with(requireContext())
            .load(history.imageUri)
            .placeholder(R.drawable.ic_placeholder)
            .into(binding.imageHistoryDetail)

        // --- Actions based on type ---
        when (history.action) {
            "Deleted" -> {
                binding.btnPrimary.text = "Restore"
                binding.btnPrimary.setOnClickListener {
                    viewModel.restoreDeleted(history)
                    dismiss()
                }
            }
            "Used" -> {
                binding.btnPrimary.text = "Unuse"
                binding.btnPrimary.setOnClickListener {
                    viewModel.unuse(history)
                    dismiss()
                }
            }
            "Expired" -> {
                binding.btnPrimary.text = "Change Expiry"
                binding.btnPrimary.setOnClickListener { showDatePicker() }
            }
        }

        // --- Permanent delete with confirmation ---
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

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val newDate = Calendar.getInstance().apply {
                    set(y, m, d, 0, 0, 0)
                }.timeInMillis
                viewModel.changeExpiry(history, newDate)
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
            val f = HistoryDetailBottomSheet()
            f.arguments = Bundle().apply { putParcelable("history", history) }
            return f
        }
    }
}
