package com.example.grama_sanjeevini

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        val emailInput = findViewById<EditText>(R.id.etResetEmail)
        val resetBtn = findViewById<Button>(R.id.btnResetPassword)
        val backLink = findViewById<TextView>(R.id.tvBackToLogin)

        resetBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Reset link sent to your email!", Toast.LENGTH_LONG).show()
                    finish() // Sends them automatically back to login
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to send link: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        backLink.setOnClickListener {
            finish()
        }
    }
}
