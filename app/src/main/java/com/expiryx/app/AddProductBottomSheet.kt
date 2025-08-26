package com.expiryx.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddProductBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // ✅ Use your actual XML: bottom_sheet_add_product.xml
        val view = inflater.inflate(R.layout.bottom_sheet_add_product, container, false)

        // ✅ IDs from your XML
        val optionManual: LinearLayout = view.findViewById(R.id.optionManual)
        val optionCamera: LinearLayout = view.findViewById(R.id.optionCamera)
        val optionUpload: LinearLayout = view.findViewById(R.id.optionUpload)
        val optionCancel: View = view.findViewById(R.id.optionCancel)

        optionManual.setOnClickListener {
            startActivity(Intent(requireContext(), ManualEntryActivity::class.java))
            dismiss()
        }

        // ✅ Camera → Barcode Scanner Activity
        optionCamera.setOnClickListener {
            startActivity(Intent(requireContext(), BarcodeScannerActivity::class.java))
            dismiss()
        }

        // ✅ Upload from device → open gallery for image only (no analysis)
        optionUpload.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            startActivityForResult(
                Intent.createChooser(intent, "Select Picture"),
                REQUEST_CODE_PICK_IMAGE
            )
        }

        optionCancel.setOnClickListener { dismiss() }

        return view
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            imageUri?.let {
                // ✅ Just open ManualEntryActivity with the selected image URI for preview/edit
                val intent = Intent(requireContext(), ManualEntryActivity::class.java).apply {
                    putExtra("imageUri", it.toString())
                }
                startActivity(intent)
                dismiss()
            }
        }
    }
}
