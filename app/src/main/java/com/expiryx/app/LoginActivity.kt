package com.expiryx.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if already logged in (or if the welcome screen has been passed once)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false) // You might rename this pref if it's no longer strictly "login"

        if (isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val btnContinue = findViewById<Button>(R.id.buttonContinue) // Changed from buttonGuest

        // Continue button
        btnContinue.setOnClickListener {
            // Mark that the user has passed the welcome screen
            prefs.edit().putBoolean("isLoggedIn", true).apply() // Consider renaming "isLoggedIn" to something like "welcomeScreenPassed"

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
