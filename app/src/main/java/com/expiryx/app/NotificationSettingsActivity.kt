package com.expiryx.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.expiryx.app.databinding.ActivityNotificationSettingsBinding

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        // Note: Reminder intervals will later allow editing only "1 day before" and "7 days before"
        // with a max of 3 intervals. Currently, subtitle is static.

        highlightCurrentTab()
        setupBottomNav()
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
