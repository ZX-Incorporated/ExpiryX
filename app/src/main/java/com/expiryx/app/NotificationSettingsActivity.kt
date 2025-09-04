// app/src/main/java/com/expiryx/app/NotificationSettingsActivity.kt
package com.expiryx.app

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.expiryx.app.databinding.ActivityNotificationSettingsBinding
import java.util.*

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Back button ---
        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        // --- Load initial values ---
        refreshUI()

        // --- Default Time click ---
        binding.defaultTimeCard.setOnClickListener {
            val hour = Prefs.getDefaultHour(this)
            val minute = Prefs.getDefaultMinute(this)

            TimePickerDialog(
                this,
                { _, h, m ->
                    Prefs.setDefaultTime(this, h, m)
                    refreshUI()
                },
                hour, minute, true
            ).show()
        }

        // --- Snooze Duration click ---
        binding.snoozeCard.setOnClickListener {
            showSnoozeDialog()
        }

        // bottom nav
        highlightCurrentTab()
        setupBottomNav()
    }

    private fun refreshUI() {
        // Default time
        val h = Prefs.getDefaultHour(this)
        val m = Prefs.getDefaultMinute(this)
        val ampm = if (h < 12) "AM" else "PM"
        val hour12 = if (h % 12 == 0) 12 else h % 12
        val minuteStr = String.format("%02d", m)
        binding.defaultTimeSubtitle.text = "$hour12:$minuteStr $ampm (tap to change)"

        // Snooze
        val snoozeEnabled = Prefs.isSnoozeEnabled(this)
        val snoozeDays = Prefs.getSnoozeDays(this)
        val snoozeText = if (snoozeEnabled) {
            "Enabled: $snoozeDays day(s)"
        } else {
            "Disabled (tap to enable)"
        }
        binding.snoozeSubtitle.text = snoozeText
    }


    private fun showSnoozeDialog() {
        val snoozeEnabled = Prefs.isSnoozeEnabled(this)
        val snoozeDays = Prefs.getSnoozeDays(this)

        val input = EditText(this).apply {
            hint = "Days"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(snoozeDays.toString())
        }

        AlertDialog.Builder(this)
            .setTitle("Snooze Duration")
            .setMessage("Enable snooze and set duration in days")
            .setView(input)
            .setPositiveButton("Enable") { _, _ ->
                val days = input.text.toString().toIntOrNull() ?: 1
                Prefs.setSnooze(this, true, days)
                refreshUI()
            }
            .setNegativeButton("Disable") { _, _ ->
                Prefs.setSnooze(this, false, 1)
                refreshUI()
            }
            .show()
    }

    private fun highlightCurrentTab() {
        binding.navHome.setImageResource(R.drawable.ic_home_unfilled)
        binding.navHistory.setImageResource(R.drawable.ic_clock_unfilled)
        binding.navSettings.setImageResource(R.drawable.ic_settings_filled)
    }

    private fun setupBottomNav() {
        binding.navHomeWrapper.setOnClickListener {
            startActivity(android.content.Intent(this, MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(0, 0)
            finish()
        }
        binding.navCartWrapper.setOnClickListener {
            android.widget.Toast.makeText(this, "Store coming soon…", android.widget.Toast.LENGTH_SHORT).show()
        }
        binding.navHistoryWrapper.setOnClickListener {
            startActivity(android.content.Intent(this, HistoryActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(0, 0)
            finish()
        }
        binding.navSettingsWrapper.setOnClickListener {
            // already here
        }
    }
}
