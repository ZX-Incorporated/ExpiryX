package com.expiryx.app

import android.content.Intent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout   // <-- Needed for your XML layouts
import android.widget.TextView       // <-- For Cancel button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.expiryx.app.R


class AddProductBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_add_product, container, false)

        val optionCamera = view.findViewById<LinearLayout>(R.id.optionCamera)
        val optionUpload = view.findViewById<LinearLayout>(R.id.optionUpload)
        val optionManual = view.findViewById<LinearLayout>(R.id.optionManual)
        val optionCancel = view.findViewById<TextView>(R.id.optionCancel)

        optionCamera.setOnClickListener {
            dismiss() // Later: launch camera intent
        }
        optionUpload.setOnClickListener {
            dismiss() // Later: open gallery/file picker
        }
        optionManual.setOnClickListener {
            dismiss()
            val intent = Intent(requireContext(), ManualEntryActivity::class.java)
            startActivity(intent)
        }

        optionCancel.setOnClickListener {
            dismiss()
        }

        return view
    }
}
