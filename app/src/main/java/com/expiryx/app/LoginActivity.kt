package com.expiryx.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Step 2: Check if already logged in
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val emailField = findViewById<EditText>(R.id.editTextEmail)
        val passwordField = findViewById<EditText>(R.id.editTextPassword)
        val btnLogin = findViewById<Button>(R.id.buttonLogin)
        val btnGuest = findViewById<Button>(R.id.buttonGuest)
        val signUpText = findViewById<Button>(R.id.buttonSignUp)
        val forgotPasswordText = findViewById<TextView>(R.id.textViewForgotPassword)

        // 🔹 Save login state when logging in
        btnLogin.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                prefs.edit().putBoolean("isLoggedIn", true).apply()

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                if (email.isEmpty()) emailField.error = "Please enter email"
                if (password.isEmpty()) passwordField.error = "Please enter password"
            }
        }

        // 🔹 Guest login (also saves state)
        btnGuest.setOnClickListener {
            prefs.edit().putBoolean("isLoggedIn", true).apply()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // (Optional placeholders for signup and forgot password)
        signUpText.setOnClickListener {
            // TODO: Handle sign-up
        }

        forgotPasswordText.setOnClickListener {
            // TODO: Handle forgot password
        }
    }
}
