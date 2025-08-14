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
        setContentView(R.layout.activity_login)

        val emailField = findViewById<EditText>(R.id.editTextEmail)
        val passwordField = findViewById<EditText>(R.id.editTextPassword)
        val btnLogin = findViewById<Button>(R.id.buttonLogin)
        val btnGuest = findViewById<Button>(R.id.buttonGuest)
        val signUpText = findViewById<Button>(R.id.buttonSignUp)
        val forgotPasswordText = findViewById<TextView>(R.id.textViewForgotPassword)

        // Log In button logic
        btnLogin.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                if (email.isEmpty()) emailField.error = "Please enter email"
                if (password.isEmpty()) passwordField.error = "Please enter password"
            }
        }

        // Continue as guest button logic
        btnGuest.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }


    }
}
