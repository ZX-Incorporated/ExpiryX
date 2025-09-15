package com.expiryx.app

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.expiryx.app.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appVersionText.text = "v${getString(R.string.app_version_name)}"

        // Setup dark mode toggle
        setupDarkModeToggle()

        binding.notificationsCard.setOnClickListener {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
            overridePendingTransition(0, 0)
        }

        binding.exportCard.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Export Data")
                .setMessage("Do you want to export your data to the Downloads folder?")
                .setPositiveButton("Yes") { _, _ -> exportDataToCsv() }
                .setNegativeButton("Cancel", null)
                .show()
        }

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
            // Already here
        }
    }

    private fun setupDarkModeToggle() {
        // Set initial state based on current theme
        val isDarkMode = ThemeManager.isDarkMode(this)
        binding.darkModeSwitch.isChecked = isDarkMode
        
        // Handle toggle changes
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val themeMode = if (isChecked) ThemeManager.THEME_DARK else ThemeManager.THEME_LIGHT
            ThemeManager.setThemeMode(this, themeMode)
            
            // Restart activity to apply theme changes
            recreate()
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

            val escape = { text: String? ->
                text?.replace("\"", "\"\"")?.take(300) ?: ""
            }

            val csvContent = buildString {
                appendLine("=== Products ===")
                appendLine("ID,Name,Expiry,Quantity,Weight,Brand,Favorite,ImageUri")
                for (p in products) {
                    appendLine(
                        "${p.id},\"${escape(p.name)}\",${p.expirationDate ?: ""},${p.quantity}," +
                                "\"${escape(p.weight?.toString())}\",\"${escape(p.brand)}\",${p.isFavorite}," +
                                "\"${escape(p.imageUri)}\""
                    )
                }

                appendLine()
                appendLine("=== History ===")
                appendLine("ID,ProductID,Name,Expiry,Quantity,Weight,Brand,Favorite,ImageUri,Action,Timestamp")
                for (h in history) {
                    appendLine(
                        "${h.id},${h.productId ?: ""},\"${escape(h.productName)}\"," +
                                "${h.expirationDate ?: ""},${h.quantity}," +
                                "\"${escape(h.weight?.toString())}\",\"${escape(h.brand)}\",${h.isFavorite}," +
                                "\"${escape(h.imageUri)}\",${escape(h.action)},${h.timestamp}"
                    )
                }
            }

            val resolver = applicationContext.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "expiryx_data_${System.currentTimeMillis()}.csv")
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val message: String = try {
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { it.write(csvContent.toByteArray()) }
                    "Exported successfully to Downloads."
                } else {
                    "Failed to create export file."
                }
            } catch (e: Exception) {
                "Error during export: ${e.message}"
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_LONG).show()
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