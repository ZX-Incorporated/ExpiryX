// app/src/main/java/com/expiryx/app/NotificationSettingsActivity.kt
package com.expiryx.app

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.expiryx.app.databinding.ActivityNotificationSettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationSettingsBinding
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    private val intervalOptionsDisplay = arrayOf("On expiry date", "1 day before", "3 days before", "7 days before", "14 days before")
    private val intervalOptionsValues = arrayOf("0", "1", "3", "7", "14")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        refreshUI()

        binding.defaultTimeCard.setOnClickListener {
            val hour = Prefs.getDefaultHour(this)
            val minute = Prefs.getDefaultMinute(this)

            TimePickerDialog(
                this,
                { _, h, m ->
                    Prefs.setDefaultTime(this, h, m)
                    refreshUI()
                    rescheduleAllNotifications()
                },
                hour, minute, true
            ).show()
        }

        binding.intervalsCard.setOnClickListener {
            showIntervalsDialog()
        }

        binding.snoozeCard.setOnClickListener {
            showSnoozeDialog()
        }

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

        // Reminder Intervals
        val selectedIntervals = Prefs.getReminderIntervals(this)
        val intervalsText = intervalOptionsValues
            .filter { selectedIntervals.contains(it) }
            .mapNotNull { value -> intervalOptionsDisplay.getOrNull(intervalOptionsValues.indexOf(value)) }
            .joinToString(", ")
            .ifEmpty { "Not set" }
        binding.intervalsSubtitle.text = intervalsText

        // Snooze
        if (Prefs.isSnoozeActive(this)) {
            val snoozeEndTime = Prefs.getSnoozeEndTimestamp(this)
            binding.snoozeSubtitle.text = "Snoozed until ${dateFormat.format(Date(snoozeEndTime))}"
        } else {
            binding.snoozeSubtitle.text = "Not snoozed (tap to set)"
        }
    }

    private fun showIntervalsDialog() {
        val savedIntervals = Prefs.getReminderIntervals(this)
        val checkedItems = intervalOptionsValues.map { savedIntervals.contains(it) }.toBooleanArray()
        val selectedItems = ArrayList(savedIntervals)

        AlertDialog.Builder(this)
            .setTitle("Select Reminder Intervals")
            .setMultiChoiceItems(intervalOptionsDisplay, checkedItems) { _, which, isChecked ->
                val value = intervalOptionsValues[which]
                if (isChecked) {
                    selectedItems.add(value)
                } else {
                    selectedItems.remove(value)
                }
            }
            .setPositiveButton("OK") { _, _ ->
                Prefs.setReminderIntervals(this, HashSet(selectedItems))
                refreshUI()
                rescheduleAllNotifications()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSnoozeDialog() {
        val activityContext: Context = this // Explicitly store activity context

        val input = EditText(activityContext).apply {
            hint = "Days to snooze"
            inputType = InputType.TYPE_CLASS_NUMBER
            if (Prefs.isSnoozeActive(activityContext)) {
                val snoozeEndTime = Prefs.getSnoozeEndTimestamp(activityContext)
                val remainingMillis = snoozeEndTime - System.currentTimeMillis()
                if (remainingMillis > 0) {
                    val remainingDays = TimeUnit.MILLISECONDS.toDays(remainingMillis).toInt()
                    setText((remainingDays + 1).toString())
                } else {
                    setText("1")
                }
            } else {
                setText("1")
            }
            setSelection(text.length)
        }

        AlertDialog.Builder(activityContext) // Use the stored activity context
            .setTitle("Set Snooze Duration")
            .setMessage("Enter number of days to snooze notifications. Snooze will last until the end of the specified day.")
            .setView(input) // This should be fine
            .setPositiveButton("Set Snooze") { _, _ ->
                val days = input.text.toString().toIntOrNull() ?: 0
                if (days > 0) {
                    Prefs.setSnooze(activityContext, days)
                } else {
                    Prefs.clearSnooze(activityContext)
                }
                refreshUI()
                if (days <= 0) rescheduleAllNotifications()
            }
            .setNegativeButton("Clear Snooze") { _, _ ->
                Prefs.clearSnooze(activityContext)
                refreshUI()
                rescheduleAllNotifications()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }


    private fun highlightCurrentTab() {
        binding.navHome.setImageResource(R.drawable.ic_home_unfilled)
        binding.navHistory.setImageResource(R.drawable.ic_clock_unfilled)
        binding.navSettings.setImageResource(R.drawable.ic_settings_filled)
    }

    private fun rescheduleAllNotifications() {
        val app = application as ProductApplication
        val repository = app.repository
        lifecycleScope.launch {
            val products = withContext(Dispatchers.IO) {
                repository.getAllProductsNow()
            }
            NotificationScheduler.rescheduleAllNotifications(this@NotificationSettingsActivity, products)
        }
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
