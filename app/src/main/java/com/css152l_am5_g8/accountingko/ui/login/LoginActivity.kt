package com.css152l_am5_g8.accountingko.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
//import com.css152l_am5_g8.accountingko.api.AuthManager
import com.css152l_am5_g8.accountingko.R
import com.css152l_am5_g8.accountingko.api.ApiClient
import com.css152l_am5_g8.accountingko.api.LoginRequest
import com.css152l_am5_g8.accountingko.ui.dashboard.DashboardActivity
import com.css152l_am5_g8.accountingko.ui.register.RegisterActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Find your views
        val usernameField = findViewById<android.widget.EditText>(R.id.emailInput)
        val passwordField = findViewById<android.widget.EditText>(R.id.passwordInput)
        val loginButton = findViewById<android.widget.Button>(R.id.loginButton)
        val registerButton = findViewById<android.widget.Button>(R.id.registerButton)

        // Pre-fill email if passed from registration
        val prefilledEmail = intent.getStringExtra("email")
        if (!prefilledEmail.isNullOrEmpty()) {
            usernameField.setText(prefilledEmail)
            passwordField.requestFocus()
        }

        registerButton.setOnClickListener {
            val signupIntent = Intent(this@LoginActivity, RegisterActivity::class.java)
            startActivity(signupIntent)
        }

        loginButton.setOnClickListener {
            val username = usernameField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(username, password)
        }
    }

    private fun performLogin(username: String, password: String) {
        lifecycleScope.launch {
            try {
                // Show loading
                Toast.makeText(this@LoginActivity, "Logging in...", Toast.LENGTH_SHORT).show()

                // Log the request data for debugging
                Log.d("Login", "Attempting login with email: $username")

                // Make API call using shared client
                val response = ApiClient.apiService.login(LoginRequest(username, password))

                Log.d("Login", "Response code: ${response.code()}")
                Log.d("Login", "Response body: ${response.body()}")

                if (response.isSuccessful) {
                    val loginResponse = response.body()

                    if (loginResponse?.success == true) {
                        // Login successful
                        val token = loginResponse.token

                        // Save token
                        saveToken(token)

                        // Show success message
                        Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()

                        // Navigate to main activity
                        val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        // Login failed
                        Toast.makeText(this@LoginActivity,
                            loginResponse?.message ?: "Login failed",
                            Toast.LENGTH_LONG).show()
                    }
                } else {
                    // HTTP error - get error body for more details
                    val errorBody = response.errorBody()?.string()
                    Log.e("Login", "HTTP Error ${response.code()}: $errorBody")

                    Toast.makeText(this@LoginActivity,
                        "Server error: ${response.code()} - Check your credentials",
                        Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                // Network error
                Log.e("Login", "Network error", e)
                Toast.makeText(this@LoginActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveToken(token: String?) {
        if (token != null) {
            val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
            prefs.edit().putString("token", token).apply()
        }
    }

    // Helper function to get saved token
    private fun getToken(): String? {
        val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        return prefs.getString("token", null)
    }
}