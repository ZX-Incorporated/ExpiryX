package com.expiryx.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

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

        optionCamera.setOnClickListener { dismiss() /* TODO camera later */ }
        optionUpload.setOnClickListener { dismiss() /* TODO picker later */ }
        optionManual.setOnClickListener {
            startActivity(Intent(requireContext(), ManualEntryActivity::class.java))
            dismiss()
        }
        optionCancel.setOnClickListener { dismiss() }

        return view
    }
}
