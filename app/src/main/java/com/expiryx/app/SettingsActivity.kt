package com.expiryx.app

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

        // --- Show app version ---
        val versionText: TextView = binding.appVersionText
        versionText.text = "v${getString(R.string.app_version_name)}"

        // --- Notifications ---
        binding.notificationsCard.setOnClickListener {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
            overridePendingTransition(0, 0)
        }

        // --- Export card with confirmation ---
        binding.exportCard.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Export Data")
                .setMessage("Do you want to export your products and history to Downloads?")
                .setPositiveButton("Yes") { _, _ -> exportDataToCsv() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // --- Delete all user data ---
        binding.deleteDataCard.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete All Data")
                .setMessage("This will permanently erase all products and history. Are you sure?")
                .setPositiveButton("Delete") { _, _ -> deleteAllData() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        setupBottomNav()
        highlightCurrentTab()
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

    private fun highlightCurrentTab() {
        binding.navHome.setImageResource(R.drawable.ic_home_unfilled)
        binding.navHistory.setImageResource(R.drawable.ic_clock_unfilled)
        binding.navSettings.setImageResource(R.drawable.ic_settings_filled)
    }

    private fun exportDataToCsv() {
        val repo = (application as ProductApplication).repository

        lifecycleScope.launch(Dispatchers.IO) {
            val products = repo.getAllProductsNow()
            val history = repo.getAllHistoryNow()

            // ✅ App-private Downloads directory (no permissions needed)
            val downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && !downloadsDir.exists()) downloadsDir.mkdirs()

            val file = File(downloadsDir, "expiryx_data.csv")

            FileWriter(file).use { writer ->
                // Products
                writer.appendLine("=== Products ===")
                writer.appendLine("ID,Name,Expiry,Quantity,Weight,Notes,Favorite,ImageUri")
                for (p in products) {
                    writer.appendLine(
                        "${p.id},${p.name},${p.expirationDate ?: ""},${p.quantity}," +
                                "${p.weight ?: ""},${p.notes ?: ""},${p.isFavorite}," +
                                "${p.imageUri ?: ""}"
                    )
                }

                // History
                writer.appendLine()
                writer.appendLine("=== History ===")
                writer.appendLine("ID,ProductID,Name,Expiry,Quantity,Weight,Notes,Favorite,ImageUri,Action,Timestamp")
                for (h in history) {
                    writer.appendLine(
                        "${h.id},${h.productId ?: ""},${h.productName}," +
                                "${h.expirationDate ?: ""},${h.quantity}," +
                                "${h.weight ?: ""},${h.notes ?: ""},${h.isFavorite}," +
                                "${h.imageUri ?: ""},${h.action},${h.timestamp}"
                    )
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@SettingsActivity,
                    "Exported to: ${file.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun deleteAllData() {
        val repo = (application as ProductApplication).repository
        lifecycleScope.launch(Dispatchers.IO) {
            repo.clearAllProducts()
            repo.clearAllHistory()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@SettingsActivity, "All data deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
