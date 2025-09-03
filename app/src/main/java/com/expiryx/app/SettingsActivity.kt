package com.expiryx.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.expiryx.app.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Set version text (safe via resources) ---
        val versionText: TextView = binding.appVersionText
        versionText.text = "v${getString(R.string.app_version_name)}"

        // --- Export data card ---
        binding.exportCard.setOnClickListener {
            exportDataToCsv()
        }

        setupBottomNav()
    }

    private fun setupBottomNav() {
        binding.navHomeWrapper.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(0, 0)
            finish()
        }
        binding.navCartWrapper.setOnClickListener {
            Toast.makeText(this, "Store coming soon…", Toast.LENGTH_SHORT).show()
        }
        binding.navHistoryWrapper.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(0, 0)
            finish()
        }
        binding.navSettingsWrapper.setOnClickListener {
            // already here
        }
    }

    private fun exportDataToCsv() {
        val repo = (application as ProductApplication).repository

        lifecycleScope.launch(Dispatchers.IO) {
            val products = repo.getAllProductsNow()
            val history = repo.getAllHistoryNow()

            val file = File(getExternalFilesDir(null), "expiryx_data.csv")
            FileWriter(file).use { writer ->
                // Products
                writer.appendLine("=== Products ===")
                writer.appendLine("ID,Name,Expiry,Quantity,Weight,Notes,Favorite")
                for (p in products) {
                    writer.appendLine("${p.id},${p.name},${p.expirationDate ?: ""},${p.quantity},${p.weight ?: ""},${p.notes ?: ""},${p.isFavorite}")
                }

                // History
                writer.appendLine()
                writer.appendLine("=== History ===")
                writer.appendLine("ID,ProductID,Name,Action,Timestamp")
                for (h in history) {
                    writer.appendLine("${h.id},${h.productId ?: ""},${h.productName},${h.action},${h.timestamp}")
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@SettingsActivity, "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
