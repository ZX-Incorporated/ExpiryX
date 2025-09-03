package com.expiryx.app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var navHome: ImageView
    private lateinit var navCart: ImageView
    private lateinit var navHistory: ImageView
    private lateinit var navSettings: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        navHome = findViewById(R.id.navHome)
        navCart = findViewById(R.id.navCart)
        navHistory = findViewById(R.id.navHistory)
        navSettings = findViewById(R.id.navSettings)

        // highlight current tab
        navHome.setImageResource(R.drawable.ic_home_unfilled)
        navCart.setImageResource(R.drawable.ic_cart)
        navHistory.setImageResource(R.drawable.ic_clock_unfilled)
        navSettings.setImageResource(R.drawable.ic_settings_filled)

        navHome.setOnClickListener {
            if (this !is MainActivity) {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                overridePendingTransition(0, 0)
                finish()
            }
        }
        navCart.setOnClickListener {
            Toast.makeText(this, "Store coming soon…", Toast.LENGTH_SHORT).show()
        }
        navHistory.setOnClickListener {
            if (this !is HistoryActivity) {
                startActivity(Intent(this, HistoryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                overridePendingTransition(0, 0)
                finish()
            }
        }
        navSettings.setOnClickListener {
            // already here
        }
    }
}
